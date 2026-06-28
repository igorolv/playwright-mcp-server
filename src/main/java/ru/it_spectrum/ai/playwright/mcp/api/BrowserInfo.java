package ru.it_spectrum.ai.playwright.mcp.api;

import java.time.Instant;

public record BrowserInfo(
        String browserId,
        String browserName,
        String channel,
        boolean headless,
        boolean connected,
        Instant createdAt
) {
}
