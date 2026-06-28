package ru.it_spectrum.ai.playwright.mcp.playwright;

import com.microsoft.playwright.Page;

import java.time.Instant;

record ManagedPage(
        String pageId,
        String contextId,
        Page page,
        Instant createdAt
) {
}
