package com.bhoomi.listing;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteVisitRepository extends JpaRepository<SiteVisit, UUID> {

    List<SiteVisit> findByBuyerIdOrderByRequestedAtDesc(UUID buyerId);

    List<SiteVisit> findByListingIdInOrderByRequestedAtDesc(List<UUID> listingIds);
}
