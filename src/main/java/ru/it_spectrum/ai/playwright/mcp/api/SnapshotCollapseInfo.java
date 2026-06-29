package ru.it_spectrum.ai.playwright.mcp.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Metadata describing whether and how pageSnapshot collapsed data-like table/grid nodes.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SnapshotCollapseInfo(
        @Schema(description = "Whether snapshot collapsing was enabled for this pageSnapshot call.", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean enabled,
        @Schema(description = "Total number of nodes collapsed in the snapshot string.", requiredMode = Schema.RequiredMode.REQUIRED)
        int collapsedCount,
        @Schema(description = "True when the nodes list was capped and not all collapsed nodes are described.", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean nodesTruncated,
        @Schema(description = "Collapsed nodes described in traversal order, capped to keep the response compact.", requiredMode = Schema.RequiredMode.REQUIRED)
        List<SnapshotCollapsedNode> nodes
) {
}
