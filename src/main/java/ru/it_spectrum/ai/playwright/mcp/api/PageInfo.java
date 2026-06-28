package ru.it_spectrum.ai.playwright.mcp.api;

import java.time.Instant;

public record PageInfo(
        String pageId,
        String contextId,
        String url,
        String title,
        boolean closed,
        Instant createdAt
) {
}
