package ru.it_spectrum.ai.playwright.mcp.api;

import java.util.List;

public record LocatorTextResult(
        String pageId,
        LocatorSpec locator,
        int count,
        String textContent,
        String innerText,
        String inputValue,
        Boolean visible,
        Boolean enabled,
        List<String> allTextContents
) {
}
