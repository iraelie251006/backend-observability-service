package tech.iraelie.practice.config;

import io.lettuce.core.api.StatefulConnection;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.datatype.jsr310.JavaTimeModule;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {
    @Bean
    public CacheManager cacheManager(LettuceConnectionFactory connectionFactory) {
        JsonMapper jsonMapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .configure(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .build();

        // GenericJacksonJsonRedisSerializer adds @class field which leads to tight coupling between redis and application
        // Jackson2JsonRedisSerializer is deprecated for version 4.0+
        JacksonJsonRedisSerializer<Object> jsonSerializer = new JacksonJsonRedisSerializer<> (jsonMapper, Object.class);


        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(
                        RedisSerializationContext
                                .SerializationPair
                                .fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext
                                .SerializationPair
                                .fromSerializer(jsonSerializer)
                )
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }

    @Bean
    public LettuceConnectionFactory redisConnectionFactory () {
        RedisStandaloneConfiguration serverConfig =
                new RedisStandaloneConfiguration("localhost", 6379);

        GenericObjectPoolConfig<StatefulConnection<?, ?>> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMinIdle(2);
        poolConfig.setMaxIdle(8);
        poolConfig.setMaxTotal(16);
        poolConfig.setMaxWait(Duration.ofMillis(500));

        LettucePoolingClientConfiguration clientConfig =
                LettucePoolingClientConfiguration.builder()
                        .poolConfig(poolConfig)
                        .commandTimeout(Duration.ofSeconds(2))
                        .build();

        return new LettuceConnectionFactory(serverConfig, clientConfig);
    }
}
