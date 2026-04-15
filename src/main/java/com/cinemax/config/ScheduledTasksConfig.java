package com.cinemax.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.time.LocalDateTime;

// ─────────────────────────────────────────────
// Scheduled Tasks — seat lock cleanup
// ─────────────────────────────────────────────
@org.springframework.stereotype.Component
@org.springframework.transaction.annotation.Transactional
public class ScheduledTasksConfig {

    private static final org.slf4j.Logger log =
        org.slf4j.LoggerFactory.getLogger(ScheduledTasksConfig.class);

    private final com.cinemax.repository.ShowSeatSchedulerRepository showSeatSchedulerRepository;

    ScheduledTasksConfig(
            com.cinemax.repository.ShowSeatSchedulerRepository showSeatSchedulerRepository) {
        this.showSeatSchedulerRepository = showSeatSchedulerRepository;
    }

    /**
     * Release LOCKED seats whose payment window (10 min) has expired.
     * Runs every 2 minutes.
     */
    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 120_000)
    public void releaseExpiredSeatLocks() {
        java.time.LocalDateTime expiryTime =
            java.time.LocalDateTime.now().minusMinutes(10);
        int released = showSeatSchedulerRepository.releaseExpiredLocks(expiryTime);
        if (released > 0) {
            log.info("Released {} expired seat locks", released);
        }
    }
}
