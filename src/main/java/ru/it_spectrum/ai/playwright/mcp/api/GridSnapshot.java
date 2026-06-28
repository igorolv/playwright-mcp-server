package ru.it_spectrum.ai.playwright.mcp.api;

import java.util.List;

/**
 * Structured rows and columns of the tables and ARIA grids/treegrids on a page, returned when
 * {@code includeGrids} is set.
 *
 * <p>Virtualized grids (e.g. ag-Grid) keep only the on-screen rows in the DOM, so a grid's
 * {@code renderedRowCount} can be smaller than its real total.
 */
public record GridSnapshot(
        Integer maxRows,
        int gridCount,
        List<GridInfo> grids
) {
}
