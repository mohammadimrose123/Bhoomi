package com.bhoomi.listing;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ListingMediaRepository extends JpaRepository<ListingMedia, UUID> {

    List<ListingMedia> findByListingIdOrderBySortOrderAscCreatedAtAsc(UUID listingId);

    List<ListingMedia> findByListingIdInOrderByListingIdAscSortOrderAscCreatedAtAsc(List<UUID> listingIds);

    Optional<ListingMedia> findByIdAndListingId(UUID id, UUID listingId);

    long countByListingId(UUID listingId);
}
