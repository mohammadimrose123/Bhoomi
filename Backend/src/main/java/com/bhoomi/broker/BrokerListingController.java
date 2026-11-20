package com.bhoomi.broker;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.bhoomi.listing.ListingDtos;
import com.bhoomi.listing.ListingStatus;
import com.bhoomi.listing.ListingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/broker/listings")
public class BrokerListingController {

    private final ListingService listingService;

    public BrokerListingController(ListingService listingService) {
        this.listingService = listingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ListingDtos.ListingResponse create(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ListingDtos.CreateRequest request) {
        return listingService.create(userId(jwt), request);
    }

    @PostMapping("/drafts")
    @ResponseStatus(HttpStatus.CREATED)
    public ListingDtos.ListingResponse createDraft(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ListingDtos.CreateRequest request) {
        return listingService.createDraft(userId(jwt), request);
    }

    @PostMapping("/{id}/submit")
    public ListingDtos.ListingResponse submit(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return listingService.submit(userId(jwt), id);
    }

    @PatchMapping("/{id}/status")
    public ListingDtos.ListingResponse updateStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody BrokerListingStatusRequest request) {
        return listingService.changeBrokerStatus(userId(jwt), id, request.status());
    }

    @PostMapping("/{id}/availability/confirm")
    public ListingDtos.ListingResponse confirmAvailability(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return listingService.confirmAvailability(userId(jwt), id);
    }

    @PutMapping("/{id}")
    public ListingDtos.ListingResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody ListingDtos.UpdateRequest request) {
        return listingService.update(userId(jwt), id, request);
    }

    @GetMapping
    public List<ListingDtos.ListingResponse> mine(@AuthenticationPrincipal Jwt jwt) {
        return listingService.brokerListings(userId(jwt));
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    public record BrokerListingStatusRequest(@jakarta.validation.constraints.NotNull ListingStatus status) {
    }
}
