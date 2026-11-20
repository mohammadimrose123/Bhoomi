package com.bhoomi.listing;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.bhoomi.broker.BrokerApprovalStatus;
import com.bhoomi.broker.BrokerProfile;
import com.bhoomi.broker.BrokerProfileRepository;
import com.bhoomi.common.AuditService;
import com.bhoomi.land.LandMeasurementUnit;
import com.bhoomi.land.LandParcel;
import com.bhoomi.land.LandParcelRepository;
import com.bhoomi.land.LandType;
import com.bhoomi.land.Location;
import com.bhoomi.land.LocationRepository;

@Service
public class ListingService {

    private final PropertyListingRepository listingRepository;
    private final LandParcelRepository parcelRepository;
    private final LocationRepository locationRepository;
    private final BrokerProfileRepository brokerRepository;
    private final AuditService auditService;
    private final ListingMediaRepository mediaRepository;

    public ListingService(PropertyListingRepository listingRepository, LandParcelRepository parcelRepository,
            LocationRepository locationRepository, BrokerProfileRepository brokerRepository,
            AuditService auditService, ListingMediaRepository mediaRepository) {
        this.listingRepository = listingRepository;
        this.parcelRepository = parcelRepository;
        this.locationRepository = locationRepository;
        this.brokerRepository = brokerRepository;
        this.auditService = auditService;
        this.mediaRepository = mediaRepository;
    }

    @Transactional(readOnly = true)
    public ListingDtos.PageResponse<ListingDtos.ListingResponse> search(
            int page, int size, String sortBy, String direction, String district, String mandal, String village,
            LandType landType, BigDecimal minPrice, BigDecimal maxPrice, BigDecimal minExtent, BigDecimal maxExtent,
            LandMeasurementUnit measurementUnit) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be non-negative and size must be 1-100");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minPrice cannot exceed maxPrice");
        }
        if ((minPrice != null && minPrice.signum() < 0) || (maxPrice != null && maxPrice.signum() < 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "price filters cannot be negative");
        }
        if (minExtent != null && maxExtent != null && minExtent.compareTo(maxExtent) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minExtent cannot exceed maxExtent");
        }
        if ((minExtent != null && minExtent.signum() <= 0) || (maxExtent != null && maxExtent.signum() <= 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "extent filters must be positive");
        }
        if ((minExtent != null || maxExtent != null) && measurementUnit == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "measurementUnit is required when filtering by extent");
        }
        if (hasTooLongFilter(district) || hasTooLongFilter(mandal) || hasTooLongFilter(village)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Location filters cannot exceed 120 characters");
        }
        String sortProperty = switch (sortBy == null ? "createdAt" : sortBy) {
            case "createdAt" -> "createdAt";
            case "askingPrice" -> "askingPrice";
            case "title" -> "title";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported sortBy field");
        };
        Sort.Direction sortDirection;
        try {
            sortDirection = Sort.Direction.fromString(direction == null ? "DESC" : direction);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "direction must be ASC or DESC");
        }
        Page<PropertyListing> listings = listingRepository.findAll(
                ListingSpecification.publishedSearch(district, mandal, village, landType,
                        minPrice, maxPrice, minExtent, maxExtent, measurementUnit),
                PageRequest.of(page, size, Sort.by(sortDirection, sortProperty)));
        return new ListingDtos.PageResponse<>(toResponses(listings.getContent()), page, size,
                listings.getTotalElements(), listings.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ListingDtos.ListingResponse getPublicListing(UUID listingId) {
        PropertyListing listing = listingRepository.findById(listingId)
                .filter(candidate -> candidate.getStatus() == ListingStatus.PUBLISHED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
        return toResponse(listing);
    }

    @Transactional
    public ListingDtos.ListingResponse create(UUID userId, ListingDtos.CreateRequest request) {
        return create(userId, request, ListingStatus.SUBMITTED);
    }

    @Transactional
    public ListingDtos.ListingResponse createDraft(UUID userId, ListingDtos.CreateRequest request) {
        return create(userId, request, ListingStatus.DRAFT);
    }

    private ListingDtos.ListingResponse create(UUID userId, ListingDtos.CreateRequest request, ListingStatus initialStatus) {
        BrokerProfile broker = brokerFor(userId);
        if (initialStatus == ListingStatus.SUBMITTED && broker.getApprovalStatus() != BrokerApprovalStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Broker approval is required before submitting listings");
        }
        ListingDtos.ParcelRequest parcelRequest = request.parcel();
        Location location = locationRepository
                .findByCountryAndStateAndDistrictAndMandalAndVillage("India", parcelRequest.state().trim(),
                        parcelRequest.district().trim(), parcelRequest.mandal().trim(), parcelRequest.village().trim())
                .orElseGet(() -> locationRepository.save(new Location(parcelRequest.state().trim(),
                        parcelRequest.district().trim(), parcelRequest.mandal().trim(), parcelRequest.village().trim(), null)));
        if ((parcelRequest.latitude() == null) != (parcelRequest.longitude() == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Latitude and longitude must be provided together");
        }
        String subdivision = parcelRequest.subdivisionNumber() == null ? "" : parcelRequest.subdivisionNumber().trim();
        LandParcel parcel = parcelRepository.findByLocationIdAndSurveyNumberAndSubdivisionNumber(
                location.getId(), parcelRequest.surveyNumber().trim(), subdivision)
                .orElseGet(() -> parcelRepository.save(new LandParcel(location.getId(),
                        parcelRequest.surveyNumber().trim(), subdivision, parcelRequest.extent(),
                        parcelRequest.measurementUnit(), parcelRequest.landType(),
                        parcelRequest.latitude(), parcelRequest.longitude())));
        parcel.setCoordinatesIfAbsent(parcelRequest.latitude(), parcelRequest.longitude());
        if (listingRepository.existsByLandParcelIdAndStatusIn(parcel.getId(),
                List.of(ListingStatus.DRAFT, ListingStatus.SUBMITTED, ListingStatus.UNDER_REVIEW,
                        ListingStatus.PUBLISHED, ListingStatus.REJECTED))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An active listing already exists for this survey parcel");
        }
        PropertyListing listing = new PropertyListing(parcel.getId(), broker.getId(), request.title().trim(),
                request.description().trim(), request.askingPrice(), blankToNull(request.sourceReference()));
        if (initialStatus == ListingStatus.SUBMITTED) {
            listing.setStatus(ListingStatus.SUBMITTED, null, null);
        }
        listing = listingRepository.save(listing);
        auditService.record(userId, initialStatus == ListingStatus.SUBMITTED ? "LISTING_SUBMITTED" : "LISTING_DRAFT_CREATED",
                "property_listings", listing.getId(),
                java.util.Map.of("status", listing.getStatus().name()));
        return toResponse(listing);
    }

    @Transactional
    public ListingDtos.ListingResponse submit(UUID userId, UUID listingId) {
        BrokerProfile broker = brokerFor(userId);
        if (broker.getApprovalStatus() != BrokerApprovalStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Broker approval is required before submitting listings");
        }
        PropertyListing listing = listingRepository.findById(listingId)
                .filter(candidate -> candidate.getBrokerProfileId().equals(broker.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
        if (listing.getStatus() != ListingStatus.DRAFT && listing.getStatus() != ListingStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only draft or rejected listings can be submitted");
        }
        listing.setStatus(ListingStatus.SUBMITTED, null, null);
        listing = listingRepository.save(listing);
        auditService.record(userId, "LISTING_SUBMITTED", "property_listings", listing.getId(),
                java.util.Map.of("status", listing.getStatus().name()));
        return toResponse(listing);
    }

    @Transactional
    public ListingDtos.ListingResponse changeBrokerStatus(UUID userId, UUID listingId, ListingStatus target) {
        BrokerProfile broker = brokerFor(userId);
        PropertyListing listing = listingRepository.findById(listingId)
                .filter(candidate -> candidate.getBrokerProfileId().equals(broker.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
        if (listing.getStatus() != ListingStatus.PUBLISHED
                || (target != ListingStatus.SOLD && target != ListingStatus.UNAVAILABLE && target != ListingStatus.ARCHIVED)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Brokers may mark only their published listings as sold, unavailable, or archived");
        }
        ListingStatus previous = listing.getStatus();
        listing.setBrokerAvailabilityStatus(target);
        listing = listingRepository.save(listing);
        auditService.record(userId, "BROKER_LISTING_STATUS_CHANGED", "property_listings", listing.getId(),
                java.util.Map.of("from", previous.name(), "to", target.name()));
        return toResponse(listing);
    }

    @Transactional
    public ListingDtos.ListingResponse confirmAvailability(UUID userId, UUID listingId) {
        BrokerProfile broker = brokerFor(userId);
        PropertyListing listing = listingRepository.findById(listingId)
                .filter(candidate -> candidate.getBrokerProfileId().equals(broker.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
        if (listing.getStatus() != ListingStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only published listings can be confirmed as available");
        }
        listing.confirmAvailability();
        listing = listingRepository.save(listing);
        auditService.record(userId, "LISTING_AVAILABILITY_CONFIRMED", "property_listings", listing.getId(),
                java.util.Map.of("status", listing.getStatus().name()));
        return toResponse(listing);
    }

    @Transactional
    public ListingDtos.ListingResponse update(UUID userId, UUID listingId, ListingDtos.UpdateRequest request) {
        BrokerProfile broker = brokerFor(userId);
        PropertyListing listing = listingRepository.findById(listingId)
                .filter(candidate -> candidate.getBrokerProfileId().equals(broker.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
        if (listing.getStatus() != ListingStatus.DRAFT && listing.getStatus() != ListingStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only draft or rejected listings can be edited");
        }
        BigDecimal previousPrice = listing.getAskingPrice();
        listing.update(request.title().trim(), request.description().trim(), request.askingPrice());
        listing = listingRepository.save(listing);
        auditService.record(userId, "LISTING_UPDATED", "property_listings", listing.getId(),
                java.util.Map.of("status", listing.getStatus().name()));
        if (previousPrice.compareTo(request.askingPrice()) != 0) {
            auditService.record(userId, "LISTING_ASKING_PRICE_CHANGED", "property_listings", listing.getId(),
                    java.util.Map.of("from", previousPrice.toPlainString(),
                            "to", request.askingPrice().toPlainString(), "currency", listing.getCurrency()));
        }
        return toResponse(listing);
    }

    @Transactional(readOnly = true)
    public List<ListingDtos.ListingResponse> brokerListings(UUID userId) {
        BrokerProfile broker = brokerFor(userId);
        List<PropertyListing> listings =
                listingRepository.findByBrokerProfileIdOrderByCreatedAtDesc(broker.getId());
        return toResponses(listings);
    }

    @Transactional(readOnly = true)
    public List<ListingDtos.ListingResponse> reviewQueue() {
        List<PropertyListing> listings = listingRepository.findByStatusInOrderBySubmittedAtAsc(
                List.of(ListingStatus.SUBMITTED, ListingStatus.UNDER_REVIEW));
        return toResponses(listings);
    }

    @Transactional
    public ListingDtos.ListingResponse changeStatus(UUID adminId, UUID listingId,
            ListingDtos.AdminStatusRequest request) {
        PropertyListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
        ListingStatus current = listing.getStatus();
        ListingStatus target = request.status();
        boolean validTransition = switch (current) {
            case SUBMITTED -> target == ListingStatus.UNDER_REVIEW
                    || target == ListingStatus.PUBLISHED || target == ListingStatus.REJECTED;
            case UNDER_REVIEW -> target == ListingStatus.PUBLISHED || target == ListingStatus.REJECTED;
            case PUBLISHED -> target == ListingStatus.SOLD || target == ListingStatus.UNAVAILABLE
                    || target == ListingStatus.ARCHIVED;
            default -> false;
        };
        if (!validTransition) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The requested listing status transition is not allowed");
        }
        if (target == ListingStatus.PUBLISHED) {
            BrokerProfile broker = brokerRepository.findById(listing.getBrokerProfileId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Broker profile not found"));
            if (broker.getApprovalStatus() != BrokerApprovalStatus.APPROVED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Broker approval is required before publishing a listing");
            }
        }
        listing.setStatus(target, adminId, request.reviewNote());
        listing = listingRepository.save(listing);
        auditService.record(adminId, "LISTING_STATUS_CHANGED", "property_listings", listing.getId(),
                java.util.Map.of("from", current.name(), "to", target.name(),
                        "reviewNote", request.reviewNote() == null ? "" : request.reviewNote()));
        return toResponse(listing);
    }

    private BrokerProfile brokerFor(UUID userId) {
        return brokerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Broker profile required"));
    }

    private ListingDtos.ListingResponse toResponse(PropertyListing listing) {
        LandParcel parcel = parcelRepository.findById(listing.getLandParcelId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Listing parcel is unavailable"));
        Location location = locationRepository.findById(parcel.getLocationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Listing location is unavailable"));
        return toResponse(listing, parcel, location);
    }

    private List<ListingDtos.ListingResponse> toResponses(List<PropertyListing> listings) {
        Set<UUID> parcelIds = listings.stream().map(PropertyListing::getLandParcelId).collect(Collectors.toSet());
        Map<UUID, LandParcel> parcels = parcelRepository.findAllById(parcelIds).stream()
                .collect(Collectors.toMap(LandParcel::getId, Function.identity()));
        Set<UUID> locationIds = parcels.values().stream().map(LandParcel::getLocationId).collect(Collectors.toSet());
        Map<UUID, Location> locations = locationRepository.findAllById(locationIds).stream()
                .collect(Collectors.toMap(Location::getId, Function.identity()));
        List<UUID> listingIds = listings.stream().map(PropertyListing::getId).toList();
        Map<UUID, List<ListingMedia>> mediaByListing = new java.util.HashMap<>();
        if (!listingIds.isEmpty()) {
            for (ListingMedia media : mediaRepository
                    .findByListingIdInOrderByListingIdAscSortOrderAscCreatedAtAsc(listingIds)) {
                mediaByListing.computeIfAbsent(media.getListingId(), ignored -> new ArrayList<>()).add(media);
            }
        }
        return listings.stream().map(listing -> {
            LandParcel parcel = parcels.get(listing.getLandParcelId());
            if (parcel == null) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Listing parcel is unavailable");
            }
            Location location = locations.get(parcel.getLocationId());
            if (location == null) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Listing location is unavailable");
            }
            return toResponseWithMediaEntities(
                    listing, parcel, location, mediaByListing.getOrDefault(listing.getId(), List.of()));
        }).toList();
    }

    private ListingDtos.ListingResponse toResponse(PropertyListing listing, LandParcel parcel,
            Location location) {
        List<ListingDtos.MediaResponse> media = mediaRepository
                .findByListingIdOrderBySortOrderAscCreatedAtAsc(listing.getId()).stream()
                .map(item -> toMediaResponse(listing, item))
                .toList();
        return buildResponse(listing, parcel, location, media);
    }

    private ListingDtos.ListingResponse toResponseWithMediaEntities(PropertyListing listing, LandParcel parcel,
            Location location, List<ListingMedia> listingMedia) {
        List<ListingDtos.MediaResponse> media = listingMedia.stream()
                .map(item -> toMediaResponse(listing, item))
                .toList();
        return buildResponse(listing, parcel, location, media);
    }

    private static ListingDtos.MediaResponse toMediaResponse(PropertyListing listing, ListingMedia item) {
        return new ListingDtos.MediaResponse(item.getId(),
                (listing.getStatus() == ListingStatus.PUBLISHED ? "/api/listings/" : "/api/broker/listings/")
                        + listing.getId() + "/media/" + item.getId(),
                item.getCaption(), item.getMediaType());
    }

    private static ListingDtos.ListingResponse buildResponse(PropertyListing listing, LandParcel parcel,
            Location location, List<ListingDtos.MediaResponse> media) {
        return new ListingDtos.ListingResponse(listing.getId(), listing.getTitle(), listing.getDescription(),
                listing.getAskingPrice(), listing.getCurrency(), listing.getStatus(), listing.getSourceType(),
                listing.getSourceReference(), listing.getCreatedAt(), listing.getUpdatedAt(),
                listing.getPublishedAt(), listing.getLastAvailabilityConfirmedAt(),
                new ListingDtos.ParcelResponse(parcel.getId(), location.getState(), location.getDistrict(),
                        location.getMandal(), location.getVillage(), parcel.getSurveyNumber(),
                        parcel.getSubdivisionNumber(), parcel.getExtent(), parcel.getMeasurementUnit(),
                        parcel.getLandType(), parcel.isBoundaryApproximate(), parcel.getLatitude(), parcel.getLongitude()),
                media);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static boolean hasTooLongFilter(String value) {
        return value != null && value.length() > 120;
    }
}
