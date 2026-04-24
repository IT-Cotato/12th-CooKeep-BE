package com.cookeep.cookeep.config;

import com.cookeep.cookeep.api.dto.response.RankingResponseDto.RecipeRankDto;
import com.cookeep.cookeep.api.dto.response.RankingResponseDto.WateringRankDto;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory,
                                          ObjectMapper redisObjectMapper) {
        JavaType wateringType = redisObjectMapper.getTypeFactory()
            .constructCollectionType(List.class, WateringRankDto.class);
        JavaType recipeType = redisObjectMapper.getTypeFactory()
            .constructCollectionType(List.class, RecipeRankDto.class);

        StringRedisSerializer keySerializer = new StringRedisSerializer();

        RedisCacheConfiguration wateringConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(keySerializer))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                typedSerializer(redisObjectMapper, wateringType)))
            .disableCachingNullValues();

        RedisCacheConfiguration recipeConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(keySerializer))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                typedSerializer(redisObjectMapper, recipeType)))
            .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
            .withCacheConfiguration("wateringRanking", wateringConfig)
            .withCacheConfiguration("recipeRanking", recipeConfig)
            .build();
    }

    private static RedisSerializer<Object> typedSerializer(ObjectMapper redisObjectMapper, JavaType type) {
        return new RedisSerializer<>() {
            @Override
            public byte[] serialize(Object value) throws SerializationException {
                if (value == null) return null;
                try {
                    return redisObjectMapper.writeValueAsBytes(value);
                } catch (Exception e) {
                    throw new SerializationException("Could not serialize: " + e.getMessage(), e);
                }
            }

            @Override
            public Object deserialize(byte[] bytes) throws SerializationException {
                if (bytes == null || bytes.length == 0) return null;
                try {
                    return redisObjectMapper.readValue(bytes, type);
                } catch (Exception e) {
                    throw new SerializationException("Could not deserialize: " + e.getMessage(), e);
                }
            }
        };
    }
}
