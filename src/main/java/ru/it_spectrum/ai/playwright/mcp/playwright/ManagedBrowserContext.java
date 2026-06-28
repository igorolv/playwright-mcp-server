package ru.it_spectrum.ai.playwright.mcp.playwright;

import com.microsoft.playwright.BrowserContext;

import java.time.Instant;

record ManagedBrowserContext(
        String contextId,
        String browserId,
        BrowserContext context,
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
