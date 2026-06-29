package ru.it_spectrum.ai.playwright.mcp.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.it_spectrum.ai.playwright.mcp.api.PageScreenshotResult;
import ru.it_spectrum.ai.playwright.mcp.playwright.PlaywrightSessionManager;

@Service
@ConditionalOnProperty(prefix = "playwright-mcp.tools", name = "screenshot", havingValue = "true", matchIfMissing = true)
public class PlaywrightScreenshotTools {

    private static final Logger log = LoggerFactory.getLogger(PlaywrightScreenshotTools.class);

    private final PlaywrightSessionManager sessions;

    public PlaywrightScreenshotTools(PlaywrightSessionManager sessions) {
        this.sessions = sessions;
    }

    @McpTool(
            description = "Capture a PNG screenshot of the current page and save it on the server. Use this for documentation, bug reports, and step-by-step evidence. The image bytes are not returned in the MCP response; the result contains the saved file path.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = false)
    )
    public PageScreenshotResult pageScreenshot(
            @McpToolParam(description = "When true, capture the full scrollable page. Default false captures the current viewport.", required = false) Boolean fullPage,
            @McpToolParam(description = "Short label used in the generated PNG filename. It is sanitized; do not pass a path.", required = false) String name,
            @McpToolParam(description = "Screenshot timeout in milliseconds", required = false) Integer timeoutMs
    ) {
        log.info("Tool call: pageScreenshot (fullPage={}, name={})", fullPage, name);
        long start = System.nanoTime();
        try {
            PageScreenshotResult result = sessions.pageScreenshot(fullPage, name, timeoutMs);
            ToolLogger.completed(log, "pageScreenshot", start);
            return result;
        } catch (RuntimeException e) {
            ToolLogger.failed(log, "pageScreenshot", start, e.getMessage());
            throw e;
        }
    }
}
