package ru.it_spectrum.ai.playwright.mcp.api;

import java.util.List;

public record FormSnapshotResult(
        String pageId,
        String url,
        String title,
        LocatorSpec locator,
        Integer maxControls,
        int formCount,
        int controlCount,
        List<FormInfo> forms,
        List<FormControlInfo> controls
) {
}
