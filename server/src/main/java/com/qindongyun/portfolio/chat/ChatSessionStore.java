package com.qindongyun.portfolio.chat;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class ChatSessionStore {

    private static final int MAX_MESSAGES = 20;
    private static final Duration SESSION_TTL = Duration.ofMinutes(30);

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final Clock clock;

    public ChatSessionStore() {
        this(Clock.systemUTC());
    }

    ChatSessionStore(Clock clock) {
        this.clock = clock;
    }

    public synchronized List<ChatMessage> history(String sessionId) {
        removeExpired();
        Session session = sessions.get(sessionId);
        return session == null ? List.of() : List.copyOf(session.messages());
    }

    public synchronized void appendExchange(String sessionId, String userText, String aiText) {
        removeExpired();
        Session session = sessions.computeIfAbsent(
                sessionId,
                ignored -> new Session(new ArrayList<>(), clock.instant()));
        session.messages().add(UserMessage.from(userText));
        session.messages().add(AiMessage.from(aiText));

        while (session.messages().size() > MAX_MESSAGES) {
            session.messages().remove(0);
        }
        sessions.put(sessionId, new Session(session.messages(), clock.instant()));
    }

    private void removeExpired() {
        Instant cutoff = clock.instant().minus(SESSION_TTL);
        sessions.entrySet().removeIf(entry -> entry.getValue().lastAccess().isBefore(cutoff));
    }

    private record Session(List<ChatMessage> messages, Instant lastAccess) {
    }
}

