package ru.it_spectrum.ai.playwright.mcp.playwright;

import com.microsoft.playwright.Browser;

import java.time.Instant;

record ManagedBrowser(
        String browserId,
        String browserName,
        String channel,
        boolean headless,
        Browser browser,
        Instant createdAt
) {
}
