package com.bhoomi.user;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping("/buyer/favorites")
    public List<FavoriteDtos.FavoriteItemResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return favoriteService.list(UUID.fromString(jwt.getSubject()));
    }

    @PostMapping("/buyer/favorites/{listingId}")
    @ResponseStatus(HttpStatus.CREATED)
    public FavoriteDtos.FavoriteToggleResponse create(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID listingId) {
        return favoriteService.toggle(UUID.fromString(jwt.getSubject()), listingId);
    }

    @DeleteMapping("/buyer/favorites/{listingId}")
    public FavoriteDtos.FavoriteToggleResponse remove(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID listingId) {
        return favoriteService.remove(UUID.fromString(jwt.getSubject()), listingId);
    }
}
