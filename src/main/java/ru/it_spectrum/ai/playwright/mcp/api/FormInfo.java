package ru.it_spectrum.ai.playwright.mcp.api;

public record FormInfo(
        int index,
        String id,
        String name,
        String action,
        String method,
        String label,
        int controlCount
) {
}
