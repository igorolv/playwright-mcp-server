package ru.it_spectrum.ai.playwright.mcp.api;

import java.util.List;

public record BrowserList(
        List<BrowserInfo> browsers
) {
}
