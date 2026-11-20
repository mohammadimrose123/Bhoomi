package com.bhoomi.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class BuyerProfileServiceTest {

    @Test
    void updateChangesOnlyTheAuthenticatedUsersProfile() {
        UserRepository userRepository = mock(UserRepository.class);
        BuyerProfileService service = new BuyerProfileService(userRepository);
        UUID userId = UUID.randomUUID();
        UserEntity user = new UserEntity("buyer@example.test", "  ", "encoded", "Old Name", UserRole.BUYER);
        ReflectionTestUtils.setField(user, "id", userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        BuyerProfileDtos.ProfileResponse response = service.update(userId,
                new BuyerProfileDtos.UpdateProfileRequest("  New Name  ", "  +919876543210  "));

        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.fullName()).isEqualTo("New Name");
        assertThat(response.phone()).isEqualTo("+919876543210");
        assertThat(user.getEmail()).isEqualTo("buyer@example.test");
        assertThat(user.getPasswordHash()).isEqualTo("encoded");
    }

    @Test
    void blankPhoneIsNormalizedToNull() {
        UserRepository userRepository = mock(UserRepository.class);
        BuyerProfileService service = new BuyerProfileService(userRepository);
        UUID userId = UUID.randomUUID();
        UserEntity user = new UserEntity("buyer@example.test", "+919876543210", "encoded", "Buyer", UserRole.BUYER);
        ReflectionTestUtils.setField(user, "id", userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        BuyerProfileDtos.ProfileResponse response = service.update(userId,
                new BuyerProfileDtos.UpdateProfileRequest("Buyer", "  "));

        assertThat(response.phone()).isNull();
    }
}
