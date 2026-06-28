package ru.it_spectrum.ai.playwright.mcp.api;

import java.util.List;

/**
 * Structured view of a single data grid or table.
 *
 * <p>{@code role} is the ARIA role (grid, treegrid) or html tag (table) of the container.
 * Rows reflect only the DOM-rendered rows: virtualized grids (e.g. ag-Grid) keep just the
 * visible window in the DOM, so {@code renderedRowCount} can be smaller than the real total.
 */
public record GridInfo(
        int index,
        String role,
        int columnCount,
        int renderedRowCount,
        boolean rowsTruncated,
        List<String> columns,
        List<List<String>> rows
) {
}
