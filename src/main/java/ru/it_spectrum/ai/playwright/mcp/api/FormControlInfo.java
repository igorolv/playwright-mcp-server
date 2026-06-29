package ru.it_spectrum.ai.playwright.mcp.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "An interactive form control (input, select, textarea, button, checkbox, radio, switch) with its current state.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FormControlInfo(
        @Schema(description = "Zero-based index of the control on the page.", requiredMode = Schema.RequiredMode.REQUIRED)
        int index,
        @Schema(description = "HTML tag name of the control element.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String tagName,
        @Schema(description = "HTML type attribute (text, password, checkbox, etc.).", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String type,
        @Schema(description = "ARIA role computed for the element.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String role,
        @Schema(description = "Computed accessible name.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String accessibleName,
        @Schema(description = "Text of the associated label element.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String label,
        @Schema(description = "Placeholder text, if any.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String placeholder,
        @Schema(description = "HTML name attribute.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String name,
        @Schema(description = "HTML id attribute.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String id,
        @Schema(description = "Visible text content of the control.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String text,
        @Schema(description = "Checked state for checkboxes and radios.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        Boolean checked,
        @Schema(description = "Whether the control is currently visible.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        Boolean visible,
        @Schema(description = "Whether the control is enabled (not disabled).", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        Boolean enabled,
        @Schema(description = "HTML form id or name this control belongs to.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String form,
        @Schema(description = "Hover tooltip text of the control, captured on demand.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String tooltip
) {
}