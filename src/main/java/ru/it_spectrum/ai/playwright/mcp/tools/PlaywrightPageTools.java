package ru.it_spectrum.ai.playwright.mcp.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.it_spectrum.ai.playwright.mcp.api.AriaSnapshotResult;
import ru.it_spectrum.ai.playwright.mcp.api.FormSnapshotResult;
import ru.it_spectrum.ai.playwright.mcp.api.GridSnapshotResult;
import ru.it_spectrum.ai.playwright.mcp.api.LocatorSpec;
import ru.it_spectrum.ai.playwright.mcp.api.LocatorWaitResult;
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
            description = "Open a URL in a browser tab. Use this as the first step for live site work, then call pageAriaSnapshot to inspect the page and choose locators. Omitted ids use the default browser, isolated context, and tab; they are created automatically.",
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
            description = "Inspect the current page STRUCTURE as a compact accessibility YAML snapshot: headings, landmarks, links, text, and roles, plus the locator targets to act on. Call this first after navigation, login, and every menu click to orient yourself, then choose stable locators by role, name, text, label, or test id. Grids and tables are collapsed to a one-line summary here - call pageGridSnapshot to read their rows. For control states and labels (enabled/disabled, checked, placeholder, and icon-button tooltips) call pageFormSnapshot instead of re-reading them here. Default root is body; pass a root locator such as nav, aside, or main when the full page is too large.",
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

    @McpTool(
            description = "List the interactive CONTROLS on the page with their states - the detail pageAriaSnapshot does not carry. Use this after pageAriaSnapshot when you need to act on or describe inputs, selects, textareas, buttons, checkboxes, radios, and switches: returns role, type, label/placeholder, enabled/disabled, checked, visible/hidden, and a tooltip. Set includeTooltips=true to hover icon buttons that show only an icon code (e.g. account_balance) and capture their real tooltip. Values typed into fields are not returned. Default root is body; use maxControls to keep responses small.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public FormSnapshotResult pageFormSnapshot(
            @McpToolParam(description = "Logical browser tab id", required = false) String pageId,
            @McpToolParam(description = "Root locator for the form/control inventory. Supported kinds: css, xpath, role, text, label, placeholder, altText, title, testId.", required = false) LocatorSpec locator,
            @McpToolParam(description = "Maximum number of controls to return. Default 80; server caps larger values.", required = false) Integer maxControls,
            @McpToolParam(description = "When true, hover icon buttons and other nameless/icon-ligature controls to capture their hover tooltip text. Slower; use when buttons show only icon codes such as account_balance.", required = false) Boolean includeTooltips,
            @McpToolParam(description = "Timeout in milliseconds for visibility/enabled checks", required = false) Integer timeoutMs
    ) {
        log.info("Tool call: pageFormSnapshot (pageId={}, locator={}, maxControls={}, includeTooltips={})",
                pageId, locator, maxControls, includeTooltips);
        long start = System.nanoTime();
        try {
            FormSnapshotResult result = sessions.formSnapshot(pageId, locator, maxControls, includeTooltips, timeoutMs);
            ToolLogger.completed(log, "pageFormSnapshot", start);
            return result;
        } catch (RuntimeException e) {
            ToolLogger.failed(log, "pageFormSnapshot", start, e.getMessage());
            throw e;
        }
    }

    @McpTool(
            description = "Wait until an element reaches a state: visible, hidden, attached, or detached. Use after a menu click or filter change to let asynchronous content settle before pageAriaSnapshot, e.g. wait for role=row to become visible, or for a loading spinner to become hidden. Returns found=false instead of throwing when the state is not reached before the timeout.",
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

    @McpTool(
            description = "Read the DATA inside tables and grids as structured rows and columns. Use this whenever pageAriaSnapshot shows a collapsed grid/table summary and you need its contents: covers HTML tables and ARIA grids/treegrids such as ag-Grid or Angular Material tables. Returns column headers and rendered row cells. Note: virtualized grids keep only on-screen rows in the DOM, so renderedRowCount may be less than the real total. Default root is body; use maxRows to bound the response.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public GridSnapshotResult pageGridSnapshot(
            @McpToolParam(description = "Logical browser tab id", required = false) String pageId,
            @McpToolParam(description = "Root locator to search for grids/tables. Supported kinds: css, xpath, role, text, label, placeholder, altText, title, testId.", required = false) LocatorSpec locator,
            @McpToolParam(description = "Maximum rendered rows per grid to return. Default 50; server caps larger values.", required = false) Integer maxRows,
            @McpToolParam(description = "Timeout in milliseconds to wait for a grid to be attached before reading", required = false) Integer timeoutMs
    ) {
        log.info("Tool call: pageGridSnapshot (pageId={}, locator={}, maxRows={})", pageId, locator, maxRows);
        long start = System.nanoTime();
        try {
            GridSnapshotResult result = sessions.gridSnapshot(pageId, locator, maxRows, timeoutMs);
            ToolLogger.completed(log, "pageGridSnapshot", start);
            return result;
        } catch (RuntimeException e) {
            ToolLogger.failed(log, "pageGridSnapshot", start, e.getMessage());
            throw e;
        }
    }
}
