package ru.it_spectrum.ai.playwright.mcp.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Structured view of a single data grid or table.
 *
 * <p>{@code role} is the ARIA role (grid, treegrid) or html tag (table) of the container.
 * Rows reflect only the DOM-rendered rows: virtualized grids (e.g. ag-Grid) keep just the
 * visible window in the DOM, so {@code renderedRowCount} can be smaller than the real total.
 */
@Schema(description = "Structured view of a single data grid or table with column headers and rendered rows.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GridInfo(
        @Schema(description = "Zero-based index of the grid on the page.", requiredMode = Schema.RequiredMode.REQUIRED)
        int index,
        @Schema(description = "ARIA role (grid, treegrid) or HTML tag (table) of the container.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String role,
        @Schema(description = "Number of columns in the grid.", requiredMode = Schema.RequiredMode.REQUIRED)
        int columnCount,
        @Schema(description = "Number of rows rendered in the DOM.", requiredMode = Schema.RequiredMode.REQUIRED)
        int renderedRowCount,
        @Schema(description = "True when the rendered rows were truncated by the maxRows limit.", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean rowsTruncated,
        @Schema(description = "Column header labels.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        List<String> columns,
        @Schema(description = "Row data, each row being a list of cell text values.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        List<List<String>> rows
) {
}