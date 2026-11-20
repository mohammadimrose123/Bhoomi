package com.bhoomi.admin;

import java.util.Map;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.bhoomi.broker.BrokerApprovalStatus;
import com.bhoomi.broker.BrokerProfile;
import com.bhoomi.broker.BrokerProfileRepository;
import com.bhoomi.common.AuditService;

@Service
public class AdminBrokerService {

    private final BrokerProfileRepository brokerRepository;
    private final AuditService auditService;

    public AdminBrokerService(BrokerProfileRepository brokerRepository, AuditService auditService) {
        this.brokerRepository = brokerRepository;
        this.auditService = auditService;
    }

    @Transactional
    public AdminBrokerController.BrokerStatusResponse changeStatus(UUID adminId, UUID brokerId,
            AdminBrokerController.BrokerStatusRequest request) {
        if (request.status() == BrokerApprovalStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Admin decisions must be APPROVED or REJECTED");
        }
        BrokerProfile profile = brokerRepository.findById(brokerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Broker profile not found"));
        BrokerApprovalStatus previousStatus = profile.getApprovalStatus();
        profile.setApprovalStatus(request.status());
        profile = brokerRepository.save(profile);
        auditService.record(adminId, "BROKER_APPROVAL_CHANGED", "broker_profiles", profile.getId(),
                Map.of("from", previousStatus.name(), "to", request.status().name(),
                        "reviewNote", request.reviewNote() == null ? "" : request.reviewNote()));
        return new AdminBrokerController.BrokerStatusResponse(
                profile.getId(), profile.getBusinessName(), profile.getApprovalStatus());
    }

    @Transactional(readOnly = true)
    public List<AdminBrokerController.BrokerQueueItem> list(BrokerApprovalStatus status) {
        return brokerRepository.findByApprovalStatusOrderByCreatedAtAsc(status).stream()
                .map(profile -> new AdminBrokerController.BrokerQueueItem(profile.getId(), profile.getUserId(),
                        profile.getBusinessName(), profile.getLicenseNumber(), profile.getApprovalStatus()))
                .toList();
    }
}
