package com.bhoomi.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.bhoomi.listing.PropertyListingRepository;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final PropertyListingRepository listingRepository;

    public FavoriteService(FavoriteRepository favoriteRepository, PropertyListingRepository listingRepository) {
        this.favoriteRepository = favoriteRepository;
        this.listingRepository = listingRepository;
    }

    @Transactional
    public FavoriteDtos.FavoriteToggleResponse toggle(UUID userId, UUID listingId) {
        if (!listingRepository.existsById(listingId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found");
        }
        Optional<Favorite> existing = favoriteRepository.findByUserIdAndListingId(userId, listingId);
        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            return new FavoriteDtos.FavoriteToggleResponse(listingId, false, favoriteRepository.countByUserId(userId));
        }
        favoriteRepository.save(new Favorite(userId, listingId));
        return new FavoriteDtos.FavoriteToggleResponse(listingId, true, favoriteRepository.countByUserId(userId));
    }

    @Transactional
    public FavoriteDtos.FavoriteToggleResponse remove(UUID userId, UUID listingId) {
        favoriteRepository.findByUserIdAndListingId(userId, listingId)
                .ifPresent(favoriteRepository::delete);
        return new FavoriteDtos.FavoriteToggleResponse(listingId, false, favoriteRepository.countByUserId(userId));
    }

    @Transactional(readOnly = true)
    public List<FavoriteDtos.FavoriteItemResponse> list(UUID userId) {
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(favorite -> new FavoriteDtos.FavoriteItemResponse(favorite.getListingId(), favorite.getCreatedAt()))
                .toList();
    }
}
