package com.bhoomi.land;

import java.math.BigDecimal;
import java.util.UUID;

import org.locationtech.jts.geom.MultiPolygon;

import com.bhoomi.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "land_parcels")
public class LandParcel extends BaseEntity {

    @Column(name = "location_id", nullable = false)
    private UUID locationId;

    @Column(name = "survey_number", nullable = false, length = 80)
    private String surveyNumber;

    @Column(name = "subdivision_number", nullable = false, length = 80)
    private String subdivisionNumber = "";

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal extent;

    @Enumerated(EnumType.STRING)
    @Column(name = "measurement_unit", nullable = false, length = 24)
    private LandMeasurementUnit measurementUnit;

    @Enumerated(EnumType.STRING)
    @Column(name = "land_type", nullable = false, length = 40)
    private LandType landType;

    @Column(columnDefinition = "geometry(MultiPolygon,4326)")
    private MultiPolygon boundary;

    @Column(name = "boundary_is_approximate", nullable = false)
    private boolean boundaryIsApproximate = true;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    protected LandParcel() {
    }

    public LandParcel(UUID locationId, String surveyNumber, String subdivisionNumber,
            BigDecimal extent, LandMeasurementUnit measurementUnit, LandType landType) {
        this(locationId, surveyNumber, subdivisionNumber, extent, measurementUnit, landType, null, null);
    }

    public LandParcel(UUID locationId, String surveyNumber, String subdivisionNumber,
            BigDecimal extent, LandMeasurementUnit measurementUnit, LandType landType,
            BigDecimal latitude, BigDecimal longitude) {
        this.locationId = locationId;
        this.surveyNumber = surveyNumber;
        this.subdivisionNumber = subdivisionNumber == null ? "" : subdivisionNumber;
        this.extent = extent;
        this.measurementUnit = measurementUnit;
        this.landType = landType;
        if ((latitude == null) != (longitude == null)) {
            throw new IllegalArgumentException("Latitude and longitude must be provided together");
        }
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public UUID getLocationId() { return locationId; }
    public String getSurveyNumber() { return surveyNumber; }
    public String getSubdivisionNumber() { return subdivisionNumber; }
    public BigDecimal getExtent() { return extent; }
    public LandMeasurementUnit getMeasurementUnit() { return measurementUnit; }
    public LandType getLandType() { return landType; }
    public MultiPolygon getBoundary() { return boundary; }
    public boolean isBoundaryApproximate() { return boundaryIsApproximate; }
    public BigDecimal getLatitude() { return latitude; }
    public BigDecimal getLongitude() { return longitude; }

    public void setCoordinatesIfAbsent(BigDecimal latitude, BigDecimal longitude) {
        if (this.latitude == null && this.longitude == null && latitude != null && longitude != null) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}
