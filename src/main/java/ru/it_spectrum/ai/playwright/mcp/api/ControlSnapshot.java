package ru.it_spectrum.ai.playwright.mcp.api;

import java.util.List;

/**
 * Interactive-control inventory for a page, returned when {@code includeControls} is set.
 *
 * <p>Lists inputs, selects, textareas, buttons, checkboxes, radios, and switches with their states
 * (the detail the structural {@code snapshot} omits). Values typed into fields are not returned.
 */
public record ControlSnapshot(
        Integer maxControls,
        int formCount,
        int controlCount,
        List<FormInfo> forms,
        List<FormControlInfo> controls
) {
}
