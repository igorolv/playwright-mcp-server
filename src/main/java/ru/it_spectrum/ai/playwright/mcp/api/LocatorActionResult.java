package ru.it_spectrum.ai.playwright.mcp.api;

public record LocatorActionResult(
        String pageId,
        String action,
        LocatorSpec locator,
        String url,
        String title
) {
}
