package ru.it_spectrum.ai.playwright.mcp.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.it_spectrum.ai.playwright.mcp.api.AriaSnapshotResult;
import ru.it_spectrum.ai.playwright.mcp.api.LocatorSpec;
import ru.it_spectrum.ai.playwright.mcp.api.PageNavigationResult;
import ru.it_spectrum.ai.playwright.mcp.api.WaitResult;
import ru.it_spectrum.ai.playwright.mcp.playwright.PlaywrightSessionManager;

@Service
@ConditionalOnProperty(prefix = "playwright-mcp.tools", name = "page", havingValue = "true", matchIfMissing = true)
public class PlaywrightPageTools {

    private static final Logger log = LoggerFactory.getLogger(PlaywrightPageTools.class);

    private final PlaywrightSessionManager sessions;

    public PlaywrightPageTools(PlaywrightSessionManager sessions) {
        this.sessions = sessions;
    }

    @McpTool(
            description = "Open a URL in a browser tab. Use this as the first tool for live site work. Omitted ids use the default browser, isolated context, and tab; they are created automatically.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = false)
    )
    public PageNavigationResult pageNavigate(
            @McpToolParam(description = "Logical browser tab id", required = false) String pageId,
            @McpToolParam(description = "Absolute URL or URL relative to the context baseUrl") String url,
            @McpToolParam(description = "Navigation wait condition: load, domcontentloaded, networkidle, or commit. Default domcontentloaded.", required = false) String waitUntil,
            @McpToolParam(description = "Navigation timeout in milliseconds", required = false) Integer timeoutMs
    ) {
        log.info("Tool call: pageNavigate (pageId={}, url={}, waitUntil={})", pageId, url, waitUntil);
        long start = System.nanoTime();
        try {
            PageNavigationResult result = sessions.navigate(pageId, url, waitUntil, timeoutMs);
            ToolLogger.completed(log, "pageNavigate", start);
            return result;
        } catch (RuntimeException e) {
            ToolLogger.failed(log, "pageNavigate", start, e.getMessage());
            throw e;
        }
    }

    @McpTool(
            description = "Reload the current browser tab.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = false)
    )
    public PageNavigationResult pageReload(
            @McpToolParam(description = "Logical browser tab id", required = false) String pageId,
            @McpToolParam(description = "Navigation wait condition: load, domcontentloaded, networkidle, or commit. Default domcontentloaded.", required = false) String waitUntil,
            @McpToolParam(description = "Reload timeout in milliseconds", required = false) Integer timeoutMs
    ) {
        log.info("Tool call: pageReload (pageId={}, waitUntil={})", pageId, waitUntil);
        long start = System.nanoTime();
        try {
            PageNavigationResult result = sessions.reload(pageId, waitUntil, timeoutMs);
            ToolLogger.completed(log, "pageReload", start);
            return result;
        } catch (RuntimeException e) {
            ToolLogger.failed(log, "pageReload", start, e.getMessage());
            throw e;
        }
    }

    @McpTool(
            description = "Wait until the current browser tab reaches a load state. Use after actions that trigger navigation or dynamic loading.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = false)
    )
    public WaitResult pageWaitForLoadState(
            @McpToolParam(description = "Logical browser tab id", required = false) String pageId,
            @McpToolParam(description = "Load state: load, domcontentloaded, or networkidle. Default domcontentloaded.", required = false) String state,
            @McpToolParam(description = "Wait timeout in milliseconds", required = false) Integer timeoutMs
    ) {
        log.info("Tool call: pageWaitForLoadState (pageId={}, state={})", pageId, state);
        long start = System.nanoTime();
        try {
            WaitResult result = sessions.waitForLoadState(pageId, state, timeoutMs);
            ToolLogger.completed(log, "pageWaitForLoadState", start);
            return result;
        } catch (RuntimeException e) {
            ToolLogger.failed(log, "pageWaitForLoadState", start, e.getMessage());
            throw e;
        }
    }

    @McpTool(
            description = "Inspect the current page as a compact accessibility YAML snapshot. Use after navigation and before actions to choose stable element locators by role, name, text, label, or test id. Default root is body.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public AriaSnapshotResult pageAriaSnapshot(
            @McpToolParam(description = "Logical browser tab id", required = false) String pageId,
            @McpToolParam(description = "Snapshot root locator. Supported kinds: css, xpath, role, text, label, placeholder, altText, title, testId.", required = false) LocatorSpec locator,
            @McpToolParam(description = "Snapshot timeout in milliseconds", required = false) Integer timeoutMs
    ) {
        log.info("Tool call: pageAriaSnapshot (pageId={}, locator={})", pageId, locator);
        long start = System.nanoTime();
        try {
            AriaSnapshotResult result = sessions.ariaSnapshot(pageId, locator, timeoutMs);
            ToolLogger.completed(log, "pageAriaSnapshot", start);
            return result;
        } catch (RuntimeException e) {
            ToolLogger.failed(log, "pageAriaSnapshot", start, e.getMessage());
            throw e;
        }
    }
}
