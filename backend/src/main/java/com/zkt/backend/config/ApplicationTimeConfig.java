package com.zkt.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;
import java.util.TimeZone;

@Configuration
public class ApplicationTimeConfig {
    @Bean
    Clock applicationClock(@Value("${app.time-zone:Asia/Shanghai}") String zoneName) {
        ZoneId zone = ZoneId.of(zoneName);
        TimeZone.setDefault(TimeZone.getTimeZone(zone));
        return Clock.system(zone);
    }
}
