package com.bhoomi.listing;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class SiteVisitDtos {

    private SiteVisitDtos() {
    }

    public record SiteVisitRequest(
            @NotNull @Future Instant requestedAt,
            @NotBlank @Size(max = 1000) String notes) {
    }

    public record SiteVisitStatusRequest(
            @NotNull SiteVisitStatus status,
            @Future Instant requestedAt,
            @Size(max = 1000) String notes) {
    }

    public record SiteVisitResponse(
            UUID id,
            UUID listingId,
            UUID buyerId,
            Instant requestedAt,
            SiteVisitStatus status,
            String notes,
            Instant createdAt) {
    }
}
