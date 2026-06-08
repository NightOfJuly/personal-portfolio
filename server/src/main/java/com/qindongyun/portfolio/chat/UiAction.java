package com.qindongyun.portfolio.chat;

import java.util.Map;

public record UiAction(String type, Map<String, String> payload) {
}

