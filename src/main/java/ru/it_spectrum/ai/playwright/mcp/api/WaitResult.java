package ru.it_spectrum.ai.playwright.mcp.api;

public record WaitResult(
        String pageId,
        String waitedFor,
        String url,
        String title
) {
}
