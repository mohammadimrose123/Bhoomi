package com.bhoomi.document;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
@Validated
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(value = "/broker/listings/{listingId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentDtos.DocumentResponse upload(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID listingId, @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String description) {
        return documentService.upload(userId(jwt), listingId, file, description);
    }

    @GetMapping("/broker/listings/{listingId}/documents")
    public List<DocumentDtos.DocumentResponse> brokerListingDocuments(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID listingId) {
        return documentService.forBrokerListing(userId(jwt), listingId);
    }

    @GetMapping("/broker/documents/{id}/download")
    public ResponseEntity<byte[]> downloadForBroker(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return download(documentService.downloadForBroker(userId(jwt), id));
    }

    @GetMapping("/admin/documents")
    public List<DocumentDtos.DocumentResponse> adminQueue(
            @RequestParam(defaultValue = "PENDING") DocumentReviewStatus status) {
        return documentService.adminQueue(status);
    }

    @PatchMapping("/admin/documents/{id}/review")
    public DocumentDtos.DocumentResponse review(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody DocumentDtos.ReviewRequest request) {
        return documentService.review(userId(jwt), id, request);
    }

    @GetMapping("/admin/documents/{id}/download")
    public ResponseEntity<byte[]> downloadForAdmin(@PathVariable UUID id) {
        return download(documentService.downloadForAdmin(id));
    }

    private static ResponseEntity<byte[]> download(DocumentService.Download download) {
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.metadata().originalFilename(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(download.content());
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
