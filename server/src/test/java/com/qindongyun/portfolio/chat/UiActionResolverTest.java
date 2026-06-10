package com.qindongyun.portfolio.chat;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UiActionResolverTest {

    private final UiActionResolver resolver = new UiActionResolver();

    @Test
    void resolvesProjectHighlightFromTaskPlatformQuestion() {
        assertThat(resolver.resolve("司机任务平台做过哪些优化？"))
                .containsExactly(new UiAction(
                        "highlight_project",
                        java.util.Map.of("projectId", "driver-task-platform")));
    }

    @Test
    void resolvesContactActionBeforeGenericQuestions() {
        assertThat(resolver.resolve("如何联系本人？"))
                .containsExactly(new UiAction("open_contact", java.util.Map.of()));
    }

    @Test
    void resolvesProjectHighlightFromSpecificProjectQuestions() {
        assertThat(resolver.resolve("开放平台怎么做接口鉴权？"))
                .containsExactly(new UiAction(
                        "highlight_project",
                        java.util.Map.of("projectId", "openapi-service")));

        assertThat(resolver.resolve("LBS 服务怎么做城市识别？"))
                .containsExactly(new UiAction(
                        "highlight_project",
                        java.util.Map.of("projectId", "lbs-service")));

        assertThat(resolver.resolve("热区服务用了 H3 吗？"))
                .containsExactly(new UiAction(
                        "highlight_project",
                        java.util.Map.of("projectId", "grid-hot-service")));

        assertThat(resolver.resolve("会员权益系统有什么复杂点？"))
                .containsExactly(new UiAction(
                        "highlight_project",
                        java.util.Map.of("projectId", "member-service")));

        assertThat(resolver.resolve("OCR 和 FaceID 是哪个项目？"))
                .containsExactly(new UiAction(
                        "highlight_project",
                        java.util.Map.of("projectId", "third-party-service")));
    }
}
