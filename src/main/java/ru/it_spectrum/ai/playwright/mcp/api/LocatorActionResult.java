package ru.it_spectrum.ai.playwright.mcp.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of a locator action (click, fill, press, check).")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LocatorActionResult(
        @Schema(description = "Action that was performed.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String action,
        @Schema(description = "Locator that was acted on.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        LocatorSpec locator,
        @Schema(description = "Page URL after the action.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String url,
        @Schema(description = "Document title after the action.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String title
) {
}