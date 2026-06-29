package ru.it_spectrum.ai.playwright.mcp.live;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import ru.it_spectrum.ai.playwright.mcp.api.ControlSnapshot;
import ru.it_spectrum.ai.playwright.mcp.api.FormControlInfo;
import ru.it_spectrum.ai.playwright.mcp.api.FormInfo;
import ru.it_spectrum.ai.playwright.mcp.api.GridInfo;
import ru.it_spectrum.ai.playwright.mcp.api.GridSnapshot;
import ru.it_spectrum.ai.playwright.mcp.api.LocatorActionResult;
import ru.it_spectrum.ai.playwright.mcp.api.LocatorSpec;
import ru.it_spectrum.ai.playwright.mcp.api.PageSnapshotResult;
import ru.it_spectrum.ai.playwright.mcp.config.PlaywrightMcpProperties;
import ru.it_spectrum.ai.playwright.mcp.playwright.PlaywrightDispatcher;
import ru.it_spectrum.ai.playwright.mcp.playwright.PlaywrightSessionManager;
import ru.it_spectrum.ai.playwright.mcp.testsupport.BrowserExecutableSupport;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Manual live smoke/exploration test for a web application.
 *
 * <p>Run with: {@code ./gradlew liveApplicationTest}
 *
 * <p>Required environment variables:
 * <ul>
 *   <li>{@code LIVE_APPLICATION_URL}</li>
 *   <li>{@code LIVE_APPLICATION_USERNAME}</li>
 *   <li>{@code LIVE_APPLICATION_PASSWORD}</li>
 * </ul>
 *
 * <p>Optional environment variables:
 * <ul>
 *   <li>{@code LIVE_APPLICATION_HEADLESS} - defaults to {@code true}</li>
 *   <li>{@code LIVE_APPLICATION_MAX_MENU_ITEMS} - defaults to {@code 30}</li>
 * </ul>
 */
@Tag("live-application")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LiveApplicationExplorationTest {

    private static final int ACTION_TIMEOUT_MS = 10_000;
    private static final int PROBE_TIMEOUT_MS = 2_000;
    private static final int SNAPSHOT_TIMEOUT_MS = 15_000;
    /** Short best-effort wait for async content (grid rows) after a click; grid pages return early. */
    private static final int CONTENT_SETTLE_TIMEOUT_MS = 3_000;
    /** Deadline for the Angular shell to bootstrap and render the menu after the Keycloak redirect. */
    private static final int APP_READY_TIMEOUT_MS = 20_000;
    private static final Pattern ACTIONABLE_LINE =
            Pattern.compile("^\\s*-\\s+(link|menuitem|button)\\s+\"([^\"]{1,160})\".*$");

    @TempDir
    Path tempDir;

    private PlaywrightDispatcher dispatcher;
    private PlaywrightSessionManager sessions;
    private String baseUrl;
    private String username;
    private String password;

    @BeforeEach
    void setUp() {
        baseUrl = env("LIVE_APPLICATION_URL");
        username = env("LIVE_APPLICATION_USERNAME");
        password = env("LIVE_APPLICATION_PASSWORD");

        Assumptions.assumeTrue(hasText(baseUrl) && hasText(username) && hasText(password),
                "LIVE_APPLICATION_URL / LIVE_APPLICATION_USERNAME / LIVE_APPLICATION_PASSWORD are not set; "
                        + "skipping live application test");

        String executablePath = BrowserExecutableSupport.chromiumExecutableOrNull();
        Assumptions.assumeTrue(executablePath != null,
                "No Chromium executable found. Run .\\gradlew.bat installPlaywrightBrowsers "
                        + "or set PLAYWRIGHT_MCP_EXECUTABLE.");

        dispatcher = new PlaywrightDispatcher();
        sessions = new PlaywrightSessionManager(properties(executablePath), dispatcher);
    }

    @AfterEach
    void tearDown() {
        if (sessions != null) {
            sessions.shutdown();
        }
        if (dispatcher != null) {
            dispatcher.shutdown();
        }
    }

    @Test
    void logsInAndExploresVisibleMenuEntries() throws Exception {
        sessions.navigate(baseUrl, "domcontentloaded", 60_000);
        PageSnapshotResult loginSnapshot = snapshotBody();

        LocatorSpec passwordLocator = fillLoginForm(loginSnapshot.snapshot());
        submitLogin(passwordLocator);

        MenuSnapshot initialMenu = waitForInitialMenu();
        assertThat(initialMenu.candidates())
                .as("No menu candidates found after login. Snapshot:\n%s", excerpt(initialMenu.snapshot(), 80))
                .isNotEmpty();

        int maxMenuItems = positiveIntEnv("LIVE_APPLICATION_MAX_MENU_ITEMS", 30);
        ArrayDeque<MenuCandidate> queue = new ArrayDeque<>();
        Set<String> enqueued = new LinkedHashSet<>();
        for (MenuCandidate candidate : initialMenu.candidates()) {
            enqueue(queue, enqueued, candidate);
        }

        List<PageVisit> visits = new ArrayList<>();
        while (!queue.isEmpty() && visits.size() < maxMenuItems) {
            MenuCandidate candidate = queue.removeFirst();
            PageVisit visit = openAndDescribe(candidate);
            visits.add(visit);

            if (visit.error() == null) {
                for (MenuCandidate discovered : menuSnapshot().candidates()) {
                    enqueue(queue, enqueued, discovered);
                }
            }
        }

        long successfulVisits = visits.stream().filter(visit -> visit.error() == null).count();
        assertThat(successfulVisits)
                .as("No menu entry was opened successfully. Reported attempts: %s", visits.size())
                .isGreaterThan(0);

        Path report = writeReport(visits, initialMenu);
        System.out.println("Live application report: " + report.toAbsolutePath());
    }

    private LocatorSpec fillLoginForm(String loginSnapshot) {
        LocatorSpec usernameLocator = fillFirst(username, usernameLocators());
        assertThat(usernameLocator)
                .as("Username field was not found. Login page snapshot:\n%s", excerpt(loginSnapshot, 80))
                .isNotNull();

        LocatorSpec passwordLocator = fillFirst(password, passwordLocators());
        assertThat(passwordLocator)
                .as("Password field was not found. Login page snapshot:\n%s", excerpt(loginSnapshot, 80))
                .isNotNull();

        return passwordLocator;
    }

    private void submitLogin(LocatorSpec passwordLocator) {
        if (clickFirst(loginButtonLocators()) != null) {
            return;
        }
        sessions.press(passwordLocator, "Enter", ACTION_TIMEOUT_MS);
    }

    private LocatorSpec fillFirst(String value, List<LocatorSpec> locators) {
        for (LocatorSpec locator : locators) {
            try {
                sessions.fill(locator, value, PROBE_TIMEOUT_MS);
                return locator;
            } catch (RuntimeException ignored) {
            }
        }
        return null;
    }

    private LocatorSpec clickFirst(List<LocatorSpec> locators) {
        for (LocatorSpec locator : locators) {
            try {
                sessions.click(locator, PROBE_TIMEOUT_MS, null);
                return locator;
            } catch (RuntimeException ignored) {
            }
        }
        return null;
    }

    private PageVisit openAndDescribe(MenuCandidate candidate) {
        RuntimeException lastError = null;
        for (LocatorSpec locator : clickLocators(candidate)) {
            try {
                LocatorActionResult click = sessions.click(locator, ACTION_TIMEOUT_MS, null);
                waitForContentSettle();
                PageSnapshotResult snapshot = describeSnapshot();
                ControlSnapshot controls = snapshot.controls();
                GridSnapshot grids = snapshot.grids();
                return new PageVisit(candidate, click.url(), click.title(),
                        describe(snapshot.snapshot(), controls, grids), formInventory(controls),
                        gridInventory(grids), null);
            } catch (RuntimeException e) {
                lastError = e;
            }
        }
        String error = lastError == null ? "unknown click failure" : firstLine(lastError.getMessage());
        return new PageVisit(candidate, null, null, null, null, null, error);
    }

    /**
     * After a SPA menu click, networkidle never settles, so wait on visible content instead: grid rows.
     * On a grid page the header row appears almost immediately and this returns early; on a grid-less
     * page it costs the (small) ceiling. Grid data is re-read later in pageSnapshot regardless.
     */
    private void waitForContentSettle() {
        waitVisibleQuietly(role("row", null, false), CONTENT_SETTLE_TIMEOUT_MS);
    }

    private void waitVisibleQuietly(LocatorSpec locator, int timeoutMs) {
        try {
            sessions.waitForLocator(locator, "visible", timeoutMs);
        } catch (RuntimeException ignored) {
        }
    }

    /**
     * Poll the menu until candidates appear or the readiness deadline passes. Replaces a blind
     * post-login wait: the Angular shell renders the nav asynchronously after the Keycloak redirect,
     * so we keep looking (as an explorer would) instead of guessing a fixed delay. Returns fast once
     * the menu is up; the per-attempt cost is just the menuRoots probe timeouts.
     */
    private MenuSnapshot waitForInitialMenu() {
        long deadline = System.currentTimeMillis() + APP_READY_TIMEOUT_MS;
        MenuSnapshot menu = menuSnapshot();
        while (menu.candidates().isEmpty() && System.currentTimeMillis() < deadline) {
            menu = menuSnapshot();
        }
        return menu;
    }

    private MenuSnapshot menuSnapshot() {
        RuntimeException lastError = null;
        for (LocatorSpec root : menuRoots()) {
            try {
                PageSnapshotResult snapshot = sessions.pageSnapshot(root, null, null, null, null, null, null,
                        PROBE_TIMEOUT_MS);
                List<MenuCandidate> candidates = extractMenuCandidates(snapshot.snapshot());
                if (!candidates.isEmpty()) {
                    return new MenuSnapshot(root, snapshot.snapshot(), candidates);
                }
            } catch (RuntimeException e) {
                lastError = e;
            }
        }
        try {
            PageSnapshotResult body = snapshotBody();
            return new MenuSnapshot(css("body"), body.snapshot(), extractMenuCandidates(body.snapshot()));
        } catch (RuntimeException e) {
            if (lastError != null) {
                throw lastError;
            }
            throw e;
        }
    }

    private List<MenuCandidate> extractMenuCandidates(String snapshot) {
        Map<String, MenuCandidate> candidates = new LinkedHashMap<>();
        for (String line : snapshot.lines().toList()) {
            Matcher matcher = ACTIONABLE_LINE.matcher(line);
            if (!matcher.matches()) {
                continue;
            }
            String role = matcher.group(1);
            String name = cleanName(matcher.group(2));
            if (!hasText(name) || shouldSkipMenuName(name)) {
                continue;
            }
            MenuCandidate candidate = new MenuCandidate(role, name);
            candidates.putIfAbsent(candidate.key(), candidate);
        }
        return List.copyOf(candidates.values());
    }

    private void enqueue(ArrayDeque<MenuCandidate> queue, Set<String> enqueued, MenuCandidate candidate) {
        if (enqueued.add(candidate.key())) {
            queue.add(candidate);
        }
    }

    // The app authenticates through a standard Keycloak login page (realm asv), so target its known
    // field ids first and keep only a couple of generic fallbacks. This mimics an LLM that, seeing the
    // login form, tries the obvious candidates instead of a long speculative sweep.
    private List<LocatorSpec> usernameLocators() {
        return List.of(
                css("#username"),
                css("input[name='username']"),
                css("input[type='text']", 0)
        );
    }

    private List<LocatorSpec> passwordLocators() {
        return List.of(
                css("#password"),
                css("input[name='password']"),
                css("input[type='password']")
        );
    }

    private List<LocatorSpec> loginButtonLocators() {
        return List.of(
                css("#kc-login"),
                css("button[type='submit']"),
                css("input[type='submit']")
        );
    }

    // The SSJ toolbar icon buttons are anonymous in the accessibility tree (names live only in their
    // matTooltip), so they never become menu candidates - which means scanning the whole body already
    // yields just the real menu entries. We tried scoping to a nav container (mat-sidenav, role=navigation,
    // ...) but none matched this app, and each miss cost a probe timeout, so body is both correct and fast.
    private List<LocatorSpec> menuRoots() {
        return List.of(
                css("body")
        );
    }

    private List<LocatorSpec> clickLocators(MenuCandidate candidate) {
        return List.of(
                role(candidate.role(), candidate.name(), true),
                role(candidate.role(), candidate.name(), false),
                text(candidate.name(), false)
        );
    }

    private PageSnapshotResult snapshotBody() {
        return sessions.pageSnapshot(css("body"), null, null, null, null, null, null, SNAPSHOT_TIMEOUT_MS);
    }

    /** Full inspection of the current page: structure plus control states (with tooltips) and grid rows. */
    private PageSnapshotResult describeSnapshot() {
        return sessions.pageSnapshot(css("body"), true, true, true, null, 120, 25, PROBE_TIMEOUT_MS);
    }

    private String describe(String snapshot, ControlSnapshot forms, GridSnapshot grids) {
        List<String> headings = roleNames(snapshot, "heading", 6);
        List<String> links = roleNames(snapshot, "link", 6);
        List<String> buttons = buttonLabels(forms, 10);
        List<String> textboxes = controlsByKind(forms, "textbox", 10);
        List<String> searchboxes = controlsByKind(forms, "searchbox", 10);
        List<String> comboboxes = controlsByKind(forms, "combobox", 10);
        List<String> checkboxes = controlsByKind(forms, "checkbox", 10);
        List<String> radios = controlsByKind(forms, "radio", 10);
        List<String> switches = controlsByKind(forms, "switch", 10);
        // Count plain HTML tables and ARIA grids/treegrids (ag-Grid, Material tables) together.
        long tableCount = countRole(snapshot, "table") + countRole(snapshot, "grid") + countRole(snapshot, "treegrid");

        StringBuilder description = new StringBuilder();
        if (!headings.isEmpty()) {
            description.append("Headings: ").append(String.join(", ", headings)).append(". ");
        }
        description.append("Visible structure: ")
                .append(tableCount).append(" table/grid(s), ")
                .append(forms.formCount()).append(" HTML form(s), ")
                .append(forms.controlCount()).append(" form/control element(s). ");
        for (GridInfo grid : grids.grids()) {
            description.append("Grid #").append(grid.index()).append(" (").append(grid.role()).append("): ")
                    .append(grid.renderedRowCount()).append(grid.rowsTruncated() ? "+" : "").append(" row(s)");
            if (!grid.columns().isEmpty()) {
                description.append(", columns: ").append(String.join(", ", grid.columns()));
            }
            description.append(". ");
        }
        appendControls(description, "Text fields", textboxes);
        appendControls(description, "Search fields", searchboxes);
        appendControls(description, "Comboboxes", comboboxes);
        appendControls(description, "Checkboxes", checkboxes);
        appendControls(description, "Radio buttons", radios);
        appendControls(description, "Switches", switches);
        if (!links.isEmpty()) {
            description.append("Links include: ").append(String.join(", ", links)).append(". ");
        }
        if (!buttons.isEmpty()) {
            description.append("Buttons include: ").append(String.join(", ", buttons)).append(".");
        }
        return description.toString().trim();
    }

    /** Buttons described by their accessible name, falling back to the hover tooltip for icon-only buttons. */
    private List<String> buttonLabels(ControlSnapshot forms, int limit) {
        return forms.controls().stream()
                .filter(control -> "button".equals(controlKind(control)))
                .filter(control -> Boolean.TRUE.equals(control.visible()))
                .map(this::buttonLabel)
                .filter(this::hasText)
                .distinct()
                .limit(limit)
                .toList();
    }

    private String buttonLabel(FormControlInfo control) {
        String name = control.accessibleName();
        String tooltip = control.tooltip();
        boolean iconOnly = !hasText(name) || name.matches("^[a-z][a-z0-9_]*$");
        if (iconOnly && hasText(tooltip)) {
            return hasText(name) ? tooltip + " [" + name + "]" : tooltip;
        }
        if (hasText(name) && hasText(tooltip) && !tooltip.equals(name)) {
            return name + " (" + tooltip + ")";
        }
        return hasText(name) ? name : tooltip;
    }

    private String gridInventory(GridSnapshot grids) {
        if (grids.gridCount() == 0) {
            return "No tables or grids detected.";
        }
        StringBuilder inventory = new StringBuilder();
        inventory.append("Grids detected: ").append(grids.gridCount()).append('\n');
        for (GridInfo grid : grids.grids()) {
            inventory.append("\n#").append(grid.index()).append(' ').append(grid.role())
                    .append(" columns=").append(grid.columnCount())
                    .append(" rows=").append(grid.renderedRowCount()).append(grid.rowsTruncated() ? "+" : "")
                    .append('\n');
            if (!grid.columns().isEmpty()) {
                inventory.append("| ").append(String.join(" | ", grid.columns())).append(" |\n");
            }
            for (List<String> row : grid.rows()) {
                inventory.append("| ").append(String.join(" | ", row)).append(" |\n");
            }
        }
        return inventory.toString().trim();
    }

    private List<String> controlsByKind(ControlSnapshot forms, String kind, int limit) {
        return forms.controls().stream()
                .filter(control -> kind.equals(controlKind(control)))
                .filter(control -> Boolean.TRUE.equals(control.visible()))
                .map(this::controlDisplayName)
                .filter(this::hasText)
                .distinct()
                .limit(limit)
                .toList();
    }

    private String formInventory(ControlSnapshot forms) {
        StringBuilder inventory = new StringBuilder();
        inventory.append("Forms detected: ").append(forms.formCount())
                .append("; controls detected: ").append(forms.controlCount()).append('\n');
        if (!forms.forms().isEmpty()) {
            inventory.append("\nForms:\n");
            for (FormInfo form : forms.forms()) {
                inventory.append("- #").append(form.index()).append(' ')
                        .append(firstNonBlank(form.label(), form.name(), form.id(), "(unnamed form)"))
                        .append(" controls=").append(form.controlCount());
                if (hasText(form.method())) {
                    inventory.append(" method=").append(form.method());
                }
                if (hasText(form.action())) {
                    inventory.append(" action=").append(form.action());
                }
                inventory.append('\n');
            }
        }
        if (!forms.controls().isEmpty()) {
            inventory.append("\nControls:\n");
            for (FormControlInfo control : forms.controls()) {
                inventory.append("- #").append(control.index()).append(' ')
                        .append(controlKind(control)).append(' ')
                        .append('"').append(controlDisplayName(control)).append('"')
                        .append(" tag=").append(nullToEmpty(control.tagName()));
                if (hasText(control.type())) {
                    inventory.append(" type=").append(control.type());
                }
                if (control.visible() != null) {
                    inventory.append(" visible=").append(control.visible());
                }
                if (control.enabled() != null) {
                    inventory.append(" enabled=").append(control.enabled());
                }
                if (control.checked() != null) {
                    inventory.append(" checked=").append(control.checked());
                }
                if (hasText(control.form())) {
                    inventory.append(" form=").append(control.form());
                }
                if (hasText(control.tooltip())) {
                    inventory.append(" tooltip=\"").append(control.tooltip()).append('"');
                }
                inventory.append('\n');
            }
        }
        return inventory.toString().trim();
    }

    private String controlKind(FormControlInfo control) {
        String role = lower(control.role());
        String tag = lower(control.tagName());
        String type = lower(control.type());
        if (hasText(role)) {
            return switch (role) {
                case "button", "textbox", "combobox", "checkbox", "radio", "switch", "searchbox" -> role;
                default -> role;
            };
        }
        if ("select".equals(tag)) {
            return "combobox";
        }
        if ("textarea".equals(tag)) {
            return "textbox";
        }
        if ("button".equals(tag)) {
            return "button";
        }
        if ("input".equals(tag)) {
            return switch (type) {
                case "button", "submit", "reset" -> "button";
                case "checkbox" -> "checkbox";
                case "radio" -> "radio";
                case "search" -> "searchbox";
                default -> "textbox";
            };
        }
        return hasText(tag) ? tag : "control";
    }

    private String controlDisplayName(FormControlInfo control) {
        return firstNonBlank(
                control.accessibleName(),
                control.label(),
                control.placeholder(),
                control.text(),
                control.name(),
                control.id(),
                "(unnamed)");
    }

    private void appendControls(StringBuilder description, String label, List<String> names) {
        if (!names.isEmpty()) {
            description.append(label).append(": ").append(String.join(", ", names)).append(". ");
        }
    }

    private List<String> roleNames(String snapshot, String role, int limit) {
        Pattern pattern = Pattern.compile("^\\s*-\\s+" + Pattern.quote(role) + "\\s+\"([^\"]{1,160})\".*$");
        return snapshot.lines()
                .map(pattern::matcher)
                .filter(Matcher::matches)
                .map(matcher -> cleanName(matcher.group(1)))
                .filter(this::hasText)
                .distinct()
                .limit(limit)
                .toList();
    }

    private long countRole(String snapshot, String role) {
        Pattern pattern = Pattern.compile("^\\s*-\\s+" + Pattern.quote(role) + "(\\s|:|$).*$");
        return snapshot.lines().map(pattern::matcher).filter(Matcher::matches).count();
    }

    private Path writeReport(List<PageVisit> visits, MenuSnapshot initialMenu) throws IOException {
        Path report = Path.of("build", "reports", "live-application", "report.md");
        Files.createDirectories(report.getParent());
        Files.writeString(report, reportContent(visits, initialMenu), StandardCharsets.UTF_8);
        return report;
    }

    private String reportContent(List<PageVisit> visits, MenuSnapshot initialMenu) {
        StringBuilder report = new StringBuilder();
        report.append("# Live application exploration\n\n");
        report.append("- Base URL: ").append(baseUrl).append('\n');
        report.append("- Initial menu root: ").append(initialMenu.root()).append('\n');
        report.append("- Attempts: ").append(visits.size()).append('\n');
        report.append("- Successful opens: ")
                .append(visits.stream().filter(visit -> visit.error() == null).count())
                .append("\n\n");

        for (int i = 0; i < visits.size(); i++) {
            PageVisit visit = visits.get(i);
            report.append("## ").append(i + 1).append(". ")
                    .append(visit.candidate().name()).append("\n\n");
            report.append("- Role: ").append(visit.candidate().role()).append('\n');
            if (visit.error() != null) {
                report.append("- Error: ").append(visit.error()).append("\n\n");
                continue;
            }
            report.append("- URL: ").append(visit.url()).append('\n');
            report.append("- Title: ").append(nullToEmpty(visit.title())).append('\n');
            report.append("- Description: ").append(visit.description()).append("\n\n");
            report.append("### Form inventory\n\n");
            report.append("```text\n").append(visit.formInventory()).append("\n```\n\n");
            report.append("### Grid inventory\n\n");
            report.append("```text\n").append(visit.gridInventory()).append("\n```\n\n");
        }
        return report.toString();
    }

    private PlaywrightMcpProperties properties(String executablePath) {
        boolean headless = booleanEnv("LIVE_APPLICATION_HEADLESS", true);
        return new PlaywrightMcpProperties(
                tempDir.toString(),
                new PlaywrightMcpProperties.Browser("chromium", null, executablePath, headless, 0),
                new PlaywrightMcpProperties.Context(1440, 1000, null, "ru-RU", "Europe/Moscow", false),
                new PlaywrightMcpProperties.Sessions("live", "live", "live", 1, 1, 2, 45_000),
                new PlaywrightMcpProperties.Snapshot("body"));
    }

    private LocatorSpec role(String role, String name, Boolean exact) {
        return new LocatorSpec("role", null, role, name, hasText(name) ? exact : null, null, null, true, null);
    }

    private LocatorSpec label(String value) {
        return new LocatorSpec("label", value, null, null, false, null, null, true, null);
    }

    private LocatorSpec placeholder(String value) {
        return new LocatorSpec("placeholder", value, null, null, false, null, null, true, null);
    }

    private LocatorSpec text(String value, Boolean exact) {
        return new LocatorSpec("text", value, null, null, exact, null, null, true, null);
    }

    private LocatorSpec css(String selector) {
        return new LocatorSpec("css", selector, null, null, null, null, null, true, null);
    }

    private LocatorSpec css(String selector, Integer nth) {
        return new LocatorSpec("css", selector, null, null, null, null, nth, true, null);
    }

    private boolean shouldSkipMenuName(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        return normalized.contains("logout")
                || normalized.contains("log out")
                || normalized.contains("sign out")
                || normalized.contains("delete")
                || normalized.contains("remove")
                || normalized.contains("submit")
                || normalized.contains("save")
                || normalized.contains("open filter menu")
                || normalized.contains("filter menu")
                || normalized.contains("выйти")
                || normalized.contains("выход")
                || normalized.contains("удал")
                || normalized.contains("сохран")
                || normalized.contains("войти");
    }

    private String excerpt(String text, int maxLines) {
        return text.lines().limit(maxLines).collect(Collectors.joining("\n"));
    }

    private String firstLine(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.lines().findFirst().orElse(text);
    }

    private String cleanName(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value;
            }
        }
        return "";
    }

    private String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String env(String name) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean booleanEnv(String name, boolean defaultValue) {
        String value = env(name);
        return value == null ? defaultValue : Boolean.parseBoolean(value);
    }

    private int positiveIntEnv(String name, int defaultValue) {
        String value = env(name);
        if (value == null) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : defaultValue;
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private record MenuSnapshot(LocatorSpec root, String snapshot, List<MenuCandidate> candidates) {
    }

    private record MenuCandidate(String role, String name) {
        private String key() {
            return role + ":" + name.toLowerCase(Locale.ROOT);
        }
    }

    private record PageVisit(
            MenuCandidate candidate,
            String url,
            String title,
            String description,
            String formInventory,
            String gridInventory,
            String error
    ) {
    }
}
