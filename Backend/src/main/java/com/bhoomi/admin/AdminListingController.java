package com.bhoomi.admin;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bhoomi.listing.ListingDtos;
import com.bhoomi.listing.ListingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin")
public class AdminListingController {

    private final ListingService listingService;

    public AdminListingController(ListingService listingService) {
        this.listingService = listingService;
    }

    @GetMapping("/reviews")
    public List<ListingDtos.ListingResponse> reviews() {
        return listingService.reviewQueue();
    }

    @PatchMapping("/listings/{id}/status")
    public ListingDtos.ListingResponse changeStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody ListingDtos.AdminStatusRequest request) {
        return listingService.changeStatus(UUID.fromString(jwt.getSubject()), id, request);
    }
}
