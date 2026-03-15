package com.ott.core.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.type.CollectionType;
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
    public RedisTemplate<String, List<Double>> redisVectorTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, List<Double>> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // Key는 단순 문자열 직렬화
        template.setKeySerializer(new StringRedisSerializer());

        // 1. ObjectMapper 생성
        ObjectMapper mapper = new ObjectMapper();

        // 2. [주의] Jackson의 CollectionType을 사용하여 List<Double> 타입 정의
        CollectionType listType = mapper.getTypeFactory().constructCollectionType(List.class, Double.class);

        // 3. Spring Boot 3.x 방식: 생성자에서 ObjectMapper와 Type을 한 번에 주입 (setObjectMapper 안 씀!)
        Jackson2JsonRedisSerializer<List<Double>> serializer = new Jackson2JsonRedisSerializer<>(mapper, listType);

        // Value 직렬화 설정
        template.setValueSerializer(serializer);

        return template;
    }
}
