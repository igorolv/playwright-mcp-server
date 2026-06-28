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
            description = "Open a URL in a browser tab. Use this as the first step for live site work, then call pageSnapshot to inspect the page and choose locators. Omitted ids use the default browser, isolated context, and tab; they are created automatically.",
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
            description = "Inspect the current page. Returns a compact accessibility YAML snapshot of the page STRUCTURE (headings, landmarks, links, text, roles) plus the locator targets to act on - call this first after navigation, login, and every menu click to orient yourself, then choose stable locators by role, name, text, label, or test id. Grids and tables are collapsed to a one-line summary by default. Opt into extra detail with flags (each adds tokens and time): set includeControls=true for the interactive CONTROLS and their states (role, type, label/placeholder, enabled/disabled, checked, visible/hidden, tooltip; typed-in values are not returned); also set includeTooltips=true to hover icon-only buttons that show just an icon code (e.g. account_balance) and capture their real tooltip; set includeGrids=true to read the rows and columns inside tables and ARIA grids/treegrids (ag-Grid, Angular Material; virtualized grids keep only on-screen rows in the DOM, so renderedRowCount may be less than the real total). Default root is body; pass a root locator such as nav, aside, or main when the full page is too large, and use maxControls/maxRows to bound the response.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public PageSnapshotResult pageSnapshot(
            @McpToolParam(description = "Logical browser tab id", required = false) String pageId,
            @McpToolParam(description = "Snapshot root locator. Supported kinds: css, xpath, role, text, label, placeholder, altText, title, testId. Default body.", required = false) LocatorSpec locator,
            @McpToolParam(description = "When true, also return the interactive controls (inputs, selects, buttons, checkboxes, radios, switches) with their states and labels.", required = false) Boolean includeControls,
            @McpToolParam(description = "When true (and includeControls is true), hover icon-only/nameless controls to capture their hover tooltip text. Slower; use when buttons show only icon codes such as account_balance.", required = false) Boolean includeTooltips,
            @McpToolParam(description = "When true, also return the rows and columns inside tables and ARIA grids/treegrids.", required = false) Boolean includeGrids,
            @McpToolParam(description = "Maximum number of controls to return when includeControls is true. Default 80; server caps larger values.", required = false) Integer maxControls,
            @McpToolParam(description = "Maximum rendered rows per grid to return when includeGrids is true. Default 50; server caps larger values.", required = false) Integer maxRows,
            @McpToolParam(description = "Timeout in milliseconds for the snapshot and visibility/enabled checks", required = false) Integer timeoutMs
    ) {
        log.info("Tool call: pageSnapshot (pageId={}, locator={}, includeControls={}, includeTooltips={}, includeGrids={})",
                pageId, locator, includeControls, includeTooltips, includeGrids);
        long start = System.nanoTime();
        try {
            PageSnapshotResult result = sessions.pageSnapshot(pageId, locator, includeControls, includeTooltips,
                    includeGrids, maxControls, maxRows, timeoutMs);
            ToolLogger.completed(log, "pageSnapshot", start);
            return result;
        } catch (RuntimeException e) {
            ToolLogger.failed(log, "pageSnapshot", start, e.getMessage());
            throw e;
        }
    }

    @McpTool(
            description = "Wait until an element reaches a state: visible, hidden, attached, or detached. Use after a menu click or filter change to let asynchronous content settle before pageSnapshot, e.g. wait for role=row to become visible, or for a loading spinner to become hidden. Returns found=false instead of throwing when the state is not reached before the timeout.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = false)
    )
    public LocatorWaitResult pageWaitForLocator(
            @McpToolParam(description = "Logical browser tab id", required = false) String pageId,
            @McpToolParam(description = "Element locator to wait for") LocatorSpec locator,
            @McpToolParam(description = "Target state: visible, hidden, attached, or detached. Default visible.", required = false) String state,
            @McpToolParam(description = "Wait timeout in milliseconds", required = false) Integer timeoutMs
    ) {
        log.info("Tool call: pageWaitForLocator (pageId={}, locator={}, state={})", pageId, locator, state);
        long start = System.nanoTime();
        try {
            LocatorWaitResult result = sessions.waitForLocator(pageId, locator, state, timeoutMs);
            ToolLogger.completed(log, "pageWaitForLocator", start);
            return result;
        } catch (RuntimeException e) {
            ToolLogger.failed(log, "pageWaitForLocator", start, e.getMessage());
            throw e;
        }
    }

}
