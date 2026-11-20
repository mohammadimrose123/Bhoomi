package com.bhoomi.document;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class DocumentDtos {

    private DocumentDtos() {
    }

    public record DocumentResponse(
            UUID id,
            UUID landParcelId,
            String originalFilename,
            String contentType,
            String provenance,
            String description,
            DocumentReviewStatus reviewStatus,
            Instant reviewedAt,
            String reviewNote,
            Instant createdAt) {
    }

    public record ReviewRequest(
            @NotNull DocumentReviewStatus status,
            @Size(max = 1000) String reviewNote) {
    }
}
