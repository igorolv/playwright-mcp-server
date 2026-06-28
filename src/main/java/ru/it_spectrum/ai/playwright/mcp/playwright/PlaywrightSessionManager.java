package ru.it_spectrum.ai.playwright.mcp.playwright;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.options.WaitUntilState;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;
import ru.it_spectrum.ai.playwright.mcp.api.AriaSnapshotResult;
import ru.it_spectrum.ai.playwright.mcp.api.BrowserContextInfo;
import ru.it_spectrum.ai.playwright.mcp.api.BrowserContextList;
import ru.it_spectrum.ai.playwright.mcp.api.BrowserInfo;
import ru.it_spectrum.ai.playwright.mcp.api.BrowserList;
import ru.it_spectrum.ai.playwright.mcp.api.FormControlInfo;
import ru.it_spectrum.ai.playwright.mcp.api.FormInfo;
import ru.it_spectrum.ai.playwright.mcp.api.FormSnapshotResult;
import ru.it_spectrum.ai.playwright.mcp.api.GridInfo;
import ru.it_spectrum.ai.playwright.mcp.api.GridSnapshotResult;
import ru.it_spectrum.ai.playwright.mcp.api.LocatorActionResult;
import ru.it_spectrum.ai.playwright.mcp.api.LocatorCountResult;
import ru.it_spectrum.ai.playwright.mcp.api.LocatorSpec;
import ru.it_spectrum.ai.playwright.mcp.api.LocatorTextResult;
import ru.it_spectrum.ai.playwright.mcp.api.LocatorWaitResult;
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
import java.util.regex.Pattern;

@Service
public class PlaywrightSessionManager {

    private static final int DEFAULT_MAX_FORM_CONTROLS = 80;
    private static final int MAX_FORM_CONTROLS = 200;
    private static final int MAX_FORMS = 30;
    private static final int DEFAULT_MAX_GRID_ROWS = 50;
    private static final int MAX_GRID_ROWS = 500;
    private static final int MAX_GRID_COLUMNS = 40;
    private static final int MAX_GRIDS = 10;
    private static final int MAX_GRID_CELL_LENGTH = 200;
    private static final int MAX_TOOLTIP_PROBES = 40;
    private static final int TOOLTIP_SHOW_TIMEOUT_MS = 700;
    private static final int TOOLTIP_HOVER_TIMEOUT_MS = 800;
    /** Container selector used by Angular Material / Ant / generic overlay tooltips. */
    private static final String TOOLTIP_SELECTOR =
            ".mat-mdc-tooltip, .mat-tooltip, [role='tooltip'], .ant-tooltip-inner, .tooltip-inner";
    private static final String GRID_CONTAINER_SELECTOR = "[role='grid'], [role='treegrid'], table";
    /** Icon-font ligature accessible name, e.g. "account_balance" or "light_mode": no spaces, lowercase + underscores. */
    private static final Pattern ICON_LIGATURE = Pattern.compile("^[a-z][a-z0-9_]*$");
    /** A {@code - grid:} / {@code - treegrid:} node header line in a Playwright aria snapshot. */
    private static final Pattern ARIA_GRID_HEADER =
            Pattern.compile("^(\\s*)-\\s+(grid|treegrid)(\\s+\"[^\"]*\")?:\\s*$");
    /** A {@code - columnheader "name"} line nested under a grid. */
    private static final Pattern ARIA_COLUMNHEADER =
            Pattern.compile("^\\s*-\\s+columnheader\\s+\"([^\"]*)\".*$");
    private static final String GRID_EXTRACT_SCRIPT = """
            (el, opts) => {
              const clip = value => {
                let text = (value || '').replace(/\\s+/g, ' ').trim();
                return text.length > opts.maxCellLen ? text.slice(0, opts.maxCellLen) + '…' : text;
              };
              const role = el.getAttribute('role') || (el.tagName ? el.tagName.toLowerCase() : 'table');
              let headerCells = el.querySelectorAll("[role='columnheader']");
              if (!headerCells.length) headerCells = el.querySelectorAll('thead th');
              const columns = Array.from(headerCells).slice(0, opts.maxCols).map(c => clip(c.innerText || c.textContent));
              const rows = [];
              const pushRow = cells => {
                if (!cells.length) return false;
                rows.push(Array.from(cells).slice(0, opts.maxCols).map(c => clip(c.innerText || c.textContent)));
                return rows.length >= opts.maxRows;
              };
              const rowEls = el.querySelectorAll("[role='row']");
              if (rowEls.length) {
                for (const r of rowEls) {
                  let cells = r.querySelectorAll("[role='gridcell'], [role='cell']");
                  if (!cells.length) continue;
                  if (pushRow(cells)) break;
                }
              } else {
                for (const r of el.querySelectorAll('tbody tr')) {
                  if (pushRow(r.querySelectorAll('td'))) break;
                }
              }
              return { role, columnCount: columns.length, columns, renderedRowCount: rows.length, rows };
            }
            """;
    private static final String FORM_CONTROL_SELECTOR = """
            input,
            textarea,
            select,
            button,
            [role='button'],
            [role='textbox'],
            [role='combobox'],
            [role='checkbox'],
            [role='radio'],
            [role='switch']
            """;
    private static final String FORM_METADATA_SCRIPT = """
            element => {
              const compact = value => (value || '').replace(/\\s+/g, ' ').trim();
              const labelledBy = compact((element.getAttribute('aria-labelledby') || '')
                .split(/\\s+/)
                .map(id => document.getElementById(id))
                .filter(Boolean)
                .map(node => node.innerText || node.textContent || '')
                .join(' '));
              return {
                id: element.id || null,
                name: element.getAttribute('name') || null,
                action: element.getAttribute('action') || null,
                method: element.getAttribute('method') || null,
                label: compact(element.getAttribute('aria-label')) || labelledBy || null
              };
            }
            """;
    private static final String CONTROL_METADATA_SCRIPT = """
            element => {
              const compact = value => (value || '').replace(/\\s+/g, ' ').trim();
              const labels = [];
              if (element.labels) {
                for (const label of element.labels) {
                  const text = compact(label.innerText || label.textContent || '');
                  if (text) labels.push(text);
                }
              }
              const labelledBy = compact((element.getAttribute('aria-labelledby') || '')
                .split(/\\s+/)
                .map(id => document.getElementById(id))
                .filter(Boolean)
                .map(node => node.innerText || node.textContent || '')
                .join(' '));
              const tagName = element.tagName ? element.tagName.toLowerCase() : null;
              const type = element.getAttribute('type') || null;
              const role = element.getAttribute('role') || null;
              const text = tagName === 'input' || tagName === 'textarea'
                ? null
                : compact(element.innerText || element.textContent || '');
              const label = labels.join(' | ') || null;
              const ariaLabel = compact(element.getAttribute('aria-label'));
              const placeholder = element.getAttribute('placeholder') || null;
              const accessibleName = label || ariaLabel || labelledBy || placeholder || text
                || element.getAttribute('name') || element.id || null;
              const describedBy = compact((element.getAttribute('aria-describedby') || '')
                .split(/\\s+/)
                .map(id => document.getElementById(id))
                .filter(Boolean)
                .map(node => node.innerText || node.textContent || '')
                .join(' ')) || null;
              const title = compact(element.getAttribute('title')) || null;
              const form = element.closest('form');
              return {
                tagName,
                type,
                role,
                accessibleName,
                label,
                placeholder,
                name: element.getAttribute('name') || null,
                id: element.id || null,
                text,
                title,
                describedBy,
                checked: typeof element.checked === 'boolean' ? element.checked : null,
                form: form ? (form.getAttribute('aria-label') || form.getAttribute('name') || form.id || null) : null
              };
            }
            """;

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
            return new AriaSnapshotResult(managed.pageId(), actualLocator, timeoutMs,
                    collapseGrids(resolved.ariaSnapshot(options)));
        });
    }

    /**
     * Replace the verbose body of every grid/treegrid node in a Playwright aria snapshot with a one-line
     * summary that lists column headers and points to {@code pageGridSnapshot} for the rows. Keeps the
     * snapshot focused on page structure and avoids duplicating (and bloating) the data already available
     * through {@code pageGridSnapshot}.
     */
    static String collapseGrids(String snapshot) {
        if (snapshot == null || snapshot.isBlank()) {
            return snapshot;
        }
        List<String> lines = snapshot.lines().toList();
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < lines.size()) {
            String line = lines.get(i);
            java.util.regex.Matcher header = ARIA_GRID_HEADER.matcher(line);
            if (!header.matches()) {
                appendLine(out, line);
                i++;
                continue;
            }
            int indent = header.group(1).length();
            String name = header.group(3) == null ? "" : header.group(3);
            List<String> columns = new ArrayList<>();
            int j = i + 1;
            while (j < lines.size() && !lines.get(j).isBlank() && indentOf(lines.get(j)) > indent) {
                java.util.regex.Matcher column = ARIA_COLUMNHEADER.matcher(lines.get(j));
                if (column.matches()) {
                    String value = column.group(1).trim();
                    if (!value.isEmpty() && !columns.contains(value)) {
                        columns.add(value);
                    }
                }
                j++;
            }
            String summary = columns.isEmpty()
                    ? "[rows collapsed - call pageGridSnapshot to read columns and rows]"
                    : "[" + columns.size() + " column(s): " + String.join(", ", columns)
                            + " - rows collapsed, call pageGridSnapshot to read rows]";
            appendLine(out, " ".repeat(indent) + "- " + header.group(2) + name + ": " + summary);
            i = j;
        }
        return out.toString();
    }

    private static void appendLine(StringBuilder out, String line) {
        if (out.length() > 0) {
            out.append('\n');
        }
        out.append(line);
    }

    private static int indentOf(String line) {
        int count = 0;
        while (count < line.length() && line.charAt(count) == ' ') {
            count++;
        }
        return count;
    }

    public FormSnapshotResult formSnapshot(String pageId, LocatorSpec locator, Integer maxControls,
                                           Boolean includeTooltips, Integer timeoutMs) {
        return dispatcher.call(() -> {
            ManagedPage managed = page(null, pageId);
            LocatorSpec actualLocator = locator != null
                    ? locator
                    : LocatorSpec.css(properties.snapshot().defaultRootSelector());
            Locator root = locators.resolve(managed.page(), actualLocator);
            Locator forms = root.locator("form");
            int formCount = safeCount(forms);
            List<FormInfo> formInfos = collectForms(forms, Math.min(formCount, MAX_FORMS));

            Locator controls = root.locator(FORM_CONTROL_SELECTOR);
            int controlCount = safeCount(controls);
            int limit = Math.min(controlCount, normalizeMaxControls(maxControls));
            List<FormControlInfo> controlInfos = collectControls(managed.page(), controls, limit,
                    Boolean.TRUE.equals(includeTooltips), timeoutMs);

            return new FormSnapshotResult(
                    managed.pageId(),
                    managed.page().url(),
                    safeTitle(managed.page()),
                    actualLocator,
                    maxControls,
                    formCount,
                    controlCount,
                    formInfos,
                    controlInfos);
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

    private List<FormInfo> collectForms(Locator forms, int limit) {
        List<FormInfo> result = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            Locator form = forms.nth(i);
            Map<String, Object> metadata = safeMetadata(form, FORM_METADATA_SCRIPT);
            result.add(new FormInfo(
                    i,
                    stringValue(metadata, "id"),
                    stringValue(metadata, "name"),
                    stringValue(metadata, "action"),
                    stringValue(metadata, "method"),
                    stringValue(metadata, "label"),
                    safeCount(form.locator(FORM_CONTROL_SELECTOR))));
        }
        return result;
    }

    private List<FormControlInfo> collectControls(Page page, Locator controls, int limit,
                                                  boolean includeTooltips, Integer timeoutMs) {
        List<FormControlInfo> result = new ArrayList<>();
        int tooltipProbes = 0;
        for (int i = 0; i < limit; i++) {
            Locator control = controls.nth(i);
            Map<String, Object> metadata = safeMetadata(control, CONTROL_METADATA_SCRIPT);
            String accessibleName = stringValue(metadata, "accessibleName");
            Boolean visible = safeVisible(control, timeoutMs);
            String tooltip = firstNonBlank(stringValue(metadata, "title"), stringValue(metadata, "describedBy"));
            if (tooltip == null && includeTooltips && tooltipProbes < MAX_TOOLTIP_PROBES
                    && Boolean.TRUE.equals(visible) && needsTooltipProbe(accessibleName)) {
                tooltipProbes++;
                tooltip = hoverTooltip(page, control);
            }
            result.add(new FormControlInfo(
                    i,
                    stringValue(metadata, "tagName"),
                    stringValue(metadata, "type"),
                    stringValue(metadata, "role"),
                    accessibleName,
                    stringValue(metadata, "label"),
                    stringValue(metadata, "placeholder"),
                    stringValue(metadata, "name"),
                    stringValue(metadata, "id"),
                    stringValue(metadata, "text"),
                    booleanValue(metadata, "checked"),
                    visible,
                    safeEnabled(control, timeoutMs),
                    stringValue(metadata, "form"),
                    tooltip));
        }
        return result;
    }

    /** Probe only elements whose accessible name is missing or just an icon-font ligature ("account_balance"). */
    private boolean needsTooltipProbe(String accessibleName) {
        return accessibleName == null || accessibleName.isBlank()
                || ICON_LIGATURE.matcher(accessibleName).matches();
    }

    /** Hover an element and return the text of the overlay tooltip it reveals, or null if none appears. */
    private String hoverTooltip(Page page, Locator control) {
        try {
            control.hover(new Locator.HoverOptions().setTimeout(TOOLTIP_HOVER_TIMEOUT_MS));
        } catch (RuntimeException e) {
            return null;
        }
        try {
            Locator tooltip = page.locator(TOOLTIP_SELECTOR).first();
            tooltip.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(TOOLTIP_SHOW_TIMEOUT_MS));
            return compact(tooltip.innerText());
        } catch (RuntimeException e) {
            return null;
        } finally {
            // Move the pointer away so the tooltip dismisses and does not bleed into the next probe.
            try {
                page.mouse().move(0, 0);
            } catch (RuntimeException ignored) {
            }
        }
    }

    public LocatorWaitResult waitForLocator(String pageId, LocatorSpec locator, String stateName, Integer timeoutMs) {
        return dispatcher.call(() -> {
            ManagedPage managed = page(null, pageId);
            Locator resolved = locators.resolve(managed.page(), required(locator));
            WaitForSelectorState state = waitForSelectorState(stateName);
            Locator.WaitForOptions options = new Locator.WaitForOptions().setState(state);
            if (timeoutMs != null && timeoutMs > 0) {
                options.setTimeout(timeoutMs);
            }
            boolean found;
            try {
                resolved.waitFor(options);
                found = true;
            } catch (com.microsoft.playwright.TimeoutError e) {
                found = false;
            }
            return new LocatorWaitResult(managed.pageId(), locator, state.name().toLowerCase(Locale.ROOT),
                    found, safeCount(resolved), managed.page().url(), safeTitle(managed.page()));
        });
    }

    public GridSnapshotResult gridSnapshot(String pageId, LocatorSpec locator, Integer maxRows, Integer timeoutMs) {
        return dispatcher.call(() -> {
            ManagedPage managed = page(null, pageId);
            LocatorSpec actualLocator = locator != null
                    ? locator
                    : LocatorSpec.css(properties.snapshot().defaultRootSelector());
            Locator root = locators.resolve(managed.page(), actualLocator);
            Locator grids = root.locator(GRID_CONTAINER_SELECTOR);
            int gridCount = safeCount(grids);
            int rowLimit = normalizeMaxGridRows(maxRows);
            Map<String, Object> opts = Map.of(
                    "maxRows", rowLimit,
                    "maxCols", MAX_GRID_COLUMNS,
                    "maxCellLen", MAX_GRID_CELL_LENGTH);
            List<GridInfo> gridInfos = new ArrayList<>();
            int limit = Math.min(gridCount, MAX_GRIDS);
            for (int i = 0; i < limit; i++) {
                gridInfos.add(extractGrid(grids.nth(i), i, opts, rowLimit, timeoutMs));
            }
            return new GridSnapshotResult(managed.pageId(), managed.page().url(), safeTitle(managed.page()),
                    actualLocator, maxRows, gridCount, gridInfos);
        });
    }

    private GridInfo extractGrid(Locator grid, int index, Map<String, Object> opts, int rowLimit, Integer timeoutMs) {
        Map<String, Object> data = safeEvaluate(grid, GRID_EXTRACT_SCRIPT, opts, timeoutMs);
        String role = stringValue(data, "role");
        List<String> columns = stringList(data.get("columns"));
        List<List<String>> rows = rowList(data.get("rows"));
        int columnCount = intValue(data.get("columnCount"), columns.size());
        int renderedRowCount = intValue(data.get("renderedRowCount"), rows.size());
        return new GridInfo(index, role, columnCount, renderedRowCount,
                renderedRowCount >= rowLimit, columns, rows);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> safeMetadata(Locator locator, String script) {
        try {
            Object value = locator.evaluate(script);
            if (value instanceof Map<?, ?> map) {
                return (Map<String, Object>) map;
            }
        } catch (RuntimeException ignored) {
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> safeEvaluate(Locator locator, String script, Object arg, Integer timeoutMs) {
        try {
            Locator target = locator;
            if (timeoutMs != null && timeoutMs > 0) {
                target.waitFor(new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.ATTACHED)
                        .setTimeout(timeoutMs));
            }
            Object value = target.evaluate(script, arg);
            if (value instanceof Map<?, ?> map) {
                return (Map<String, Object>) map;
            }
        } catch (RuntimeException ignored) {
        }
        return Map.of();
    }

    private List<String> stringList(Object value) {
        List<String> result = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object item : list) {
                result.add(item == null ? "" : String.valueOf(item));
            }
        }
        return result;
    }

    private List<List<String>> rowList(Object value) {
        List<List<String>> result = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object row : list) {
                result.add(stringList(row));
            }
        }
        return result;
    }

    private int intValue(Object value, int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return defaultValue;
    }

    private String compact(String value) {
        if (value == null) {
            return null;
        }
        String text = value.replaceAll("\\s+", " ").trim();
        return text.isEmpty() ? null : text;
    }

    private int normalizeMaxGridRows(Integer maxRows) {
        if (maxRows == null || maxRows <= 0) {
            return DEFAULT_MAX_GRID_ROWS;
        }
        return Math.min(maxRows, MAX_GRID_ROWS);
    }

    private WaitForSelectorState waitForSelectorState(String value) {
        String normalized = value == null || value.isBlank() ? "visible" : value;
        normalized = normalized.replace("-", "").replace("_", "").toUpperCase(Locale.ROOT);
        return WaitForSelectorState.valueOf(normalized);
    }

    private LocatorSpec required(LocatorSpec locator) {
        if (locator == null) {
            throw new IllegalArgumentException("locator is required");
        }
        return locator;
    }

    private int safeCount(Locator locator) {
        try {
            return locator.count();
        } catch (RuntimeException e) {
            return 0;
        }
    }

    private int normalizeMaxControls(Integer maxControls) {
        if (maxControls == null || maxControls <= 0) {
            return DEFAULT_MAX_FORM_CONTROLS;
        }
        return Math.min(maxControls, MAX_FORM_CONTROLS);
    }

    private String stringValue(Map<String, Object> values, String name) {
        Object value = values.get(name);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? null : text;
    }

    private Boolean booleanValue(Map<String, Object> values, String name) {
        Object value = values.get(name);
        return value instanceof Boolean booleanValue ? booleanValue : null;
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
