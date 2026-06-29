package ru.it_spectrum.ai.playwright.mcp.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Structured rows and columns of the tables and ARIA grids/treegrids on a page, returned when
 * {@code includeGrids} is set.
 *
 * <p>Virtualized grids (e.g. ag-Grid) keep only the on-screen rows in the DOM, so a grid's
 * {@code renderedRowCount} can be smaller than its real total.
 */
@Schema(description = "Structured rows and columns of tables/ARIA grids on a page, returned when includeGrids is requested.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GridSnapshot(
        @Schema(description = "Maximum number of rows the caller requested.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        Integer maxRows,
        @Schema(description = "Number of grids/tables found on the page.", requiredMode = Schema.RequiredMode.REQUIRED)
        int gridCount,
        @Schema(description = "Grids/tables found on the page with their columns and rows.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        List<GridInfo> grids
) {
}