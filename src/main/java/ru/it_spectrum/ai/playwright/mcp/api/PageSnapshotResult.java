package ru.it_spectrum.ai.playwright.mcp.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Unified page inspection result.
 *
 * <p>{@code snapshot} is always present: a compact accessibility YAML view of the page structure with
 * grids/tables collapsed to a one-line summary unless the caller disables collapsing. {@code collapse}
 * explains what was collapsed. {@code controls} and {@code grids} are populated only when the caller
 * opts in via {@code includeControls} / {@code includeGrids}; otherwise they are null, keeping the
 * response small for a quick structural read.
 *
 * <p>{@code matchedCount} reports how many elements the root locator matched. When it is {@code 0} the
 * root locator did not resolve, {@code snapshot} is empty and no waiting was performed: fix the locator
 * rather than retrying. When it is greater than {@code 1} the snapshot describes only the first match.
 */
@Schema(description = "Unified page inspection result containing an accessibility YAML snapshot plus controls and grid inventories when requested.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PageSnapshotResult(
        @Schema(description = "Final URL after any redirects.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String url,
        @Schema(description = "Document title.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String title,
        @Schema(description = "Root locator that was used for the snapshot.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        LocatorSpec locator,
        @Schema(description = "Number of elements the root locator matched. 0 means the locator did not resolve (empty snapshot, no waiting); >1 means the snapshot describes only the first match.", requiredMode = Schema.RequiredMode.REQUIRED)
        int matchedCount,
        @Schema(description = "Compact accessibility YAML view of the page structure.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String snapshot,
        @Schema(description = "Interactive controls inventory; null when includeControls was not requested.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        ControlSnapshot controls,
        @Schema(description = "Structured grid/table data; null when includeGrids was not requested.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        GridSnapshot grids,
        @Schema(description = "Metadata about data-like table/grid nodes collapsed in the snapshot string.", requiredMode = Schema.RequiredMode.REQUIRED)
        SnapshotCollapseInfo collapse
) {
}
