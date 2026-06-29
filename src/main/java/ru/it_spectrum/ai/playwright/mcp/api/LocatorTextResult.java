package ru.it_spectrum.ai.playwright.mcp.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Compact text/value read from the first element matching a locator.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LocatorTextResult(
        @Schema(description = "Locator that was read.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        LocatorSpec locator,
        @Schema(description = "Text, input value, selected option text, aria-label, or title from the first matching element.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String text,
        @Schema(description = "True when text was clipped to maxChars.", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean truncated,
        @Schema(description = "Number of elements matching the locator at the time of the read.", requiredMode = Schema.RequiredMode.REQUIRED)
        int count,
        @Schema(description = "Page URL at the time the result was captured.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String url,
        @Schema(description = "Document title at the time the result was captured.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String title
) {
}
