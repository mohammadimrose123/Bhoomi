package com.bhoomi.enquiry;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.bhoomi.broker.BrokerProfile;
import com.bhoomi.broker.BrokerProfileRepository;
import com.bhoomi.listing.ListingStatus;
import com.bhoomi.listing.PropertyListing;
import com.bhoomi.listing.PropertyListingRepository;

@Service
public class EnquiryService {

    private final EnquiryRepository enquiryRepository;
    private final PropertyListingRepository listingRepository;
    private final BrokerProfileRepository brokerRepository;

    public EnquiryService(EnquiryRepository enquiryRepository, PropertyListingRepository listingRepository,
            BrokerProfileRepository brokerRepository) {
        this.enquiryRepository = enquiryRepository;
        this.listingRepository = listingRepository;
        this.brokerRepository = brokerRepository;
    }

    @Transactional
    public EnquiryDtos.EnquiryResponse create(UUID buyerId, UUID listingId, EnquiryDtos.CreateRequest request) {
        PropertyListing listing = listingRepository.findById(listingId)
                .filter(candidate -> candidate.getStatus() == ListingStatus.PUBLISHED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
        Enquiry enquiry = enquiryRepository.save(new Enquiry(listing.getId(), buyerId,
                request.message().trim(), request.contactPhone() == null ? null : request.contactPhone().trim()));
        return toResponse(enquiry);
    }

    @Transactional(readOnly = true)
    public List<EnquiryDtos.EnquiryResponse> forBroker(UUID userId) {
        BrokerProfile profile = brokerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Broker profile required"));
        List<UUID> listingIds = listingRepository.findByBrokerProfileIdOrderByCreatedAtDesc(profile.getId()).stream()
                .map(PropertyListing::getId).toList();
        if (listingIds.isEmpty()) {
            return List.of();
        }
        return enquiryRepository.findByListingIdInOrderByCreatedAtDesc(listingIds).stream()
                .map(EnquiryService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<EnquiryDtos.EnquiryResponse> forBuyer(UUID buyerId) {
        return enquiryRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId).stream()
                .map(EnquiryService::toResponse).toList();
    }

    private static EnquiryDtos.EnquiryResponse toResponse(Enquiry enquiry) {
        return new EnquiryDtos.EnquiryResponse(enquiry.getId(), enquiry.getListingId(), enquiry.getBuyerId(),
                enquiry.getMessage(), enquiry.getContactPhone(), enquiry.getStatus(), enquiry.getCreatedAt());
    }
}
