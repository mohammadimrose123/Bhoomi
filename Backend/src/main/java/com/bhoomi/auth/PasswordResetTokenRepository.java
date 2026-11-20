package com.bhoomi.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select token from PasswordResetToken token
            where token.tokenHash = :tokenHash
              and token.usedAt is null
              and token.expiresAt > :now
            """)
    Optional<PasswordResetToken> findUsableToken(
            @Param("tokenHash") String tokenHash, @Param("now") Instant now);

    @Modifying
    @Query("""
            update PasswordResetToken token
               set token.usedAt = :usedAt
             where token.userId = :userId and token.usedAt is null
            """)
    int invalidateUnusedTokens(@Param("userId") UUID userId, @Param("usedAt") Instant usedAt);
}
