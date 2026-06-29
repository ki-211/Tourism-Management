package com.zkt.backend.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // 注册 Java 8 时间模块，支持 LocalDateTime 等类型
        mapper.registerModule(new JavaTimeModule());
        // 日期时间序列化为字符串而非时间戳
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 忽略 JSON 中有但 Java 对象中没有的字段
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }
}
