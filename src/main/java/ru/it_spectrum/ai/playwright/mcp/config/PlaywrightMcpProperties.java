package ru.it_spectrum.ai.playwright.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "playwright-mcp")
public record PlaywrightMcpProperties(
        String dataDir,
        Browser browser,
        Context context,
        Sessions sessions,
        Snapshot snapshot
) {
    public static final String DEFAULT_DATA_DIR_NAME = ".playwright-mcp-server";
    public static final String DEFAULT_BROWSER_NAME = "chromium";
    public static final String DEFAULT_BROWSER_ID = "default";
    public static final String DEFAULT_CONTEXT_ID = "default";
    public static final String DEFAULT_PAGE_ID = "default";
    public static final int DEFAULT_MAX_BROWSERS = 2;
    public static final int DEFAULT_MAX_CONTEXTS = 8;
    public static final int DEFAULT_MAX_PAGES = 16;
    public static final int DEFAULT_TIMEOUT_MS = 30_000;
    public static final int DEFAULT_VIEWPORT_WIDTH = 1440;
    public static final int DEFAULT_VIEWPORT_HEIGHT = 1000;
    public static final String DEFAULT_SNAPSHOT_ROOT_SELECTOR = "body";

    public PlaywrightMcpProperties {
        browser = browser != null ? browser : new Browser(DEFAULT_BROWSER_NAME, null, null, true, 0);
        context = context != null
                ? context
                : new Context(DEFAULT_VIEWPORT_WIDTH, DEFAULT_VIEWPORT_HEIGHT, null, null, null, false);
        sessions = sessions != null
                ? sessions
                : new Sessions(DEFAULT_BROWSER_ID, DEFAULT_CONTEXT_ID, DEFAULT_PAGE_ID,
                DEFAULT_MAX_BROWSERS, DEFAULT_MAX_CONTEXTS, DEFAULT_MAX_PAGES, DEFAULT_TIMEOUT_MS);
        snapshot = snapshot != null ? snapshot : new Snapshot(DEFAULT_SNAPSHOT_ROOT_SELECTOR);
    }

    public Path resolvedDataDir() {
        String value = dataDir;
        if (value == null || value.isBlank()) {
            value = Path.of(System.getProperty("user.home"), DEFAULT_DATA_DIR_NAME).toString();
        }
        return Path.of(value).toAbsolutePath().normalize();
    }

    public record Browser(
            @DefaultValue(DEFAULT_BROWSER_NAME) String browserName,
            String channel,
            String executablePath,
            @DefaultValue("true") boolean headless,
            @DefaultValue("0") int slowMoMs
    ) {
        public Browser {
            if (browserName == null || browserName.isBlank()) {
                browserName = DEFAULT_BROWSER_NAME;
            }
            browserName = browserName.trim().toLowerCase();
            if (slowMoMs < 0) {
                slowMoMs = 0;
            }
        }
    }

    public record Context(
            @DefaultValue("" + DEFAULT_VIEWPORT_WIDTH) int viewportWidth,
            @DefaultValue("" + DEFAULT_VIEWPORT_HEIGHT) int viewportHeight,
            String userAgent,
            String locale,
            String timezoneId,
            @DefaultValue("false") boolean ignoreHttpsErrors
    ) {
        public Context {
            if (viewportWidth <= 0) {
                viewportWidth = DEFAULT_VIEWPORT_WIDTH;
            }
            if (viewportHeight <= 0) {
                viewportHeight = DEFAULT_VIEWPORT_HEIGHT;
            }
        }
    }

    public record Sessions(
            @DefaultValue(DEFAULT_BROWSER_ID) String defaultBrowserId,
            @DefaultValue(DEFAULT_CONTEXT_ID) String defaultContextId,
            @DefaultValue(DEFAULT_PAGE_ID) String defaultPageId,
            @DefaultValue("" + DEFAULT_MAX_BROWSERS) int maxBrowsers,
            @DefaultValue("" + DEFAULT_MAX_CONTEXTS) int maxContexts,
            @DefaultValue("" + DEFAULT_MAX_PAGES) int maxPages,
            @DefaultValue("" + DEFAULT_TIMEOUT_MS) int defaultTimeoutMs
    ) {
        public Sessions {
            if (defaultBrowserId == null || defaultBrowserId.isBlank()) {
                defaultBrowserId = DEFAULT_BROWSER_ID;
            }
            if (defaultContextId == null || defaultContextId.isBlank()) {
                defaultContextId = DEFAULT_CONTEXT_ID;
            }
            if (defaultPageId == null || defaultPageId.isBlank()) {
                defaultPageId = DEFAULT_PAGE_ID;
            }
            if (maxBrowsers <= 0) {
                maxBrowsers = DEFAULT_MAX_BROWSERS;
            }
            if (maxContexts <= 0) {
                maxContexts = DEFAULT_MAX_CONTEXTS;
            }
            if (maxPages <= 0) {
                maxPages = DEFAULT_MAX_PAGES;
            }
            if (defaultTimeoutMs <= 0) {
                defaultTimeoutMs = DEFAULT_TIMEOUT_MS;
            }
        }
    }

    public record Snapshot(
            @DefaultValue(DEFAULT_SNAPSHOT_ROOT_SELECTOR) String defaultRootSelector
    ) {
        public Snapshot {
            if (defaultRootSelector == null || defaultRootSelector.isBlank()) {
                defaultRootSelector = DEFAULT_SNAPSHOT_ROOT_SELECTOR;
            }
        }
    }
}
