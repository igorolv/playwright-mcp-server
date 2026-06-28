package ru.it_spectrum.ai.playwright.mcp.playwright;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitUntilState;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;
import ru.it_spectrum.ai.playwright.mcp.api.AriaSnapshotResult;
import ru.it_spectrum.ai.playwright.mcp.api.BrowserContextInfo;
import ru.it_spectrum.ai.playwright.mcp.api.BrowserContextList;
import ru.it_spectrum.ai.playwright.mcp.api.BrowserInfo;
import ru.it_spectrum.ai.playwright.mcp.api.BrowserList;
import ru.it_spectrum.ai.playwright.mcp.api.LocatorActionResult;
import ru.it_spectrum.ai.playwright.mcp.api.LocatorCountResult;
import ru.it_spectrum.ai.playwright.mcp.api.LocatorSpec;
import ru.it_spectrum.ai.playwright.mcp.api.LocatorTextResult;
import ru.it_spectrum.ai.playwright.mcp.api.PageInfo;
import ru.it_spectrum.ai.playwright.mcp.api.PageList;
import ru.it_spectrum.ai.playwright.mcp.api.PageNavigationResult;
import ru.it_spectrum.ai.playwright.mcp.api.WaitResult;
import ru.it_spectrum.ai.playwright.mcp.config.PlaywrightMcpProperties;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class PlaywrightSessionManager {

    private final PlaywrightMcpProperties properties;
    private final PlaywrightDispatcher dispatcher;
    private final LocatorResolver locators = new LocatorResolver();
    private final State state = new State();

    public PlaywrightSessionManager(PlaywrightMcpProperties properties, PlaywrightDispatcher dispatcher) {
        this.properties = properties;
        this.dispatcher = dispatcher;
    }

    public BrowserInfo launchBrowser(
            String browserId,
            String browserName,
            Boolean headless,
            String channel,
            String executablePath,
            Integer slowMoMs
    ) {
        return dispatcher.call(() -> browserInfo(browser(browserId, browserName, headless, channel, executablePath, slowMoMs)));
    }

    public BrowserList listBrowsers() {
        return dispatcher.call(() -> {
            List<BrowserInfo> infos = new ArrayList<>();
            for (ManagedBrowser browser : state.browsers.values()) {
                infos.add(browserInfo(browser));
            }
            return new BrowserList(infos);
        });
    }

    public BrowserInfo closeBrowser(String browserId) {
        return dispatcher.call(() -> {
            String id = normalizeBrowserId(browserId);
            ManagedBrowser browser = state.browsers.remove(id);
            if (browser == null) {
                throw new IllegalArgumentException("Unknown browserId: " + id);
            }
            BrowserInfo info = browserInfo(browser);
            removePagesForBrowser(id);
            removeContextsForBrowser(id);
            closeQuietly(browser.browser());
            return info;
        });
    }

    public BrowserContextInfo newContext(
            String browserId,
            String contextId,
            String baseUrl,
            Integer viewportWidth,
            Integer viewportHeight,
            String userAgent,
            String locale,
            String timezoneId,
            Boolean ignoreHttpsErrors
    ) {
        return dispatcher.call(() -> contextInfo(context(browserId, contextId, baseUrl, viewportWidth, viewportHeight,
                userAgent, locale, timezoneId, ignoreHttpsErrors)));
    }

    public BrowserContextList listContexts() {
        return dispatcher.call(() -> {
            List<BrowserContextInfo> infos = new ArrayList<>();
            for (ManagedBrowserContext context : state.contexts.values()) {
                infos.add(contextInfo(context));
            }
            return new BrowserContextList(infos);
        });
    }

    public BrowserContextInfo closeContext(String contextId) {
        return dispatcher.call(() -> {
            String id = normalizeContextId(contextId);
            ManagedBrowserContext context = state.contexts.remove(id);
            if (context == null) {
                throw new IllegalArgumentException("Unknown contextId: " + id);
            }
            BrowserContextInfo info = contextInfo(context);
            removePagesForContext(id);
            closeQuietly(context.context());
            return info;
        });
    }

    public PageInfo newPage(String contextId, String pageId) {
        return dispatcher.call(() -> pageInfo(page(contextId, pageId)));
    }

    public PageList listPages() {
        return dispatcher.call(() -> {
            List<PageInfo> infos = new ArrayList<>();
            for (ManagedPage page : state.pages.values()) {
                infos.add(pageInfo(page));
            }
            return new PageList(infos);
        });
    }

    public PageInfo closePage(String pageId) {
        return dispatcher.call(() -> {
            String id = normalizePageId(pageId);
            ManagedPage page = state.pages.remove(id);
            if (page == null) {
                throw new IllegalArgumentException("Unknown pageId: " + id);
            }
            PageInfo info = pageInfo(page);
            closeQuietly(page.page());
            return info;
        });
    }

    public PageNavigationResult navigate(String pageId, String url, String waitUntil, Integer timeoutMs) {
        return dispatcher.call(() -> {
            ManagedPage managed = page(null, pageId);
            Page.NavigateOptions options = new Page.NavigateOptions()
                    .setWaitUntil(waitUntil(waitUntil));
            setTimeout(options, timeoutMs);
            Response response = managed.page().navigate(required(url, "url"), options);
            return navigationResult(managed, response);
        });
    }

    public PageNavigationResult reload(String pageId, String waitUntil, Integer timeoutMs) {
        return dispatcher.call(() -> {
            ManagedPage managed = page(null, pageId);
            Page.ReloadOptions options = new Page.ReloadOptions()
                    .setWaitUntil(waitUntil(waitUntil));
            setTimeout(options, timeoutMs);
            Response response = managed.page().reload(options);
            return navigationResult(managed, response);
        });
    }

    public WaitResult waitForLoadState(String pageId, String stateName, Integer timeoutMs) {
        return dispatcher.call(() -> {
            ManagedPage managed = page(null, pageId);
            Page.WaitForLoadStateOptions options = new Page.WaitForLoadStateOptions();
            if (timeoutMs != null && timeoutMs > 0) {
                options.setTimeout(timeoutMs);
            }
            LoadState loadState = loadState(stateName);
            managed.page().waitForLoadState(loadState, options);
            return new WaitResult(managed.pageId(), "loadState:" + loadState.name().toLowerCase(Locale.ROOT),
                    managed.page().url(), safeTitle(managed.page()));
        });
    }

    public AriaSnapshotResult ariaSnapshot(String pageId, LocatorSpec locator, Integer timeoutMs) {
        return dispatcher.call(() -> {
            ManagedPage managed = page(null, pageId);
            LocatorSpec actualLocator = locator != null
                    ? locator
                    : LocatorSpec.css(properties.snapshot().defaultRootSelector());
            Locator resolved = locators.resolve(managed.page(), actualLocator);
            Locator.AriaSnapshotOptions options = new Locator.AriaSnapshotOptions();
            if (timeoutMs != null && timeoutMs > 0) {
                options.setTimeout(timeoutMs);
            }
            return new AriaSnapshotResult(managed.pageId(), actualLocator, timeoutMs, resolved.ariaSnapshot(options));
        });
    }

    public LocatorActionResult click(String pageId, LocatorSpec locator, Integer timeoutMs, Boolean force) {
        return locatorAction(pageId, locator, "click", resolved -> {
            Locator.ClickOptions options = new Locator.ClickOptions();
            if (timeoutMs != null && timeoutMs > 0) {
                options.setTimeout(timeoutMs);
            }
            if (force != null) {
                options.setForce(force);
            }
            resolved.click(options);
        });
    }

    public LocatorActionResult fill(String pageId, LocatorSpec locator, String value, Integer timeoutMs) {
        return locatorAction(pageId, locator, "fill", resolved -> {
            Locator.FillOptions options = new Locator.FillOptions();
            if (timeoutMs != null && timeoutMs > 0) {
                options.setTimeout(timeoutMs);
            }
            resolved.fill(required(value, "value"), options);
        });
    }

    public LocatorActionResult press(String pageId, LocatorSpec locator, String key, Integer timeoutMs) {
        return locatorAction(pageId, locator, "press", resolved -> {
            Locator.PressOptions options = new Locator.PressOptions();
            if (timeoutMs != null && timeoutMs > 0) {
                options.setTimeout(timeoutMs);
            }
            resolved.press(required(key, "key"), options);
        });
    }

    public LocatorActionResult hover(String pageId, LocatorSpec locator, Integer timeoutMs) {
        return locatorAction(pageId, locator, "hover", resolved -> {
            Locator.HoverOptions options = new Locator.HoverOptions();
            if (timeoutMs != null && timeoutMs > 0) {
                options.setTimeout(timeoutMs);
            }
            resolved.hover(options);
        });
    }

    public LocatorActionResult check(String pageId, LocatorSpec locator, Boolean checked, Integer timeoutMs) {
        return locatorAction(pageId, locator, Boolean.FALSE.equals(checked) ? "uncheck" : "check", resolved -> {
            if (Boolean.FALSE.equals(checked)) {
                Locator.UncheckOptions options = new Locator.UncheckOptions();
                if (timeoutMs != null && timeoutMs > 0) {
                    options.setTimeout(timeoutMs);
                }
                resolved.uncheck(options);
            } else {
                Locator.CheckOptions options = new Locator.CheckOptions();
                if (timeoutMs != null && timeoutMs > 0) {
                    options.setTimeout(timeoutMs);
                }
                resolved.check(options);
            }
        });
    }

    public LocatorTextResult locatorText(String pageId, LocatorSpec locator, Integer timeoutMs) {
        return dispatcher.call(() -> {
            ManagedPage managed = page(null, pageId);
            Locator resolved = locators.resolve(managed.page(), locator);
            int count = resolved.count();
            Locator first = resolved.first();
            return new LocatorTextResult(
                    managed.pageId(),
                    locator,
                    count,
                    count > 0 ? safeTextContent(first, timeoutMs) : null,
                    count > 0 ? safeInnerText(first, timeoutMs) : null,
                    count > 0 ? safeInputValue(first, timeoutMs) : null,
                    count > 0 ? safeVisible(first, timeoutMs) : null,
                    count > 0 ? safeEnabled(first, timeoutMs) : null,
                    safeAllTextContents(resolved));
        });
    }

    public LocatorCountResult locatorCount(String pageId, LocatorSpec locator) {
        return dispatcher.call(() -> {
            ManagedPage managed = page(null, pageId);
            return new LocatorCountResult(managed.pageId(), locator, locators.resolve(managed.page(), locator).count());
        });
    }

    private LocatorActionResult locatorAction(String pageId, LocatorSpec locator, String action, LocatorOperation operation) {
        return dispatcher.call(() -> {
            ManagedPage managed = page(null, pageId);
            LocatorSpec actualLocator = locator != null ? locator : LocatorSpec.css(properties.snapshot().defaultRootSelector());
            Locator resolved = locators.resolve(managed.page(), actualLocator);
            operation.apply(resolved);
            return new LocatorActionResult(managed.pageId(), action, actualLocator,
                    managed.page().url(), safeTitle(managed.page()));
        });
    }

    private ManagedBrowser browser(
            String browserId,
            String browserName,
            Boolean headless,
            String channel,
            String executablePath,
            Integer slowMoMs
    ) {
        String id = normalizeBrowserId(browserId);
        ManagedBrowser existing = state.browsers.get(id);
        if (existing != null) {
            return existing;
        }
        if (state.browsers.size() >= properties.sessions().maxBrowsers()) {
            throw new IllegalStateException("Maximum browser count reached: " + properties.sessions().maxBrowsers());
        }
        Playwright playwright = playwright();
        String actualBrowserName = normalizeBrowserName(browserName);
        BrowserType type = browserType(playwright, actualBrowserName);
        BrowserType.LaunchOptions options = launchOptions(headless, channel, executablePath, slowMoMs);
        Browser launched = type.launch(options);
        ManagedBrowser managed = new ManagedBrowser(
                id,
                actualBrowserName,
                blankToNull(channel) != null ? channel.trim() : properties.browser().channel(),
                headless != null ? headless : properties.browser().headless(),
                launched,
                Instant.now());
        state.browsers.put(id, managed);
        return managed;
    }

    private ManagedBrowserContext context(
            String browserId,
            String contextId,
            String baseUrl,
            Integer viewportWidth,
            Integer viewportHeight,
            String userAgent,
            String locale,
            String timezoneId,
            Boolean ignoreHttpsErrors
    ) {
        String id = normalizeContextId(contextId);
        ManagedBrowserContext existing = state.contexts.get(id);
        if (existing != null) {
            return existing;
        }
        if (state.contexts.size() >= properties.sessions().maxContexts()) {
            throw new IllegalStateException("Maximum browser context count reached: " + properties.sessions().maxContexts());
        }
        ManagedBrowser parent = browser(browserId, null, null, null, null, null);
        int width = viewportWidth != null && viewportWidth > 0 ? viewportWidth : properties.context().viewportWidth();
        int height = viewportHeight != null && viewportHeight > 0 ? viewportHeight : properties.context().viewportHeight();
        String actualUserAgent = firstNonBlank(userAgent, properties.context().userAgent());
        String actualLocale = firstNonBlank(locale, properties.context().locale());
        String actualTimezoneId = firstNonBlank(timezoneId, properties.context().timezoneId());
        boolean actualIgnoreHttpsErrors = ignoreHttpsErrors != null
                ? ignoreHttpsErrors
                : properties.context().ignoreHttpsErrors();

        Browser.NewContextOptions options = new Browser.NewContextOptions()
                .setViewportSize(width, height)
                .setIgnoreHTTPSErrors(actualIgnoreHttpsErrors);
        if (blankToNull(baseUrl) != null) {
            options.setBaseURL(baseUrl.trim());
        }
        if (actualUserAgent != null) {
            options.setUserAgent(actualUserAgent);
        }
        if (actualLocale != null) {
            options.setLocale(actualLocale);
        }
        if (actualTimezoneId != null) {
            options.setTimezoneId(actualTimezoneId);
        }

        BrowserContext context = parent.browser().newContext(options);
        ManagedBrowserContext managed = new ManagedBrowserContext(
                id,
                parent.browserId(),
                context,
                blankToNull(baseUrl),
                width,
                height,
                actualUserAgent,
                actualLocale,
                actualTimezoneId,
                actualIgnoreHttpsErrors,
                Instant.now());
        state.contexts.put(id, managed);
        return managed;
    }

    private ManagedPage page(String contextId, String pageId) {
        String id = normalizePageId(pageId);
        ManagedPage existing = state.pages.get(id);
        if (existing != null && !existing.page().isClosed()) {
            return existing;
        }
        state.pages.remove(id);
        if (state.pages.size() >= properties.sessions().maxPages()) {
            throw new IllegalStateException("Maximum page count reached: " + properties.sessions().maxPages());
        }
        ManagedBrowserContext parent = context(null, contextId, null, null, null, null, null, null, null);
        Page page = parent.context().newPage();
        page.setDefaultTimeout(properties.sessions().defaultTimeoutMs());
        page.setDefaultNavigationTimeout(properties.sessions().defaultTimeoutMs());
        ManagedPage managed = new ManagedPage(id, parent.contextId(), page, Instant.now());
        state.pages.put(id, managed);
        return managed;
    }

    private Playwright playwright() {
        if (state.playwright == null) {
            state.playwright = Playwright.create();
        }
        return state.playwright;
    }

    private BrowserType browserType(Playwright playwright, String browserName) {
        return switch (browserName) {
            case "chromium" -> playwright.chromium();
            case "firefox" -> playwright.firefox();
            case "webkit" -> playwright.webkit();
            default -> throw new IllegalArgumentException("Unsupported browserName: " + browserName);
        };
    }

    private BrowserType.LaunchOptions launchOptions(Boolean headless, String channel, String executablePath, Integer slowMoMs) {
        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                .setHeadless(headless != null ? headless : properties.browser().headless())
                .setSlowMo(slowMoMs != null && slowMoMs >= 0 ? slowMoMs : properties.browser().slowMoMs());
        String actualChannel = firstNonBlank(channel, properties.browser().channel());
        if (actualChannel != null) {
            options.setChannel(actualChannel);
        }
        String actualExecutable = firstNonBlank(executablePath, properties.browser().executablePath());
        if (actualExecutable != null) {
            options.setExecutablePath(Path.of(actualExecutable));
        }
        return options;
    }

    private BrowserInfo browserInfo(ManagedBrowser managed) {
        return new BrowserInfo(
                managed.browserId(),
                managed.browserName(),
                managed.channel(),
                managed.headless(),
                managed.browser().isConnected(),
                managed.createdAt());
    }

    private BrowserContextInfo contextInfo(ManagedBrowserContext managed) {
        return new BrowserContextInfo(
                managed.contextId(),
                managed.browserId(),
                managed.baseUrl(),
                managed.viewportWidth(),
                managed.viewportHeight(),
                managed.userAgent(),
                managed.locale(),
                managed.timezoneId(),
                managed.ignoreHttpsErrors(),
                managed.createdAt());
    }

    private PageInfo pageInfo(ManagedPage managed) {
        Page page = managed.page();
        boolean closed = page.isClosed();
        return new PageInfo(
                managed.pageId(),
                managed.contextId(),
                closed ? null : page.url(),
                closed ? null : safeTitle(page),
                closed,
                managed.createdAt());
    }

    private PageNavigationResult navigationResult(ManagedPage managed, Response response) {
        return new PageNavigationResult(
                managed.pageId(),
                managed.page().url(),
                safeTitle(managed.page()),
                response == null ? null : response.status(),
                response == null ? null : response.statusText(),
                response == null ? null : response.ok());
    }

    private void removePagesForBrowser(String browserId) {
        for (Iterator<Map.Entry<String, ManagedPage>> it = state.pages.entrySet().iterator(); it.hasNext(); ) {
            ManagedPage page = it.next().getValue();
            ManagedBrowserContext context = state.contexts.get(page.contextId());
            if (context != null && context.browserId().equals(browserId)) {
                closeQuietly(page.page());
                it.remove();
            }
        }
    }

    private void removePagesForContext(String contextId) {
        for (Iterator<Map.Entry<String, ManagedPage>> it = state.pages.entrySet().iterator(); it.hasNext(); ) {
            ManagedPage page = it.next().getValue();
            if (page.contextId().equals(contextId)) {
                closeQuietly(page.page());
                it.remove();
            }
        }
    }

    private void removeContextsForBrowser(String browserId) {
        for (Iterator<Map.Entry<String, ManagedBrowserContext>> it = state.contexts.entrySet().iterator(); it.hasNext(); ) {
            ManagedBrowserContext context = it.next().getValue();
            if (context.browserId().equals(browserId)) {
                closeQuietly(context.context());
                it.remove();
            }
        }
    }

    private String normalizeBrowserId(String browserId) {
        return normalizeId(browserId, properties.sessions().defaultBrowserId());
    }

    private String normalizeContextId(String contextId) {
        return normalizeId(contextId, properties.sessions().defaultContextId());
    }

    private String normalizePageId(String pageId) {
        return normalizeId(pageId, properties.sessions().defaultPageId());
    }

    private String normalizeId(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    private String normalizeBrowserName(String browserName) {
        String value = firstNonBlank(browserName, properties.browser().browserName());
        return value.toLowerCase(Locale.ROOT);
    }

    private WaitUntilState waitUntil(String value) {
        String normalized = value == null || value.isBlank() ? "domcontentloaded" : value;
        normalized = normalized.replace("-", "").replace("_", "").toUpperCase(Locale.ROOT);
        return WaitUntilState.valueOf(normalized);
    }

    private LoadState loadState(String value) {
        String normalized = value == null || value.isBlank() ? "domcontentloaded" : value;
        normalized = normalized.replace("-", "").replace("_", "").toUpperCase(Locale.ROOT);
        return LoadState.valueOf(normalized);
    }

    private void setTimeout(Page.NavigateOptions options, Integer timeoutMs) {
        if (timeoutMs != null && timeoutMs > 0) {
            options.setTimeout(timeoutMs);
        }
    }

    private void setTimeout(Page.ReloadOptions options, Integer timeoutMs) {
        if (timeoutMs != null && timeoutMs > 0) {
            options.setTimeout(timeoutMs);
        }
    }

    private String safeTitle(Page page) {
        try {
            return page.title();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String safeTextContent(Locator locator, Integer timeoutMs) {
        try {
            Locator.TextContentOptions options = new Locator.TextContentOptions();
            if (timeoutMs != null && timeoutMs > 0) {
                options.setTimeout(timeoutMs);
            }
            return locator.textContent(options);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String safeInnerText(Locator locator, Integer timeoutMs) {
        try {
            Locator.InnerTextOptions options = new Locator.InnerTextOptions();
            if (timeoutMs != null && timeoutMs > 0) {
                options.setTimeout(timeoutMs);
            }
            return locator.innerText(options);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String safeInputValue(Locator locator, Integer timeoutMs) {
        try {
            Locator.InputValueOptions options = new Locator.InputValueOptions();
            if (timeoutMs != null && timeoutMs > 0) {
                options.setTimeout(timeoutMs);
            }
            return locator.inputValue(options);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private Boolean safeVisible(Locator locator, Integer timeoutMs) {
        try {
            Locator.IsVisibleOptions options = new Locator.IsVisibleOptions();
            if (timeoutMs != null && timeoutMs > 0) {
                options.setTimeout(timeoutMs);
            }
            return locator.isVisible(options);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private Boolean safeEnabled(Locator locator, Integer timeoutMs) {
        try {
            Locator.IsEnabledOptions options = new Locator.IsEnabledOptions();
            if (timeoutMs != null && timeoutMs > 0) {
                options.setTimeout(timeoutMs);
            }
            return locator.isEnabled(options);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private List<String> safeAllTextContents(Locator locator) {
        try {
            return locator.allTextContents();
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    private String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    private String firstNonBlank(String first, String second) {
        String value = blankToNull(first);
        return value != null ? value : blankToNull(second);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private void closeQuietly(AutoCloseable closeable) {
        try {
            closeable.close();
        } catch (Exception ignored) {
        }
    }

    @PreDestroy
    public void shutdown() {
        dispatcher.call(() -> {
            for (ManagedPage page : state.pages.values()) {
                closeQuietly(page.page());
            }
            state.pages.clear();
            for (ManagedBrowserContext context : state.contexts.values()) {
                closeQuietly(context.context());
            }
            state.contexts.clear();
            for (ManagedBrowser browser : state.browsers.values()) {
                closeQuietly(browser.browser());
            }
            state.browsers.clear();
            if (state.playwright != null) {
                closeQuietly(state.playwright);
                state.playwright = null;
            }
            return null;
        });
    }

    private interface LocatorOperation {
        void apply(Locator locator);
    }

    private static final class State {
        private Playwright playwright;
        private final Map<String, ManagedBrowser> browsers = new LinkedHashMap<>();
        private final Map<String, ManagedBrowserContext> contexts = new LinkedHashMap<>();
        private final Map<String, ManagedPage> pages = new LinkedHashMap<>();
    }
}
