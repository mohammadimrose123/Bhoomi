package com.bhoomi.user;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BuyerProfileService {

    private final UserRepository userRepository;

    public BuyerProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public BuyerProfileDtos.ProfileResponse get(UUID userId) {
        return toResponse(findUser(userId));
    }

    @Transactional
    public BuyerProfileDtos.ProfileResponse update(UUID userId, BuyerProfileDtos.UpdateProfileRequest request) {
        UserEntity user = findUser(userId);
        String phone = request.phone() == null || request.phone().isBlank() ? null : request.phone().trim();
        user.updateProfile(request.fullName().trim(), phone);
        return toResponse(user);
    }

    private UserEntity findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Buyer profile not found"));
    }

    private static BuyerProfileDtos.ProfileResponse toResponse(UserEntity user) {
        return new BuyerProfileDtos.ProfileResponse(user.getId(), user.getEmail(), user.getFullName(), user.getPhone());
    }
}
