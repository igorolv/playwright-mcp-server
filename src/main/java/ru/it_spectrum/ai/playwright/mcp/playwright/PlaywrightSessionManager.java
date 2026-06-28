package ru.it_spectrum.ai.playwright.mcp.playwright;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.ScreenshotType;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.options.WaitUntilState;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;
import ru.it_spectrum.ai.playwright.mcp.api.ControlSnapshot;
import ru.it_spectrum.ai.playwright.mcp.api.FormControlInfo;
import ru.it_spectrum.ai.playwright.mcp.api.FormInfo;
import ru.it_spectrum.ai.playwright.mcp.api.GridInfo;
import ru.it_spectrum.ai.playwright.mcp.api.GridSnapshot;
import ru.it_spectrum.ai.playwright.mcp.api.LocatorActionResult;
import ru.it_spectrum.ai.playwright.mcp.api.LocatorSpec;
import ru.it_spectrum.ai.playwright.mcp.api.LocatorWaitResult;
import ru.it_spectrum.ai.playwright.mcp.api.PageScreenshotResult;
import ru.it_spectrum.ai.playwright.mcp.api.PageSnapshotResult;
import ru.it_spectrum.ai.playwright.mcp.api.PageNavigationResult;
import ru.it_spectrum.ai.playwright.mcp.config.PlaywrightMcpProperties;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
    /**
     * Per-element metadata reads must stay bounded. Locator.evaluate auto-waits for the element to be
     * attached using the page default timeout (tens of seconds); on a virtualized grid (ag-Grid) the
     * DOM is full of detached/hidden control elements, so an unbounded wait there stalls for minutes.
     */
    private static final int METADATA_TIMEOUT_MS = 1500;
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
    private static final DateTimeFormatter SCREENSHOT_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS").withZone(ZoneOffset.UTC);
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

    public PageNavigationResult navigate(String url, String waitUntil, Integer timeoutMs) {
        return dispatcher.call(() -> {
            ManagedPage managed = page(null, null);
            Page.NavigateOptions options = new Page.NavigateOptions()
                    .setWaitUntil(waitUntil(waitUntil));
            setTimeout(options, timeoutMs);
            Response response = managed.page().navigate(required(url, "url"), options);
            return navigationResult(managed, response);
        });
    }

    public PageSnapshotResult pageSnapshot(LocatorSpec locator, Boolean includeControls,
                                           Boolean includeTooltips, Boolean includeGrids,
                                           Integer maxControls, Integer maxRows, Integer timeoutMs) {
        return dispatcher.call(() -> {
            ManagedPage managed = page(null, null);
            LocatorSpec actualLocator = locator != null
                    ? locator
                    : LocatorSpec.css(properties.snapshot().defaultRootSelector());
            Locator root = locators.resolve(managed.page(), actualLocator);
            Locator.AriaSnapshotOptions options = new Locator.AriaSnapshotOptions();
            if (timeoutMs != null && timeoutMs > 0) {
                options.setTimeout(timeoutMs);
            }
            String snapshot = collapseGrids(root.ariaSnapshot(options));
            ControlSnapshot controls = Boolean.TRUE.equals(includeControls)
                    ? collectControlSnapshot(managed.page(), root, maxControls,
                            Boolean.TRUE.equals(includeTooltips), timeoutMs)
                    : null;
            GridSnapshot grids = Boolean.TRUE.equals(includeGrids)
                    ? collectGridSnapshot(root, maxRows, timeoutMs)
                    : null;
            return new PageSnapshotResult(managed.page().url(), safeTitle(managed.page()),
                    actualLocator, snapshot, controls, grids);
        });
    }

    public PageScreenshotResult pageScreenshot(Boolean fullPage, String name, Integer timeoutMs) {
        return dispatcher.call(() -> {
            ManagedPage managed = page(null, null);
            Path root = properties.resolvedDataDir();
            Path directory = root.resolve("screenshots");
            Files.createDirectories(directory);
            Path path = uniqueScreenshotPath(directory.resolve(screenshotFileName(name))).toAbsolutePath().normalize();

            Page.ScreenshotOptions options = new Page.ScreenshotOptions()
                    .setPath(path)
                    .setType(ScreenshotType.PNG)
                    .setFullPage(Boolean.TRUE.equals(fullPage));
            if (timeoutMs != null && timeoutMs > 0) {
                options.setTimeout(timeoutMs);
            }
            managed.page().screenshot(options);

            return new PageScreenshotResult(
                    managed.page().url(),
                    safeTitle(managed.page()),
                    path.toString(),
                    root.relativize(path).toString(),
                    Boolean.TRUE.equals(fullPage),
                    Files.size(path));
        });
    }

    /**
     * Replace the verbose body of every grid/treegrid node in a Playwright aria snapshot with a one-line
     * summary that lists column headers and points to {@code pageSnapshot} with {@code includeGrids=true}
     * for the rows. Keeps the structural snapshot focused on page layout and avoids duplicating (and
     * bloating) the data available through the {@code includeGrids} section.
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
                    ? "[rows collapsed - call pageSnapshot with includeGrids=true to read columns and rows]"
                    : "[" + columns.size() + " column(s): " + String.join(", ", columns)
                            + " - rows collapsed, call pageSnapshot with includeGrids=true to read rows]";
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

    private ControlSnapshot collectControlSnapshot(Page page, Locator root, Integer maxControls,
                                                   boolean includeTooltips, Integer timeoutMs) {
        Locator forms = root.locator("form");
        int formCount = safeCount(forms);
        List<FormInfo> formInfos = collectForms(forms, Math.min(formCount, MAX_FORMS));

        Locator controls = root.locator(FORM_CONTROL_SELECTOR);
        int controlCount = safeCount(controls);
        int limit = Math.min(controlCount, normalizeMaxControls(maxControls));
        List<FormControlInfo> controlInfos = collectControls(page, controls, limit, includeTooltips, timeoutMs);

        return new ControlSnapshot(maxControls, formCount, controlCount, formInfos, controlInfos);
    }

    public LocatorActionResult click(LocatorSpec locator, Integer timeoutMs, Boolean force) {
        return locatorAction(locator, "click", resolved -> {
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

    public LocatorActionResult fill(LocatorSpec locator, String value, Integer timeoutMs) {
        return locatorAction(locator, "fill", resolved -> {
            Locator.FillOptions options = new Locator.FillOptions();
            if (timeoutMs != null && timeoutMs > 0) {
                options.setTimeout(timeoutMs);
            }
            resolved.fill(required(value, "value"), options);
        });
    }

    public LocatorActionResult press(LocatorSpec locator, String key, Integer timeoutMs) {
        return locatorAction(locator, "press", resolved -> {
            Locator.PressOptions options = new Locator.PressOptions();
            if (timeoutMs != null && timeoutMs > 0) {
                options.setTimeout(timeoutMs);
            }
            resolved.press(required(key, "key"), options);
        });
    }

    public LocatorActionResult check(LocatorSpec locator, Boolean checked, Integer timeoutMs) {
        return locatorAction(locator, Boolean.FALSE.equals(checked) ? "uncheck" : "check", resolved -> {
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

    private LocatorActionResult locatorAction(LocatorSpec locator, String action, LocatorOperation operation) {
        return dispatcher.call(() -> {
            ManagedPage managed = page(null, null);
            LocatorSpec actualLocator = locator != null ? locator : LocatorSpec.css(properties.snapshot().defaultRootSelector());
            Locator resolved = locators.resolve(managed.page(), actualLocator);
            operation.apply(resolved);
            return new LocatorActionResult(action, actualLocator,
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

    private PageNavigationResult navigationResult(ManagedPage managed, Response response) {
        return new PageNavigationResult(
                managed.page().url(),
                safeTitle(managed.page()),
                response == null ? null : response.status(),
                response == null ? null : response.statusText(),
                response == null ? null : response.ok());
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

    private void setTimeout(Page.NavigateOptions options, Integer timeoutMs) {
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

    private Path uniqueScreenshotPath(Path path) {
        if (!Files.exists(path)) {
            return path;
        }
        String fileName = path.getFileName().toString();
        String baseName = fileName.endsWith(".png") ? fileName.substring(0, fileName.length() - 4) : fileName;
        for (int i = 2; i < 1_000; i++) {
            Path candidate = path.resolveSibling(baseName + "-" + i + ".png");
            if (!Files.exists(candidate)) {
                return candidate;
            }
        }
        return path.resolveSibling(baseName + "-" + System.nanoTime() + ".png");
    }

    private String screenshotFileName(String name) {
        String timestamp = SCREENSHOT_TIMESTAMP.format(Instant.now());
        String slug = screenshotSlug(name);
        return slug == null ? timestamp + ".png" : timestamp + "-" + slug + ".png";
    }

    private String screenshotSlug(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        StringBuilder out = new StringBuilder();
        boolean previousDash = false;
        for (int i = 0; i < value.length() && out.length() < 80; i++) {
            char ch = Character.toLowerCase(value.charAt(i));
            boolean alphanumeric = (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9');
            if (alphanumeric) {
                out.append(ch);
                previousDash = false;
            } else if (!previousDash && !out.isEmpty()) {
                out.append('-');
                previousDash = true;
            }
        }
        while (!out.isEmpty() && out.charAt(out.length() - 1) == '-') {
            out.setLength(out.length() - 1);
        }
        return out.isEmpty() ? null : out.toString();
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
            Boolean visible = safeVisible(control, timeoutMs);
            if (!Boolean.TRUE.equals(visible)) {
                // Hidden or detached control (common filler in virtualized grids): skip the metadata
                // read and tooltip hover, which would otherwise stall on attach waits for no value.
                result.add(hiddenControl(i, visible));
                continue;
            }
            Map<String, Object> metadata = safeMetadata(control, CONTROL_METADATA_SCRIPT);
            String accessibleName = stringValue(metadata, "accessibleName");
            String tooltip = firstNonBlank(stringValue(metadata, "title"), stringValue(metadata, "describedBy"));
            if (tooltip == null && includeTooltips && tooltipProbes < MAX_TOOLTIP_PROBES
                    && needsTooltipProbe(accessibleName)) {
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

    /** Minimal record for a hidden/detached control we deliberately did not inspect further. */
    private FormControlInfo hiddenControl(int index, Boolean visible) {
        return new FormControlInfo(index, null, null, null, null, null, null, null, null, null,
                null, visible, null, null, null);
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

    public LocatorWaitResult waitForLocator(LocatorSpec locator, String stateName, Integer timeoutMs) {
        return dispatcher.call(() -> {
            ManagedPage managed = page(null, null);
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
            return new LocatorWaitResult(locator, state.name().toLowerCase(Locale.ROOT),
                    found, safeCount(resolved), managed.page().url(), safeTitle(managed.page()));
        });
    }

    private GridSnapshot collectGridSnapshot(Locator root, Integer maxRows, Integer timeoutMs) {
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
        return new GridSnapshot(maxRows, gridCount, gridInfos);
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
            Object value = locator.evaluate(script, null,
                    new Locator.EvaluateOptions().setTimeout(METADATA_TIMEOUT_MS));
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
