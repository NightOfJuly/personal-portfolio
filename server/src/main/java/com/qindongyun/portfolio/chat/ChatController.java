package com.qindongyun.portfolio.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ResumeAssistantService assistantService;
    private final UiActionResolver actionResolver;
    private final RequestRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    public ChatController(
            ResumeAssistantService assistantService,
            UiActionResolver actionResolver,
            RequestRateLimiter rateLimiter,
            ObjectMapper objectMapper) {
        this.assistantService = assistantService;
        this.actionResolver = actionResolver;
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
    }

    @PostMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public SseEmitter stream(
            @Valid @RequestBody ChatRequestDto request,
            HttpServletRequest servletRequest) {
        String rateLimitKey = clientIp(servletRequest) + ":" + request.sessionId();
        if (!rateLimiter.tryAcquire(rateLimitKey)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "请求过于频繁，请稍后再试。");
        }

        SseEmitter emitter = new SseEmitter(60_000L);
        AtomicBoolean closed = new AtomicBoolean(false);

        assistantService.stream(request.sessionId(), request.message(), new ResumeAssistantService.ResponseListener() {
            @Override
            public void onDelta(String content) {
                send(emitter, closed, "message_delta", Map.of("content", content));
            }

            @Override
            public void onComplete() {
                actionResolver.resolve(request.message())
                        .forEach(action -> send(emitter, closed, "ui_action", action));
                send(emitter, closed, "done", Map.of());
                complete(emitter, closed);
            }

            @Override
            public void onError(String code, String message) {
                send(emitter, closed, "error", Map.of("code", code, "message", message));
                complete(emitter, closed);
            }
        });

        emitter.onTimeout(() -> complete(emitter, closed));
        emitter.onError(ignored -> closed.set(true));
        return emitter;
    }

    private void send(SseEmitter emitter, AtomicBoolean closed, String eventName, Object data) {
        if (closed.get()) {
            return;
        }
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(objectMapper.writeValueAsString(data)));
        } catch (IOException exception) {
            closed.set(true);
            emitter.completeWithError(exception);
        }
    }

    private void complete(SseEmitter emitter, AtomicBoolean closed) {
        if (closed.compareAndSet(false, true)) {
            emitter.complete();
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
