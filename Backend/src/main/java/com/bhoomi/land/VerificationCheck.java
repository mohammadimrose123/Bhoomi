package com.bhoomi.land;

import java.time.Instant;
import java.util.UUID;

import com.bhoomi.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "verification_checks")
public class VerificationCheck extends BaseEntity {

    @Column(name = "land_parcel_id", nullable = false)
    private UUID landParcelId;

    @Column(name = "checked_by", nullable = false)
    private UUID checkedBy;

    @Column(name = "check_type", nullable = false, length = 80)
    private String checkType;

    @Column(nullable = false, length = 24)
    private String status;

    @Column(name = "evidence_reference", length = 500)
    private String evidenceReference;

    @Column(length = 2000)
    private String notes;

    @Column(name = "checked_at", nullable = false)
    private Instant checkedAt;

    protected VerificationCheck() {
    }

    public UUID getLandParcelId() { return landParcelId; }
    public UUID getCheckedBy() { return checkedBy; }
    public String getCheckType() { return checkType; }
    public String getStatus() { return status; }
    public String getEvidenceReference() { return evidenceReference; }
    public String getNotes() { return notes; }
    public Instant getCheckedAt() { return checkedAt; }
}
