package com.bhoomi.enquiry;

import java.util.UUID;

import com.bhoomi.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "enquiries")
public class Enquiry extends BaseEntity {

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(name = "buyer_id", nullable = false)
    private UUID buyerId;

    @Column(nullable = false, length = 2000)
    private String message;

    @Column(name = "contact_phone", length = 32)
    private String contactPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EnquiryStatus status = EnquiryStatus.OPEN;

    protected Enquiry() {
    }

    public Enquiry(UUID listingId, UUID buyerId, String message, String contactPhone) {
        this.listingId = listingId;
        this.buyerId = buyerId;
        this.message = message;
        this.contactPhone = contactPhone;
    }

    public UUID getListingId() { return listingId; }
    public UUID getBuyerId() { return buyerId; }
    public String getMessage() { return message; }
    public String getContactPhone() { return contactPhone; }
    public EnquiryStatus getStatus() { return status; }
}
