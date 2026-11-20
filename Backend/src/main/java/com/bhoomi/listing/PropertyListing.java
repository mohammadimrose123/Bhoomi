package com.bhoomi.listing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.bhoomi.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "property_listings")
public class PropertyListing extends BaseEntity {

    @Column(name = "land_parcel_id", nullable = false)
    private UUID landParcelId;

    @Column(name = "broker_profile_id", nullable = false)
    private UUID brokerProfileId;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, length = 5000)
    private String description;

    @Column(name = "asking_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal askingPrice;

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ListingStatus status = ListingStatus.DRAFT;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "review_note", length = 1000)
    private String reviewNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 32)
    private ListingSourceType sourceType = ListingSourceType.BROKER_SUBMITTED;

    @Column(name = "source_reference", length = 500)
    private String sourceReference;

    @Column(name = "last_availability_confirmed_at")
    private Instant lastAvailabilityConfirmedAt;

    protected PropertyListing() {
    }

    public PropertyListing(UUID landParcelId, UUID brokerProfileId, String title, String description,
            BigDecimal askingPrice) {
        this(landParcelId, brokerProfileId, title, description, askingPrice, null);
    }

    public PropertyListing(UUID landParcelId, UUID brokerProfileId, String title, String description,
            BigDecimal askingPrice, String sourceReference) {
        this.landParcelId = landParcelId;
        this.brokerProfileId = brokerProfileId;
        this.title = title;
        this.description = description;
        this.askingPrice = askingPrice;
        this.sourceReference = sourceReference;
    }

    public UUID getLandParcelId() { return landParcelId; }
    public UUID getBrokerProfileId() { return brokerProfileId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public BigDecimal getAskingPrice() { return askingPrice; }
    public String getCurrency() { return currency; }
    public ListingStatus getStatus() { return status; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public UUID getReviewedBy() { return reviewedBy; }
    public String getReviewNote() { return reviewNote; }
    public ListingSourceType getSourceType() { return sourceType; }
    public String getSourceReference() { return sourceReference; }
    public Instant getLastAvailabilityConfirmedAt() { return lastAvailabilityConfirmedAt; }

    public void update(String title, String description, BigDecimal askingPrice) {
        this.title = title;
        this.description = description;
        this.askingPrice = askingPrice;
    }

    public void setStatus(ListingStatus status, UUID reviewerId, String reviewNote) {
        this.status = status;
        this.reviewedBy = reviewerId;
        this.reviewNote = reviewNote;
        if (status == ListingStatus.SUBMITTED) {
            this.submittedAt = Instant.now();
        }
        if (status == ListingStatus.PUBLISHED) {
            this.publishedAt = Instant.now();
        }
    }

    public void setBrokerAvailabilityStatus(ListingStatus status) {
        this.status = status;
        this.lastAvailabilityConfirmedAt = Instant.now();
    }

    public void confirmAvailability() {
        this.lastAvailabilityConfirmedAt = Instant.now();
    }
}
