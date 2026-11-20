package com.bhoomi.listing;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PropertyListingRepository extends JpaRepository<PropertyListing, UUID>,
        JpaSpecificationExecutor<PropertyListing> {

    Page<PropertyListing> findByStatus(ListingStatus status, Pageable pageable);

    List<PropertyListing> findByBrokerProfileIdOrderByCreatedAtDesc(UUID brokerProfileId);

    List<PropertyListing> findByStatusInOrderBySubmittedAtAsc(List<ListingStatus> statuses);

    boolean existsByLandParcelIdAndStatusIn(UUID landParcelId, List<ListingStatus> statuses);
}
