package ru.it_spectrum.ai.playwright.mcp.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Interactive-control inventory for a page, returned when {@code includeControls} is set.
 *
 * <p>Lists inputs, selects, textareas, buttons, checkboxes, radios, and switches with their states
 * (the detail the structural {@code snapshot} omits). Values typed into fields are not returned.
 */
@Schema(description = "Interactive-control inventory for a page, returned when includeControls is requested.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ControlSnapshot(
        @Schema(description = "Maximum number of controls the caller requested.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        Integer maxControls,
        @Schema(description = "Number of forms found on the page.", requiredMode = Schema.RequiredMode.REQUIRED)
        int formCount,
        @Schema(description = "Number of controls found on the page.", requiredMode = Schema.RequiredMode.REQUIRED)
        int controlCount,
        @Schema(description = "Forms found on the page with their metadata.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        List<FormInfo> forms,
        @Schema(description = "Interactive controls (inputs, selects, buttons, etc.) with their states.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        List<FormControlInfo> controls
) {
}