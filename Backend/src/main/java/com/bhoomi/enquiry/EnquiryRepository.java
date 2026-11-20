package com.bhoomi.enquiry;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EnquiryRepository extends JpaRepository<Enquiry, UUID> {

    List<Enquiry> findByListingIdInOrderByCreatedAtDesc(List<UUID> listingIds);

    List<Enquiry> findByBuyerIdOrderByCreatedAtDesc(UUID buyerId);
}
