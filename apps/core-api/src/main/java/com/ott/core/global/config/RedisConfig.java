package com.ott.core.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.List;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType("com.ott.core.modules")
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL
        );

        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jsonSerializer);

        return template;
    }

    // =========================================================================
    // 384차원 AI 임베딩 벡터 전용 RedisTemplate
    // =========================================================================
    @Bean
    public RedisTemplate<String, List<Double>> redisVectorTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, List<Double>> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key는 일반 String
        template.setKeySerializer(new StringRedisSerializer());

        // Value는 List 타입을 위한 Jackson 직렬화기 사용 (복잡한 타입 검증 없이 단순 JSON 배열로 처리)
        Jackson2JsonRedisSerializer<List> serializer = new Jackson2JsonRedisSerializer<>(List.class);
        template.setValueSerializer(serializer);

        return template;
    }
}
