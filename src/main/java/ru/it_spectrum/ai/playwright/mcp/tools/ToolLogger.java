package ru.it_spectrum.ai.playwright.mcp.tools;

import org.slf4j.Logger;

public final class ToolLogger {

    private ToolLogger() {
    }

    public static void completed(Logger log, String toolName, long startNanos) {
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
        log.info("Tool call completed: {} ({} ms)", toolName, elapsedMs);
    }

    public static void failed(Logger log, String toolName, long startNanos, String error) {
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
        log.warn("Tool call failed: {} ({} ms): {}", toolName, elapsedMs, error);
    }
}
