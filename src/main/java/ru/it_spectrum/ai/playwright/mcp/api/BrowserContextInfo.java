package ru.it_spectrum.ai.playwright.mcp.api;

import java.time.Instant;

public record BrowserContextInfo(
        String contextId,
        String browserId,
        String baseUrl,
        int viewportWidth,
        int viewportHeight,
        String userAgent,
        String locale,
        String timezoneId,
        boolean ignoreHttpsErrors,
        Instant createdAt
) {
}
