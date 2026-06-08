package com.qindongyun.portfolio.chat;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class ResumeAssistantService {

    private final StreamingChatModel chatModel;
    private final ChatSessionStore sessionStore;
    private final String systemPrompt;
    private final String profile;
    private final boolean configured;

    public ResumeAssistantService(
            StreamingChatModel chatModel,
            ChatSessionStore sessionStore,
            @Value("classpath:prompts/resume-assistant-system.txt") Resource promptResource,
            @Value("classpath:resume/public-profile.json") Resource profileResource,
            @Value("${zai.api-key:}") String apiKey) throws IOException {
        this.chatModel = chatModel;
        this.sessionStore = sessionStore;
        this.systemPrompt = promptResource.getContentAsString(StandardCharsets.UTF_8);
        this.profile = profileResource.getContentAsString(StandardCharsets.UTF_8);
        this.configured = !apiKey.isBlank();
    }

    public void stream(String sessionId, String userText, ResponseListener listener) {
        if (!configured) {
            listener.onError("AI_NOT_CONFIGURED", "AI 助手尚未配置，请通过邮箱联系本人。");
            return;
        }

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(systemPrompt + "\n\n以下是允许使用的公开简历资料：\n" + profile));
        messages.addAll(sessionStore.history(sessionId));
        messages.add(UserMessage.from(userText));

        ChatRequest request = ChatRequest.builder()
                .messages(messages)
                .build();

        chatModel.chat(request, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                listener.onDelta(partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                String answer = completeResponse.aiMessage().text();
                sessionStore.appendExchange(sessionId, userText, answer);
                listener.onComplete();
            }

            @Override
            public void onError(Throwable error) {
                listener.onError("CHAT_UNAVAILABLE", "AI 助手暂时不可用，请稍后重试。");
            }
        });
    }

    public interface ResponseListener {

        void onDelta(String content);

        void onComplete();

        void onError(String code, String message);
    }
}

