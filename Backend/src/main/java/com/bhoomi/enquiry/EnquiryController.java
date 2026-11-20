package com.bhoomi.enquiry;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class EnquiryController {

    private final EnquiryService enquiryService;

    public EnquiryController(EnquiryService enquiryService) {
        this.enquiryService = enquiryService;
    }

    @PostMapping("/listings/{id}/enquiries")
    @ResponseStatus(HttpStatus.CREATED)
    public EnquiryDtos.EnquiryResponse create(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody EnquiryDtos.CreateRequest request) {
        return enquiryService.create(UUID.fromString(jwt.getSubject()), id, request);
    }

    @GetMapping("/buyer/enquiries")
    public List<EnquiryDtos.EnquiryResponse> buyerEnquiries(@AuthenticationPrincipal Jwt jwt) {
        return enquiryService.forBuyer(UUID.fromString(jwt.getSubject()));
    }

    @GetMapping("/broker/enquiries")
    public List<EnquiryDtos.EnquiryResponse> brokerEnquiries(@AuthenticationPrincipal Jwt jwt) {
        return enquiryService.forBroker(UUID.fromString(jwt.getSubject()));
    }
}
