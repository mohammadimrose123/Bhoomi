package com.bhoomi.broker;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BrokerProfileService {

    private final BrokerProfileRepository brokerRepository;

    public BrokerProfileService(BrokerProfileRepository brokerRepository) {
        this.brokerRepository = brokerRepository;
    }

    @Transactional(readOnly = true)
    public BrokerProfileDtos.ProfileResponse get(UUID userId) {
        return toResponse(findProfile(userId));
    }

    @Transactional
    public BrokerProfileDtos.ProfileResponse update(UUID userId, BrokerProfileDtos.UpdateProfileRequest request) {
        BrokerProfile profile = findProfile(userId);
        String licenseNumber = request.licenseNumber() == null || request.licenseNumber().isBlank()
                ? null : request.licenseNumber().trim();
        profile.updateProfile(request.businessName().trim(), licenseNumber);
        return toResponse(profile);
    }

    private BrokerProfile findProfile(UUID userId) {
        return brokerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Broker profile required"));
    }

    private static BrokerProfileDtos.ProfileResponse toResponse(BrokerProfile profile) {
        return new BrokerProfileDtos.ProfileResponse(profile.getId(), profile.getBusinessName(),
                profile.getLicenseNumber(), profile.getApprovalStatus());
    }
}
