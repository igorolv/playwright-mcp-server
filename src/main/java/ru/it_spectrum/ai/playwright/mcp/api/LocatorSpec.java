package ru.it_spectrum.ai.playwright.mcp.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Element locator description for MCP callers.
 *
 * <p>Supported kinds: css, xpath, role, text, label, placeholder, altText, title, testId.
 * For role locators, set role and name/exact when matching by accessible name. For
 * CSS/XPath/text-like locators, set value. hasText/nth/visible refine the resulting locator.
 */
@Schema(description = "Element search descriptor. Choose one strategy: role with role/name, or css/xpath/text/label/placeholder/altText/title/testId with value. Add exact, hasText, nth, visible, or frameSelector only to narrow matches.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LocatorSpec(
        @Schema(description = "Search strategy. Prefer role, text, label, placeholder, altText, title, or testId; use css/xpath as fallback.", allowableValues = {"css", "xpath", "role", "text", "label", "placeholder", "altText", "title", "testId"}, requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String kind,
        @Schema(description = "Selector, XPath, visible text, label text, placeholder text, alt/title text, or test id.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String value,
        @Schema(description = "Element role for kind=role, e.g. button, link, textbox, checkbox, heading.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String role,
        @Schema(description = "Accessible name for kind=role, usually the visible label from the snapshot.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String name,
        @Schema(description = "Match full name/text instead of substring.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        Boolean exact,
        @Schema(description = "Keep matches whose text contains this value.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String hasText,
        @Schema(description = "Pick the zero-based match when multiple elements match.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        Integer nth,
        @Schema(description = "Keep visible or hidden matches.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        Boolean visible,
        @Schema(description = "CSS selector of an iframe to search inside.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String frameSelector
) {
    public static LocatorSpec css(String selector) {
        return new LocatorSpec("css", selector, null, null, null, null, null, null, null);
    }
}
