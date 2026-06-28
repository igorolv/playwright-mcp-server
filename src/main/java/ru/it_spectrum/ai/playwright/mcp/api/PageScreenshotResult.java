package ru.it_spectrum.ai.playwright.mcp.api;

public record PageScreenshotResult(
        String url,
        String title,
        String path,
        String relativePath,
        boolean fullPage,
        long sizeBytes
) {
}
