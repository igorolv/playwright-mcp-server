package ru.it_spectrum.ai.playwright.mcp.api;

/**
 * Result of waiting for an element to reach a DOM/visibility state.
 *
 * <p>{@code found} reports whether the wait condition was satisfied before the timeout.
 * {@code count} is the number of elements matching the locator at the end of the wait.
 */
public record LocatorWaitResult(
        LocatorSpec locator,
        String state,
        boolean found,
        int count,
        String url,
        String title
) {
}
