package com.bhoomi.user;

import java.time.Instant;
import java.util.UUID;

public final class FavoriteDtos {

    private FavoriteDtos() {
    }

    public record FavoriteItemResponse(UUID listingId, Instant createdAt) {
    }

    public record FavoriteToggleResponse(UUID listingId, boolean saved, long totalFavorites) {
    }
}
