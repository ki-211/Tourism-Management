package com.zkt.backend.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AuthenticatedUserCacheTest {
    private final UserRepository users = mock(UserRepository.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);

    @AfterEach void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.clearSynchronization();
    }

    @Test void loadsOnceUntilInvalidatedAfterCommit() {
        AuthenticatedUserCache cache = new AuthenticatedUserCache(users, redis, "local");
        when(users.findById(1L)).thenReturn(Optional.of(user(1L, 0L)));
        assertThat(cache.find(1L)).get().extracting(AuthenticatedUserCache.Snapshot::tokenVersion).isEqualTo(0L);
        assertThat(cache.find(1L)).isPresent();
        verify(users, times(1)).findById(1L);

        when(users.findById(1L)).thenReturn(Optional.of(user(1L, 1L)));
        TransactionSynchronizationManager.initSynchronization();
        cache.invalidate(1L);
        assertThat(cache.find(1L).orElseThrow().tokenVersion()).as("not evicted before commit").isEqualTo(0L);
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        assertThat(cache.find(1L).orElseThrow().tokenVersion()).isEqualTo(1L);
    }

    @Test void missingUsersAreNotCached() {
        AuthenticatedUserCache cache = new AuthenticatedUserCache(users, redis, "local");
        when(users.findById(2L)).thenReturn(Optional.empty());
        assertThat(cache.find(2L)).isEmpty();
        assertThat(cache.find(2L)).isEmpty();
        verify(users, times(2)).findById(2L);
    }

    @Test void distributedInvalidationIsBroadcastAndAppliedFromBroker() {
        AuthenticatedUserCache cache = new AuthenticatedUserCache(users, redis, "redis");
        when(users.findById(3L)).thenReturn(Optional.of(user(3L, 0L)));
        cache.find(3L);
        cache.invalidate(3L);
        verify(redis).convertAndSend(AuthenticatedUserCache.INVALIDATION_CHANNEL, "3");

        cache.find(3L);
        cache.evictFromBroker("3");
        cache.find(3L);
        verify(users, times(3)).findById(3L);
    }

    private static User user(Long id, long tokenVersion) {
        User user = new User();
        user.setId(id); user.setUsername("u" + id); user.setRole("USER"); user.setTokenVersion(tokenVersion);
        return user;
    }
}
