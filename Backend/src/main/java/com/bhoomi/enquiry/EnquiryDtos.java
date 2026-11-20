package com.bhoomi.enquiry;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class EnquiryDtos {

    private EnquiryDtos() {
    }

    public record CreateRequest(
            @NotBlank @Size(min = 10, max = 2000) String message,
            @Size(max = 32) String contactPhone) {
    }

    public record EnquiryResponse(
            UUID id, UUID listingId, UUID buyerId, String message, String contactPhone,
            EnquiryStatus status, Instant createdAt) {
    }
}
