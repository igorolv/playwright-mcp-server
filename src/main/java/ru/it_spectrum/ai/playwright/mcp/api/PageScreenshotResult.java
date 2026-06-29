package ru.it_spectrum.ai.playwright.mcp.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of capturing a page screenshot, saved to the server's data directory.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PageScreenshotResult(
        @Schema(description = "Page URL at the time of the screenshot.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String url,
        @Schema(description = "Document title at the time of the screenshot.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String title,
        @Schema(description = "Absolute filesystem path to the saved PNG file.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String path,
        @Schema(description = "Path relative to the screenshots data directory.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String relativePath,
        @Schema(description = "True when the screenshot captured the full scrollable page.", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean fullPage,
        @Schema(description = "Size of the saved PNG file in bytes.", requiredMode = Schema.RequiredMode.REQUIRED)
        long sizeBytes
) {
}