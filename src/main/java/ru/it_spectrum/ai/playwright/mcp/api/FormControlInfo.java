package ru.it_spectrum.ai.playwright.mcp.api;

public record FormControlInfo(
        int index,
        String tagName,
        String type,
        String role,
        String accessibleName,
        String label,
        String placeholder,
        String name,
        String id,
        String text,
        Boolean checked,
        Boolean visible,
        Boolean enabled,
        String form,
        String tooltip
) {
}
