package ru.it_spectrum.ai.playwright.mcp.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "One data-like table/grid node collapsed inside a pageSnapshot response.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SnapshotCollapsedNode(
        @Schema(description = "Zero-based index of this collapsed node among collapsed nodes in traversal order.", requiredMode = Schema.RequiredMode.REQUIRED)
        int index,
        @Schema(description = "Collapsed node kind: table, grid, or treegrid.", requiredMode = Schema.RequiredMode.REQUIRED)
        String kind,
        @Schema(description = "Accessible node name when Playwright reported one.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String name,
        @Schema(description = "Why the node was collapsed: gridRole, treegridRole, columnHeaders, largeRowCount, or nestedBodyRows.", requiredMode = Schema.RequiredMode.REQUIRED)
        String reason,
        @Schema(description = "Number of direct row nodes seen under the collapsed node.", requiredMode = Schema.RequiredMode.REQUIRED)
        int directRows,
        @Schema(description = "Total number of column headers detected under the collapsed node.", requiredMode = Schema.RequiredMode.REQUIRED)
        int columnCount,
        @Schema(description = "Column names included for orientation, capped to keep the response compact.", requiredMode = Schema.RequiredMode.REQUIRED)
        List<String> columns,
        @Schema(description = "True when the columns list was capped.", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean columnsTruncated
) {
}
