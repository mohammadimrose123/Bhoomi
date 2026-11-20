package com.bhoomi.broker;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class BrokerProfileDtos {

    private BrokerProfileDtos() {
    }

    public record ProfileResponse(UUID id, String businessName, String licenseNumber,
            BrokerApprovalStatus approvalStatus) {
    }

    public record UpdateProfileRequest(
            @NotBlank @Size(max = 160) String businessName,
            @Size(max = 100) String licenseNumber) {
    }
}
