package com.bhoomi.listing;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class SiteVisitController {

    private final SiteVisitService siteVisitService;

    public SiteVisitController(SiteVisitService siteVisitService) {
        this.siteVisitService = siteVisitService;
    }

    @PostMapping("/listings/{id}/site-visits")
    @ResponseStatus(HttpStatus.CREATED)
    public SiteVisitDtos.SiteVisitResponse create(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID listingId,
            @Valid @RequestBody SiteVisitDtos.SiteVisitRequest request) {
        return siteVisitService.create(UUID.fromString(jwt.getSubject()), listingId, request);
    }

    @GetMapping("/buyer/site-visits")
    public List<SiteVisitDtos.SiteVisitResponse> buyer(@AuthenticationPrincipal Jwt jwt) {
        return siteVisitService.forBuyer(UUID.fromString(jwt.getSubject()));
    }

    @GetMapping("/broker/site-visits")
    public List<SiteVisitDtos.SiteVisitResponse> broker(@AuthenticationPrincipal Jwt jwt) {
        return siteVisitService.forBroker(UUID.fromString(jwt.getSubject()));
    }

    @PatchMapping("/broker/site-visits/{id}/status")
    public SiteVisitDtos.SiteVisitResponse brokerStatus(@AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") UUID siteVisitId,
            @Valid @RequestBody SiteVisitDtos.SiteVisitStatusRequest request) {
        return siteVisitService.updateBrokerStatus(UUID.fromString(jwt.getSubject()), siteVisitId, request);
    }
}
