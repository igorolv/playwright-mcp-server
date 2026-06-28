package ru.it_spectrum.ai.playwright.mcp.api;

public record LocatorCountResult(
        String pageId,
        LocatorSpec locator,
        int count
) {
}
