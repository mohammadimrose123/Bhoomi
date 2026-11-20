package com.bhoomi.listing;

import java.util.UUID;

public final class ListingMediaDtos {

    private ListingMediaDtos() {
    }

    public record MediaResponse(UUID id, String url, String caption, String mediaType) {
    }
}
