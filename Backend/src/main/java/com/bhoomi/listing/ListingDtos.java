package com.bhoomi.listing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.bhoomi.land.LandMeasurementUnit;
import com.bhoomi.land.LandType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ListingDtos {

    private ListingDtos() {
    }

    public record ParcelRequest(
            @NotBlank @Size(max = 100) String state,
            @NotBlank @Size(max = 120) String district,
            @NotBlank @Size(max = 120) String mandal,
            @NotBlank @Size(max = 120) String village,
            @NotBlank @Size(max = 80) String surveyNumber,
            @Size(max = 80) String subdivisionNumber,
            @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 14, fraction = 4) BigDecimal extent,
            @NotNull LandMeasurementUnit measurementUnit,
            @NotNull LandType landType,
            @DecimalMin("-90") @DecimalMax("90") @Digits(integer = 3, fraction = 6) BigDecimal latitude,
            @DecimalMin("-180") @DecimalMax("180") @Digits(integer = 3, fraction = 6) BigDecimal longitude) {
        public ParcelRequest(String state, String district, String mandal, String village,
                String surveyNumber, String subdivisionNumber, BigDecimal extent,
                LandMeasurementUnit measurementUnit, LandType landType) {
            this(state, district, mandal, village, surveyNumber, subdivisionNumber,
                    extent, measurementUnit, landType, null, null);
        }
    }

    public record CreateRequest(
            @NotBlank @Size(max = 180) String title,
            @NotBlank @Size(max = 5000) String description,
            @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 16, fraction = 2) BigDecimal askingPrice,
            @NotNull @Valid ParcelRequest parcel,
            @Size(max = 500) String sourceReference) {
        public CreateRequest(String title, String description, BigDecimal askingPrice, ParcelRequest parcel) {
            this(title, description, askingPrice, parcel, null);
        }
    }

    public record UpdateRequest(
            @NotBlank @Size(max = 180) String title,
            @NotBlank @Size(max = 5000) String description,
            @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 16, fraction = 2) BigDecimal askingPrice) {
    }

    public record AdminStatusRequest(@NotNull ListingStatus status, @Size(max = 1000) String reviewNote) {
    }

    public record ParcelResponse(
            UUID id, String state, String district, String mandal, String village,
            String surveyNumber, String subdivisionNumber, BigDecimal extent,
            LandMeasurementUnit measurementUnit, LandType landType, boolean boundaryIsApproximate,
            BigDecimal latitude, BigDecimal longitude) {
    }

    public record MediaResponse(UUID id, String url, String caption, String mediaType) {
    }

    public record ListingResponse(
            UUID id, String title, String description, BigDecimal askingPrice, String currency,
            ListingStatus status, ListingSourceType sourceType, String sourceReference,
            Instant createdAt, Instant updatedAt, Instant publishedAt, Instant lastAvailabilityConfirmedAt,
            ParcelResponse parcel, List<MediaResponse> media) {
    }

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    }
}
