package com.zkt.backend.room;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RoomEventPublisherTest {
    private final RoomEventPublisher publisher = new RoomEventPublisher(
            new ObjectMapper().registerModule(new JavaTimeModule()), mock(StringRedisTemplate.class), "local", "test");

    @AfterEach void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.clearSynchronization();
    }

    @Test void eventsInsideATransactionAreSentOnlyAfterCommit() throws Exception {
        WebSocketSession session = session("s1", 1L);
        publisher.register(session);
        publisher.subscribe(7L, session);

        TransactionSynchronizationManager.initSynchronization();
        publisher.publish(7L, "CHAT_CREATED", Map.of("id", 1));
        verify(session, never()).sendMessage(any());

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(session).sendMessage(any(TextMessage.class));
    }

    @Test void rolledBackTransactionsNeverSendEvents() throws Exception {
        WebSocketSession session = session("s1", 1L);
        publisher.register(session);
        publisher.subscribe(7L, session);

        TransactionSynchronizationManager.initSynchronization();
        publisher.publish(7L, "CHAT_CREATED", Map.of("id", 1));
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        verify(session, never()).sendMessage(any());
    }

    @Test void disconnectClosesOnlyTheTargetUsersSessionsForThatActivity() throws Exception {
        WebSocketSession leaving = session("s1", 1L), otherActivity = session("s2", 1L), stays = session("s3", 2L);
        for (WebSocketSession s : new WebSocketSession[]{leaving, otherActivity, stays}) publisher.register(s);
        publisher.subscribe(7L, leaving);
        publisher.subscribe(8L, otherActivity);
        publisher.subscribe(7L, stays);

        publisher.disconnect(1L, 7L);

        verify(leaving).close(RoomEventPublisher.ACCESS_REVOKED);
        verify(otherActivity, never()).close(any(CloseStatus.class));
        verify(stays, never()).close(any(CloseStatus.class));
        publisher.publish(7L, "CHAT_CREATED", Map.of("id", 1));
        verify(leaving, never()).sendMessage(any());
        verify(stays).sendMessage(any(TextMessage.class));
    }

    @Test void disconnectWithoutActivityClosesEveryConnectionOfTheUser() throws Exception {
        WebSocketSession subscribed = session("s1", 1L), idle = session("s2", 1L);
        publisher.register(subscribed);
        publisher.register(idle);
        publisher.subscribe(7L, subscribed);

        publisher.disconnect(1L, null);

        verify(subscribed).close(RoomEventPublisher.ACCESS_REVOKED);
        verify(idle).close(RoomEventPublisher.ACCESS_REVOKED);
    }

    private static WebSocketSession session(String id, Long userId) {
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attributes = new ConcurrentHashMap<>(new HashMap<>(Map.of("userId", userId)));
        when(session.getId()).thenReturn(id);
        when(session.getAttributes()).thenReturn(attributes);
        when(session.isOpen()).thenReturn(true);
        return session;
    }
}
