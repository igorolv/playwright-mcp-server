package ru.it_spectrum.ai.playwright.mcp.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.it_spectrum.ai.playwright.mcp.api.LocatorSpec;
import ru.it_spectrum.ai.playwright.mcp.api.LocatorWaitResult;
import ru.it_spectrum.ai.playwright.mcp.api.PageNavigationResult;
import ru.it_spectrum.ai.playwright.mcp.api.PageSnapshotResult;
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
            description = "Open a URL in the browser. Use this as the first step for live site work, then call pageSnapshot to inspect the page and choose locators. The server creates and reuses one default browser session automatically.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = false)
    )
    public PageNavigationResult pageNavigate(
            @McpToolParam(description = "Absolute URL or URL relative to the context baseUrl") String url,
            @McpToolParam(description = "Navigation wait condition: load, domcontentloaded, networkidle, or commit. Default domcontentloaded.", required = false) String waitUntil,
            @McpToolParam(description = "Navigation timeout in milliseconds", required = false) Integer timeoutMs
    ) {
        log.info("Tool call: pageNavigate (url={}, waitUntil={})", url, waitUntil);
        long start = System.nanoTime();
        try {
            PageNavigationResult result = sessions.navigate(url, waitUntil, timeoutMs);
            ToolLogger.completed(log, "pageNavigate", start);
            return result;
        } catch (RuntimeException e) {
            ToolLogger.failed(log, "pageNavigate", start, e.getMessage());
            throw e;
        }
    }

    @McpTool(
            description = "Inspect the current page for orientation. A full body snapshot is valid when you have just navigated, logged in, opened a menu, or otherwise need to discover what exists on the page and choose locators. Returns compact accessibility YAML for page STRUCTURE (headings, landmarks, links, text, roles) plus locator targets. Grids and data-like tables are collapsed to a one-line summary by default so broad snapshots stay useful, and the collapse field explains what was collapsed. After you know the target locator, prefer pageWaitForLocator for state checks and locatorText for reading a specific row, cell, status, or input value instead of repeatedly snapshotting the full body. Use a root locator such as nav, aside, main, a dialog, iframe body, table, or form when you only need one region. Extra detail flags add response size and time: includeControls=true returns interactive controls and states; includeTooltips=true also hovers icon-only controls for tooltips; includeGrids=true reads rendered rows and columns inside tables and ARIA grids/treegrids. Set collapseSnapshot=false only when you need the raw Playwright aria snapshot for a narrow locator or collapse debugging. Use maxControls and maxRows to bound large pages. The result reports matchedCount: the root locator should match one region. matchedCount=0 means it did not resolve and an empty snapshot is returned immediately without waiting, so fix the locator instead of retrying or raising the timeout; matchedCount>1 means the snapshot, controls and grids describe only the first match.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public PageSnapshotResult pageSnapshot(
            @McpToolParam(description = "Snapshot root locator. Default body is good for initial orientation; pass a narrower locator for a known region such as nav, main, dialog, iframe body, table, or form.", required = false) LocatorSpec locator,
            @McpToolParam(description = "When true, also return interactive controls (inputs, selects, buttons, checkboxes, radios, switches) with states and labels. Prefer a narrow root locator on large forms.", required = false) Boolean includeControls,
            @McpToolParam(description = "When true (and includeControls is true), hover icon-only/nameless controls to capture hover tooltip text. Slower; use only when controls show icon codes such as account_balance.", required = false) Boolean includeTooltips,
            @McpToolParam(description = "When true, also return rendered rows and columns inside tables and ARIA grids/treegrids. Prefer locatorText for one known row or cell.", required = false) Boolean includeGrids,
            @McpToolParam(description = "When false, return the raw Playwright aria snapshot without collapsing data-like tables/grids. Default true. Use false only with a narrow root locator or when debugging collapse behavior because responses can become large.", required = false) Boolean collapseSnapshot,
            @McpToolParam(description = "Maximum number of controls to return when includeControls is true. Default 80; server caps larger values.", required = false) Integer maxControls,
            @McpToolParam(description = "Maximum rendered rows per grid to return when includeGrids is true. Default 50; server caps larger values.", required = false) Integer maxRows,
            @McpToolParam(description = "Timeout in milliseconds for the snapshot and visibility/enabled checks", required = false) Integer timeoutMs
    ) {
        log.info("Tool call: pageSnapshot (locator={}, includeControls={}, includeTooltips={}, includeGrids={}, collapseSnapshot={})",
                locator, includeControls, includeTooltips, includeGrids, collapseSnapshot);
        long start = System.nanoTime();
        try {
            PageSnapshotResult result = sessions.pageSnapshot(locator, includeControls, includeTooltips,
                    includeGrids, collapseSnapshot, maxControls, maxRows, timeoutMs);
            ToolLogger.completed(log, "pageSnapshot", start);
            return result;
        } catch (RuntimeException e) {
            ToolLogger.failed(log, "pageSnapshot", start, e.getMessage());
            throw e;
        }
    }

    @McpTool(
            description = "Wait until the first element matching a locator reaches a state: visible, hidden, attached, or detached. Use this for targeted state checks after menu clicks, tab changes, filters, and async loading before deciding whether another snapshot is needed. It returns found=false on timeout and count with the total number of matches, so ambiguous locators are visible without failing just because several elements match. For text/value extraction after a successful wait, use locatorText.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = false)
    )
    public LocatorWaitResult pageWaitForLocator(
            @McpToolParam(description = "Element locator to wait for") LocatorSpec locator,
            @McpToolParam(description = "Target state: visible, hidden, attached, or detached. Default visible.", required = false) String state,
            @McpToolParam(description = "Wait timeout in milliseconds", required = false) Integer timeoutMs
    ) {
        log.info("Tool call: pageWaitForLocator (locator={}, state={})", locator, state);
        long start = System.nanoTime();
        try {
            LocatorWaitResult result = sessions.waitForLocator(locator, state, timeoutMs);
            ToolLogger.completed(log, "pageWaitForLocator", start);
            return result;
        } catch (RuntimeException e) {
            ToolLogger.failed(log, "pageWaitForLocator", start, e.getMessage());
            throw e;
        }
    }

}
