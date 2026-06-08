package com.qindongyun.portfolio.config;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiModelConfig {

    @Bean
    StreamingChatModel streamingChatModel(
            @Value("${zai.base-url}") String baseUrl,
            @Value("${zai.api-key:}") String apiKey,
            @Value("${zai.model}") String modelName) {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey.isBlank() ? "missing-api-key" : apiKey)
                .modelName(modelName)
                .temperature(0.3)
                .timeout(Duration.ofSeconds(45))
                .build();
    }
}

