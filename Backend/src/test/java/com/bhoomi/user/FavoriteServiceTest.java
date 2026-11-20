package com.bhoomi.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.bhoomi.listing.PropertyListingRepository;

class FavoriteServiceTest {

    @Test
    void toggleAddsFavoriteWhenMissing() {
        FavoriteRepository favoriteRepository = mock(FavoriteRepository.class);
        PropertyListingRepository listingRepository = mock(PropertyListingRepository.class);
        FavoriteService service = new FavoriteService(favoriteRepository, listingRepository);

        UUID userId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        when(listingRepository.existsById(listingId)).thenReturn(true);
        when(favoriteRepository.findByUserIdAndListingId(userId, listingId)).thenReturn(Optional.empty());
        when(favoriteRepository.countByUserId(userId)).thenReturn(1L);

        FavoriteDtos.FavoriteToggleResponse response = service.toggle(userId, listingId);

        assertThat(response.listingId()).isEqualTo(listingId);
        assertThat(response.saved()).isTrue();
        assertThat(response.totalFavorites()).isEqualTo(1L);
        verify(favoriteRepository).save(any(Favorite.class));
    }

    @Test
    void listReturnsSavedListingsInNewestFirst() {
        FavoriteRepository favoriteRepository = mock(FavoriteRepository.class);
        PropertyListingRepository listingRepository = mock(PropertyListingRepository.class);
        FavoriteService service = new FavoriteService(favoriteRepository, listingRepository);

        UUID userId = UUID.randomUUID();
        UUID firstListingId = UUID.randomUUID();
        UUID secondListingId = UUID.randomUUID();
        Instant now = Instant.now();
        when(favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(new Favorite(userId, secondListingId), new Favorite(userId, firstListingId)));

        List<FavoriteDtos.FavoriteItemResponse> favorites = service.list(userId);

        assertThat(favorites).hasSize(2);
        assertThat(favorites.get(0).listingId()).isEqualTo(secondListingId);
        assertThat(favorites.get(1).listingId()).isEqualTo(firstListingId);
    }

    @Test
    void removeDeletesExistingFavoriteAndNeverCreatesAnother() {
        FavoriteRepository favoriteRepository = mock(FavoriteRepository.class);
        PropertyListingRepository listingRepository = mock(PropertyListingRepository.class);
        FavoriteService service = new FavoriteService(favoriteRepository, listingRepository);

        UUID userId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        Favorite favorite = new Favorite(userId, listingId);
        when(favoriteRepository.findByUserIdAndListingId(userId, listingId)).thenReturn(Optional.of(favorite));
        when(favoriteRepository.countByUserId(userId)).thenReturn(0L);

        FavoriteDtos.FavoriteToggleResponse response = service.remove(userId, listingId);

        assertThat(response.saved()).isFalse();
        assertThat(response.totalFavorites()).isZero();
        verify(favoriteRepository).delete(favorite);
        verify(favoriteRepository, never()).save(any(Favorite.class));
    }

    @Test
    void removeIsIdempotentWhenFavoriteDoesNotExist() {
        FavoriteRepository favoriteRepository = mock(FavoriteRepository.class);
        PropertyListingRepository listingRepository = mock(PropertyListingRepository.class);
        FavoriteService service = new FavoriteService(favoriteRepository, listingRepository);

        UUID userId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        when(favoriteRepository.findByUserIdAndListingId(userId, listingId)).thenReturn(Optional.empty());
        when(favoriteRepository.countByUserId(userId)).thenReturn(0L);

        FavoriteDtos.FavoriteToggleResponse response = service.remove(userId, listingId);

        assertThat(response.saved()).isFalse();
        assertThat(response.totalFavorites()).isZero();
        verify(favoriteRepository, never()).delete(any(Favorite.class));
        verify(favoriteRepository, never()).save(any(Favorite.class));
    }
}
