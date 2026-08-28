package com.zkt.backend.config;

import com.zkt.backend.room.RoomEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.nio.charset.StandardCharsets;

@Configuration
@ConditionalOnProperty(name = "app.realtime.mode", havingValue = "redis")
public class RealtimeRedisConfig {
    @Bean
    RedisMessageListenerContainer roomEventListener(RedisConnectionFactory factory, RoomEventPublisher publisher,
            @Value("${app.realtime.channel:tourism:room-events}") String channel) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        container.addMessageListener((message, pattern) ->
                publisher.deliverFromBroker(new String(message.getBody(), StandardCharsets.UTF_8)), new ChannelTopic(channel));
        return container;
    }
}
