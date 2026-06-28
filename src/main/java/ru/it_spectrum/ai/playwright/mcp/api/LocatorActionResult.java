package ru.it_spectrum.ai.playwright.mcp.api;

public record LocatorActionResult(
        String action,
        LocatorSpec locator,
        String url,
        String title
) {
}
