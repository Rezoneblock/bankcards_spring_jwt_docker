package com.gordeev.bankcards.dto.api;

import java.util.Map;

public record ApiError(String message, String code, Map<String, String> details) {
}
