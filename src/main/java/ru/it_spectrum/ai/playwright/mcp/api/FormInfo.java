package ru.it_spectrum.ai.playwright.mcp.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Metadata for a single HTML form on the page.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FormInfo(
        @Schema(description = "Zero-based index of the form on the page.", requiredMode = Schema.RequiredMode.REQUIRED)
        int index,
        @Schema(description = "HTML id attribute of the form.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String id,
        @Schema(description = "HTML name attribute of the form.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String name,
        @Schema(description = "HTML action attribute (submission URL).", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String action,
        @Schema(description = "HTML method attribute (GET or POST).", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String method,
        @Schema(description = "Accessible label for the form.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String label,
        @Schema(description = "Number of interactive controls inside this form.", requiredMode = Schema.RequiredMode.REQUIRED)
        int controlCount
) {
}