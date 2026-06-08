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
}

