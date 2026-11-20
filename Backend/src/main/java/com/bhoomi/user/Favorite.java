package com.bhoomi.user;

import java.util.UUID;

import com.bhoomi.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "favorites")
public class Favorite extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    protected Favorite() {
    }

    public Favorite(UUID userId, UUID listingId) {
        this.userId = userId;
        this.listingId = listingId;
    }

    public UUID getUserId() { return userId; }
    public UUID getListingId() { return listingId; }
}
