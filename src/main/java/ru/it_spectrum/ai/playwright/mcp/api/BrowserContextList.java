package ru.it_spectrum.ai.playwright.mcp.api;

import java.util.List;

public record BrowserContextList(
        List<BrowserContextInfo> contexts
) {
}
