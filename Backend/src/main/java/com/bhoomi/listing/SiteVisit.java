package com.bhoomi.listing;

import java.time.Instant;
import java.util.UUID;

import com.bhoomi.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "site_visits")
public class SiteVisit extends BaseEntity {

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(name = "buyer_id", nullable = false)
    private UUID buyerId;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SiteVisitStatus status = SiteVisitStatus.REQUESTED;

    @Column(length = 1000)
    private String notes;

    protected SiteVisit() {
    }

    public SiteVisit(UUID listingId, UUID buyerId, Instant requestedAt, SiteVisitStatus status, String notes) {
        this.listingId = listingId;
        this.buyerId = buyerId;
        this.requestedAt = requestedAt;
        this.status = status;
        this.notes = notes;
    }

    public UUID getListingId() { return listingId; }
    public UUID getBuyerId() { return buyerId; }
    public Instant getRequestedAt() { return requestedAt; }
    public SiteVisitStatus getStatus() { return status; }
    public String getNotes() { return notes; }

    public void setStatus(SiteVisitStatus status) {
        this.status = status;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
