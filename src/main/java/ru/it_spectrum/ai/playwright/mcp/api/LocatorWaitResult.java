package ru.it_spectrum.ai.playwright.mcp.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Result of waiting for an element to reach a DOM/visibility state.
 *
 * <p>{@code found} reports whether the wait condition was satisfied before the timeout.
 * {@code count} is the number of elements matching the locator at the end of the wait.
 */
@Schema(description = "Result of waiting for an element to reach a DOM/visibility state.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LocatorWaitResult(
        @Schema(description = "Locator that was waited on.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        LocatorSpec locator,
        @Schema(description = "Target state that was waited for.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String state,
        @Schema(description = "True when the wait condition was satisfied before the timeout.", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean found,
        @Schema(description = "Number of elements matching the locator at the end of the wait.", requiredMode = Schema.RequiredMode.REQUIRED)
        int count,
        @Schema(description = "Page URL at the time the result was captured.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String url,
        @Schema(description = "Document title at the time the result was captured.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String title
) {
}