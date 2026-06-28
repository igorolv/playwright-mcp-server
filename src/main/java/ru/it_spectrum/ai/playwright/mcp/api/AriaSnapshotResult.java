package ru.it_spectrum.ai.playwright.mcp.api;

public record AriaSnapshotResult(
        String pageId,
        LocatorSpec locator,
        Integer timeoutMs,
        String snapshot
) {
}
