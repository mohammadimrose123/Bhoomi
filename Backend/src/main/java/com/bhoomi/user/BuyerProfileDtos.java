package com.bhoomi.user;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class BuyerProfileDtos {

    private BuyerProfileDtos() {
    }

    public record ProfileResponse(UUID id, String email, String fullName, String phone) {
    }

    public record UpdateProfileRequest(
            @NotBlank @Size(min = 2, max = 160) String fullName,
            @Size(max = 32) String phone) {
    }
}
