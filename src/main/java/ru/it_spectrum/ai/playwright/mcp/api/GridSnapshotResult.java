package ru.it_spectrum.ai.playwright.mcp.api;

import java.util.List;

public record GridSnapshotResult(
        String pageId,
        String url,
        String title,
        LocatorSpec locator,
        Integer maxRows,
        int gridCount,
        List<GridInfo> grids
) {
}
