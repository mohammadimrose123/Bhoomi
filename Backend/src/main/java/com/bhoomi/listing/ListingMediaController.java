package com.bhoomi.listing;

import java.util.List;
import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api")
public class ListingMediaController {

    private final ListingMediaService mediaService;

    public ListingMediaController(ListingMediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping(value = "/broker/listings/{listingId}/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ListingMediaDtos.MediaResponse upload(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID listingId, @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) @Size(max = 300) String caption) {
        return mediaService.upload(UUID.fromString(jwt.getSubject()), listingId, file, caption);
    }

    @GetMapping("/listings/{listingId}/media")
    public List<ListingMediaDtos.MediaResponse> publicMedia(@PathVariable UUID listingId) {
        return mediaService.publicMedia(listingId);
    }

    @GetMapping("/listings/{listingId}/media/{mediaId}")
    public ResponseEntity<byte[]> readPublic(@PathVariable UUID listingId, @PathVariable UUID mediaId) {
        return content(mediaService.readPublic(listingId, mediaId));
    }

    @GetMapping("/broker/listings/{listingId}/media/{mediaId}")
    public ResponseEntity<byte[]> readForBroker(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID listingId, @PathVariable UUID mediaId) {
        return content(mediaService.readForBroker(UUID.fromString(jwt.getSubject()), listingId, mediaId));
    }

    private static ResponseEntity<byte[]> content(ListingMediaService.ImageContent image) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.noCache())
                .header("X-Content-Type-Options", "nosniff")
                .body(image.content());
    }
}
