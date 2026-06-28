package ru.it_spectrum.ai.playwright.mcp.api;

public record PageNavigationResult(
        String pageId,
        String url,
        String title,
        Integer status,
        String statusText,
        Boolean ok
) {
}
