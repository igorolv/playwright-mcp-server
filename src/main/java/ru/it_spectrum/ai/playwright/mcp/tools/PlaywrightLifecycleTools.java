package ru.it_spectrum.ai.playwright.mcp.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.it_spectrum.ai.playwright.mcp.api.BrowserContextInfo;
import ru.it_spectrum.ai.playwright.mcp.api.BrowserContextList;
import ru.it_spectrum.ai.playwright.mcp.api.BrowserInfo;
import ru.it_spectrum.ai.playwright.mcp.api.BrowserList;
import ru.it_spectrum.ai.playwright.mcp.api.PageInfo;
import ru.it_spectrum.ai.playwright.mcp.api.PageList;
import ru.it_spectrum.ai.playwright.mcp.playwright.PlaywrightSessionManager;

@Service
@ConditionalOnProperty(prefix = "playwright-mcp.tools", name = "lifecycle", havingValue = "true", matchIfMissing = true)
public class PlaywrightLifecycleTools {

    private static final Logger log = LoggerFactory.getLogger(PlaywrightLifecycleTools.class);

    private final PlaywrightSessionManager sessions;

    public PlaywrightLifecycleTools(PlaywrightSessionManager sessions) {
        this.sessions = sessions;
    }

    @McpTool(
            description = "Launch or reuse a browser process. Usually skip this because pageNavigate starts the default browser automatically. Use it to choose chromium/firefox/webkit, headed mode, channel, executable, or slow motion.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = true)
    )
    public BrowserInfo launchBrowser(
            @McpToolParam(description = "Logical browser process id", required = false) String browserId,
            @McpToolParam(description = "Browser engine: chromium, firefox, or webkit", required = false) String browserName,
            @McpToolParam(description = "Run without a visible browser window. Default comes from server configuration.", required = false) Boolean headless,
            @McpToolParam(description = "Browser channel, e.g. chrome, msedge, chrome-beta", required = false) String channel,
            @McpToolParam(description = "Path to a browser executable on the host machine", required = false) String executablePath,
            @McpToolParam(description = "Slow motion delay in milliseconds for debugging", required = false) Integer slowMoMs
    ) {
        log.info("Tool call: launchBrowser (browserId={}, browserName={}, channel={})", browserId, browserName, channel);
        long start = System.nanoTime();
        try {
            BrowserInfo result = sessions.launchBrowser(browserId, browserName, headless, channel, executablePath, slowMoMs);
            ToolLogger.completed(log, "launchBrowser", start);
            return result;
        } catch (RuntimeException e) {
            ToolLogger.failed(log, "launchBrowser", start, e.getMessage());
            throw e;
        }
    }

    @McpTool(
            description = "List open browser processes.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public BrowserList listBrowsers() {
        log.info("Tool call: listBrowsers");
        long start = System.nanoTime();
        try {
            BrowserList result = sessions.listBrowsers();
            ToolLogger.completed(log, "listBrowsers", start);
            return result;
        } catch (RuntimeException e) {
            ToolLogger.failed(log, "listBrowsers", start, e.getMessage());
            throw e;
        }
    }

    @McpTool(
            description = "Close a browser process and all isolated contexts and tabs under it.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = false)
    )
    public BrowserInfo closeBrowser(
            @McpToolParam(description = "Logical browser process id", required = false) String browserId
    ) {
        log.info("Tool call: closeBrowser (browserId={})", browserId);
        long start = System.nanoTime();
        try {
            BrowserInfo result = sessions.closeBrowser(browserId);
            ToolLogger.completed(log, "closeBrowser", start);
            return result;
        } catch (RuntimeException e) {
            ToolLogger.failed(log, "closeBrowser", start, e.getMessage());
            throw e;
        }
    }

    @McpTool(
            description = "Create or reuse an isolated browser context. Usually skip this because pageNavigate creates the default context. Use it for separate cookies/localStorage, baseUrl, viewport, user agent, locale, or timezone.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = true)
    )
    public BrowserContextInfo newBrowserContext(
            @McpToolParam(description = "Parent browser process id. Omit to use or launch the default browser.", required = false) String browserId,
            @McpToolParam(description = "Logical isolated context id. Omit to use the default context.", required = false) String contextId,
            @McpToolParam(description = "Base URL used for relative navigation", required = false) String baseUrl,
            @McpToolParam(description = "Viewport width in pixels", required = false) Integer viewportWidth,
            @McpToolParam(description = "Viewport height in pixels", required = false) Integer viewportHeight,
            @McpToolParam(description = "User-Agent string for pages in this context", required = false) String userAgent,
            @McpToolParam(description = "Locale, e.g. en-US", required = false) String locale,
            @McpToolParam(description = "Timezone id, e.g. Europe/Moscow", required = false) String timezoneId,
            @McpToolParam(description = "Ignore HTTPS certificate errors", required = false) Boolean ignoreHttpsErrors
    ) {
        log.info("Tool call: newBrowserContext (browserId={}, contextId={})", browserId, contextId);
        long start = System.nanoTime();
        try {
            BrowserContextInfo result = sessions.newContext(browserId, contextId, baseUrl, viewportWidth, viewportHeight,
                    userAgent, locale, timezoneId, ignoreHttpsErrors);
            ToolLogger.completed(log, "newBrowserContext", start);
            return result;
        } catch (RuntimeException e) {
            ToolLogger.failed(log, "newBrowserContext", start, e.getMessage());
            throw e;
        }
    }

    @McpTool(
            description = "List open isolated browser contexts.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public BrowserContextList listBrowserContexts() {
        log.info("Tool call: listBrowserContexts");
        long start = System.nanoTime();
        try {
            BrowserContextList result = sessions.listContexts();
            ToolLogger.completed(log, "listBrowserContexts", start);
            return result;
        } catch (RuntimeException e) {
            ToolLogger.failed(log, "listBrowserContexts", start, e.getMessage());
            throw e;
        }
    }

    @McpTool(
            description = "Close an isolated browser context and all tabs under it.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = false)
    )
    public BrowserContextInfo closeBrowserContext(
            @McpToolParam(description = "Logical isolated context id", required = false) String contextId
    ) {
        log.info("Tool call: closeBrowserContext (contextId={})", contextId);
        long start = System.nanoTime();
        try {
            BrowserContextInfo result = sessions.closeContext(contextId);
            ToolLogger.completed(log, "closeBrowserContext", start);
            return result;
        } catch (RuntimeException e) {
            ToolLogger.failed(log, "closeBrowserContext", start, e.getMessage());
            throw e;
        }
    }

    @McpTool(
            description = "Create or reuse a browser tab inside an isolated context. Usually skip this because pageNavigate creates the default tab.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = true)
    )
    public PageInfo newPage(
            @McpToolParam(description = "Parent isolated context id. Omit to use the default context.", required = false) String contextId,
            @McpToolParam(description = "Logical browser tab id. Omit to use the default tab.", required = false) String pageId
    ) {
        log.info("Tool call: newPage (contextId={}, pageId={})", contextId, pageId);
        long start = System.nanoTime();
        try {
            PageInfo result = sessions.newPage(contextId, pageId);
            ToolLogger.completed(log, "newPage", start);
            return result;
        } catch (RuntimeException e) {
            ToolLogger.failed(log, "newPage", start, e.getMessage());
            throw e;
        }
    }

    @McpTool(
            description = "List open browser tabs.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public PageList listPages() {
        log.info("Tool call: listPages");
        long start = System.nanoTime();
        try {
            PageList result = sessions.listPages();
            ToolLogger.completed(log, "listPages", start);
            return result;
        } catch (RuntimeException e) {
            ToolLogger.failed(log, "listPages", start, e.getMessage());
            throw e;
        }
    }

    @McpTool(
            description = "Close a browser tab.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = false)
    )
    public PageInfo closePage(
            @McpToolParam(description = "Logical browser tab id", required = false) String pageId
    ) {
        log.info("Tool call: closePage (pageId={})", pageId);
        long start = System.nanoTime();
        try {
            PageInfo result = sessions.closePage(pageId);
            ToolLogger.completed(log, "closePage", start);
            return result;
        } catch (RuntimeException e) {
            ToolLogger.failed(log, "closePage", start, e.getMessage());
            throw e;
        }
    }
}
