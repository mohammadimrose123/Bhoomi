package com.bhoomi.broker;

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
@RequestMapping("/api/broker/profile")
@Validated
public class BrokerProfileController {

    private final BrokerProfileService brokerProfileService;

    public BrokerProfileController(BrokerProfileService brokerProfileService) {
        this.brokerProfileService = brokerProfileService;
    }

    @GetMapping
    public BrokerProfileDtos.ProfileResponse get(@AuthenticationPrincipal Jwt jwt) {
        return brokerProfileService.get(userId(jwt));
    }

    @PutMapping
    public BrokerProfileDtos.ProfileResponse update(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody BrokerProfileDtos.UpdateProfileRequest request) {
        return brokerProfileService.update(userId(jwt), request);
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
