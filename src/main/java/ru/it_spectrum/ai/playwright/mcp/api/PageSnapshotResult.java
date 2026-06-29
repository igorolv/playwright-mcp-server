package ru.it_spectrum.ai.playwright.mcp.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Unified page inspection result.
 *
 * <p>{@code snapshot} is always present: a compact accessibility YAML view of the page structure with
 * grids/tables collapsed to a one-line summary. {@code controls} and {@code grids} are populated only
 * when the caller opts in via {@code includeControls} / {@code includeGrids}; otherwise they are null,
 * keeping the response small for a quick structural read.
 */
@Schema(description = "Unified page inspection result containing an accessibility YAML snapshot plus optional controls and grid inventories.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PageSnapshotResult(
        @Schema(description = "Final URL after any redirects.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String url,
        @Schema(description = "Document title.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String title,
        @Schema(description = "Root locator that was used for the snapshot.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        LocatorSpec locator,
        @Schema(description = "Compact accessibility YAML view of the page structure.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String snapshot,
        @Schema(description = "Interactive controls inventory; null when includeControls was not requested.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        ControlSnapshot controls,
        @Schema(description = "Structured grid/table data; null when includeGrids was not requested.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        GridSnapshot grids
) {
}