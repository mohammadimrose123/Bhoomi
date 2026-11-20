package com.bhoomi.broker;

import java.util.UUID;

import com.bhoomi.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "broker_profiles")
public class BrokerProfile extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "business_name", nullable = false, length = 160)
    private String businessName;

    @Column(name = "license_number", length = 100)
    private String licenseNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 16)
    private BrokerApprovalStatus approvalStatus = BrokerApprovalStatus.PENDING;

    protected BrokerProfile() {
    }

    public BrokerProfile(UUID userId, String businessName) {
        this.userId = userId;
        this.businessName = businessName;
    }

    public UUID getUserId() { return userId; }
    public String getBusinessName() { return businessName; }
    public String getLicenseNumber() { return licenseNumber; }
    public BrokerApprovalStatus getApprovalStatus() { return approvalStatus; }
    public void updateProfile(String businessName, String licenseNumber) {
        this.businessName = businessName;
        this.licenseNumber = licenseNumber;
    }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }
    public void setApprovalStatus(BrokerApprovalStatus approvalStatus) {
        this.approvalStatus = approvalStatus;
    }
}
