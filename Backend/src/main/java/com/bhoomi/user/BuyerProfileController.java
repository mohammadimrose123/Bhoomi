package com.bhoomi.user;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/buyer/profile")
@Validated
public class BuyerProfileController {

    private final BuyerProfileService buyerProfileService;

    public BuyerProfileController(BuyerProfileService buyerProfileService) {
        this.buyerProfileService = buyerProfileService;
    }

    @GetMapping
    public BuyerProfileDtos.ProfileResponse get(@AuthenticationPrincipal Jwt jwt) {
        return buyerProfileService.get(userId(jwt));
    }

    @PutMapping
    public BuyerProfileDtos.ProfileResponse update(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody BuyerProfileDtos.UpdateProfileRequest request) {
        return buyerProfileService.update(userId(jwt), request);
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
