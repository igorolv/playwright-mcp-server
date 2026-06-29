package ru.it_spectrum.ai.playwright.mcp.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of navigating the browser page to a URL.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PageNavigationResult(
        @Schema(description = "Final URL after any redirects.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String url,
        @Schema(description = "Document title after navigation.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String title,
        @Schema(description = "HTTP response status code, null when unavailable (e.g. navigation failed).", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        Integer status,
        @Schema(description = "HTTP response status text, null when unavailable.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String statusText,
        @Schema(description = "True when the HTTP status is in the 2xx range.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        Boolean ok
) {
}