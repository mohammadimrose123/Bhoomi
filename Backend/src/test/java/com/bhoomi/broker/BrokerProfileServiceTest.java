package com.bhoomi.broker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class BrokerProfileServiceTest {

    @Test
    void updatePreservesAdministrativeApprovalStatus() {
        BrokerProfileRepository repository = mock(BrokerProfileRepository.class);
        BrokerProfileService service = new BrokerProfileService(repository);
        UUID userId = UUID.randomUUID();
        BrokerProfile profile = new BrokerProfile(userId, "Old business");
        ReflectionTestUtils.setField(profile, "id", UUID.randomUUID());
        profile.setApprovalStatus(BrokerApprovalStatus.APPROVED);
        profile.setLicenseNumber("OLD");
        when(repository.findByUserId(userId)).thenReturn(Optional.of(profile));

        BrokerProfileDtos.ProfileResponse response = service.update(userId,
                new BrokerProfileDtos.UpdateProfileRequest("  New business  ", "  LIC-42  "));

        assertThat(response.businessName()).isEqualTo("New business");
        assertThat(response.licenseNumber()).isEqualTo("LIC-42");
        assertThat(response.approvalStatus()).isEqualTo(BrokerApprovalStatus.APPROVED);
    }
}
