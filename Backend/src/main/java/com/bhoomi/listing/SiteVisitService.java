package com.bhoomi.listing;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.bhoomi.broker.BrokerProfile;
import com.bhoomi.broker.BrokerProfileRepository;

@Service
public class SiteVisitService {

    private final SiteVisitRepository siteVisitRepository;
    private final PropertyListingRepository listingRepository;
    private final BrokerProfileRepository brokerRepository;

    public SiteVisitService(SiteVisitRepository siteVisitRepository,
            PropertyListingRepository listingRepository,
            BrokerProfileRepository brokerRepository) {
        this.siteVisitRepository = siteVisitRepository;
        this.listingRepository = listingRepository;
        this.brokerRepository = brokerRepository;
    }

    @Transactional
    public SiteVisitDtos.SiteVisitResponse create(UUID buyerId, UUID listingId, SiteVisitDtos.SiteVisitRequest request) {
        if (request.requestedAt() == null || !request.requestedAt().isAfter(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Requested site-visit time must be in the future");
        }
        PropertyListing listing = listingRepository.findById(listingId)
                .filter(candidate -> candidate.getStatus() == ListingStatus.PUBLISHED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
        String trimmedNotes = request.notes() == null ? null : request.notes().trim();
        SiteVisit visit = siteVisitRepository.save(new SiteVisit(
                listing.getId(), buyerId, request.requestedAt(), SiteVisitStatus.REQUESTED, trimmedNotes));
        return toResponse(visit);
    }

    @Transactional(readOnly = true)
    public List<SiteVisitDtos.SiteVisitResponse> forBuyer(UUID buyerId) {
        return siteVisitRepository.findByBuyerIdOrderByRequestedAtDesc(buyerId).stream()
                .map(SiteVisitService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SiteVisitDtos.SiteVisitResponse> forBroker(UUID brokerUserId) {
        BrokerProfile profile = brokerRepository.findByUserId(brokerUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Broker profile required"));
        List<UUID> listingIds = listingRepository.findByBrokerProfileIdOrderByCreatedAtDesc(profile.getId()).stream()
                .map(PropertyListing::getId)
                .toList();
        if (listingIds.isEmpty()) {
            return List.of();
        }
        return siteVisitRepository.findByListingIdInOrderByRequestedAtDesc(listingIds).stream()
                .map(SiteVisitService::toResponse)
                .toList();
    }

    @Transactional
    public SiteVisitDtos.SiteVisitResponse updateBrokerStatus(UUID brokerUserId, UUID siteVisitId,
            SiteVisitDtos.SiteVisitStatusRequest request) {
        BrokerProfile profile = brokerRepository.findByUserId(brokerUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Broker profile required"));
        SiteVisit visit = siteVisitRepository.findById(siteVisitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Site visit not found"));
        PropertyListing listing = listingRepository.findById(visit.getListingId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
        if (!listing.getBrokerProfileId().equals(profile.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This listing does not belong to this broker");
        }
        SiteVisitStatus currentStatus = visit.getStatus();
        SiteVisitStatus requestedStatus = request.status();
        boolean validTransition = switch (currentStatus) {
            case REQUESTED -> requestedStatus == SiteVisitStatus.CONFIRMED
                    || requestedStatus == SiteVisitStatus.CANCELLED;
            case CONFIRMED -> requestedStatus == SiteVisitStatus.COMPLETED
                    || requestedStatus == SiteVisitStatus.CANCELLED
                    || (requestedStatus == SiteVisitStatus.CONFIRMED && request.requestedAt() != null);
            case COMPLETED, CANCELLED -> false;
        };
        if (!validTransition) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The requested site-visit status transition is not allowed");
        }
        if (request.requestedAt() != null) {
            if (!request.requestedAt().isAfter(Instant.now())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Requested site-visit time must be in the future");
            }
            visit.setRequestedAt(request.requestedAt());
        }
        if (request.notes() != null && !request.notes().isBlank()) {
            visit.setNotes(request.notes().trim());
        }
        visit.setStatus(requestedStatus);
        return toResponse(siteVisitRepository.save(visit));
    }

    private static SiteVisitDtos.SiteVisitResponse toResponse(SiteVisit siteVisit) {
        return new SiteVisitDtos.SiteVisitResponse(siteVisit.getId(), siteVisit.getListingId(),
                siteVisit.getBuyerId(), siteVisit.getRequestedAt(), siteVisit.getStatus(),
                siteVisit.getNotes(), siteVisit.getCreatedAt());
    }
}
