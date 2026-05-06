package tech.iraelie.practice.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tech.iraelie.practice.auth.repository.RefreshTokenRepository;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class TokenCleanUpJob {

    private final RefreshTokenRepository refreshTokenRepository;

    // Runs every day at 2am
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanExpiredTokens() {
        refreshTokenRepository.deleteAllByExpiresAtBefore(Instant.now());
    }
}