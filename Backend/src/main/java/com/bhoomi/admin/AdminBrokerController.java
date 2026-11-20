package com.bhoomi.admin;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bhoomi.broker.BrokerApprovalStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/admin/brokers")
@Validated
public class AdminBrokerController {

    private final AdminBrokerService adminBrokerService;

    public AdminBrokerController(AdminBrokerService adminBrokerService) {
        this.adminBrokerService = adminBrokerService;
    }

    @GetMapping
    public java.util.List<BrokerQueueItem> list(
            @RequestParam(defaultValue = "PENDING") BrokerApprovalStatus status) {
        return adminBrokerService.list(status);
    }

    @PatchMapping("/{id}/status")
    public BrokerStatusResponse changeStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody BrokerStatusRequest request) {
        return adminBrokerService.changeStatus(UUID.fromString(jwt.getSubject()), id, request);
    }

    public record BrokerStatusRequest(
            @NotNull BrokerApprovalStatus status,
            @Size(max = 1000) String reviewNote) {
    }

    public record BrokerStatusResponse(UUID id, String businessName, BrokerApprovalStatus approvalStatus) {
    }

    public record BrokerQueueItem(UUID id, UUID userId, String businessName,
            String licenseNumber, BrokerApprovalStatus approvalStatus) {
    }
}
