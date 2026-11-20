package com.bhoomi.document;

import java.util.UUID;
import java.time.Instant;

import com.bhoomi.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity
@Table(name = "land_documents")
public class LandDocument extends BaseEntity {

    @Column(name = "land_parcel_id", nullable = false)
    private UUID landParcelId;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @Column(name = "storage_key", nullable = false, unique = true, length = 500)
    private String storageKey;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 120)
    private String contentType;

    @Column(nullable = false, length = 40)
    private String provenance;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 16)
    private DocumentReviewStatus reviewStatus = DocumentReviewStatus.PENDING;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "review_note", length = 1000)
    private String reviewNote;

    protected LandDocument() {
    }

    public LandDocument(UUID landParcelId, UUID uploadedBy, String storageKey, String originalFilename,
            String contentType, String provenance, String description) {
        this.landParcelId = landParcelId;
        this.uploadedBy = uploadedBy;
        this.storageKey = storageKey;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.provenance = provenance;
        this.description = description;
    }

    public UUID getLandParcelId() { return landParcelId; }
    public UUID getUploadedBy() { return uploadedBy; }
    public String getStorageKey() { return storageKey; }
    public String getOriginalFilename() { return originalFilename; }
    public String getContentType() { return contentType; }
    public String getProvenance() { return provenance; }
    public String getDescription() { return description; }
    public DocumentReviewStatus getReviewStatus() { return reviewStatus; }
    public UUID getReviewedBy() { return reviewedBy; }
    public Instant getReviewedAt() { return reviewedAt; }
    public String getReviewNote() { return reviewNote; }

    public void review(DocumentReviewStatus status, UUID reviewerId, String note) {
        this.reviewStatus = status;
        this.reviewedBy = reviewerId;
        this.reviewedAt = Instant.now();
        this.reviewNote = note;
    }
}
