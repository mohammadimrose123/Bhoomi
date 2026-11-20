package com.bhoomi.broker;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BrokerProfileRepository extends JpaRepository<BrokerProfile, UUID> {

    Optional<BrokerProfile> findByUserId(UUID userId);

    List<BrokerProfile> findByApprovalStatusOrderByCreatedAtAsc(BrokerApprovalStatus approvalStatus);
}
