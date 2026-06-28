package ru.it_spectrum.ai.playwright.mcp.api;

/**
 * Unified page inspection result.
 *
 * <p>{@code snapshot} is always present: a compact accessibility YAML view of the page structure with
 * grids/tables collapsed to a one-line summary. {@code controls} and {@code grids} are populated only
 * when the caller opts in via {@code includeControls} / {@code includeGrids}; otherwise they are null,
 * keeping the response small for a quick structural read.
 */
public record PageSnapshotResult(
        String pageId,
        String url,
        String title,
        LocatorSpec locator,
        String snapshot,
        ControlSnapshot controls,
        GridSnapshot grids
) {
}
