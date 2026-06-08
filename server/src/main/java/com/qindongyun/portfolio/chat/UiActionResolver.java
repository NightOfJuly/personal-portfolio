package com.qindongyun.portfolio.chat;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class UiActionResolver {

    public List<UiAction> resolve(String message) {
        String normalized = message.toLowerCase();

        if (containsAny(normalized, "联系", "邮箱", "contact", "email")) {
            return List.of(new UiAction("open_contact", Map.of()));
        }
        if (containsAny(normalized, "任务平台", "任务分发", "driver task")) {
            return List.of(new UiAction(
                    "highlight_project",
                    Map.of("projectId", "driver-task-platform")));
        }
        if (containsAny(normalized, "活动平台", "抽奖", "签到", "campaign")) {
            return List.of(new UiAction(
                    "highlight_project",
                    Map.of("projectId", "driver-campaign-platform")));
        }
        if (containsAny(normalized, "开放平台", "lbs", "地图", "位置")) {
            return List.of(new UiAction(
                    "highlight_project",
                    Map.of("projectId", "open-platform-lbs")));
        }
        if (containsAny(normalized, "经历", "公司", "工作", "experience")) {
            return List.of(new UiAction(
                    "scroll_to_section",
                    Map.of("sectionId", "experience")));
        }
        if (containsAny(normalized, "技能", "技术栈", "skill", "java", "spring")) {
            return List.of(new UiAction(
                    "scroll_to_section",
                    Map.of("sectionId", "about")));
        }
        if (containsAny(normalized, "项目", "高并发", "稳定性", "project")) {
            return List.of(new UiAction(
                    "scroll_to_section",
                    Map.of("sectionId", "projects")));
        }

        return List.of();
    }

    private boolean containsAny(String message, String... keywords) {
        for (String keyword : keywords) {
            if (message.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}

