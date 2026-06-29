package ru.it_spectrum.ai.playwright.mcp.integration;

import com.microsoft.playwright.PlaywrightException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opentest4j.TestAbortedException;
import ru.it_spectrum.ai.playwright.mcp.api.LocatorSpec;
import ru.it_spectrum.ai.playwright.mcp.playwright.PlaywrightDispatcher;
import ru.it_spectrum.ai.playwright.mcp.playwright.PlaywrightSessionManager;
import ru.it_spectrum.ai.playwright.mcp.testsupport.BrowserExecutableSupport;
import ru.it_spectrum.ai.playwright.mcp.testsupport.LocalTestSite;
import ru.it_spectrum.ai.playwright.mcp.testsupport.TestProperties;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class PlaywrightSessionManagerIntegrationTest {

    @TempDir
    Path tempDir;

    private LocalTestSite site;
    private PlaywrightDispatcher dispatcher;
    private PlaywrightSessionManager sessions;
    private String executablePath;

    @BeforeEach
    void setUp() {
        executablePath = BrowserExecutableSupport.chromiumExecutableOrNull();
        if (executablePath == null) {
            throw new TestAbortedException("No Chromium executable found. Run .\\gradlew.bat installPlaywrightBrowsers or set PLAYWRIGHT_MCP_EXECUTABLE.");
        }
        site = LocalTestSite.start();
        dispatcher = new PlaywrightDispatcher();
        sessions = new PlaywrightSessionManager(
                TestProperties.playwrightMcp(tempDir, executablePath),
                dispatcher);
    }

    @AfterEach
    void tearDown() {
        if (sessions != null) {
            sessions.shutdown();
        }
        if (dispatcher != null) {
            dispatcher.shutdown();
        }
        if (site != null) {
            site.close();
        }
    }

    @Test
    void navigatesAndReturnsAriaSnapshot() {
        try {
            var navigation = sessions.navigate(site.url("/index.html"), "domcontentloaded", null);
            var snapshot = sessions.pageSnapshot(LocatorSpec.css("body"), null, null, null, null, null, null, null);

            assertThat(navigation.status()).isEqualTo(200);
            assertThat(navigation.title()).isEqualTo("Playwright MCP Test Site");
            assertThat(snapshot.snapshot()).contains("Playwright MCP Test Site", "Form page", "Say hello");
        } catch (PlaywrightException e) {
            abortWhenBrowserIsMissing(e);
            throw e;
        }
    }

    @Test
    void capturesScreenshotToDataDir() throws Exception {
        try {
            sessions.navigate(site.url("/index.html"), "domcontentloaded", null);

            var screenshot = sessions.pageScreenshot(false, "home page", null);

            assertThat(screenshot.url()).isEqualTo(site.url("/index.html"));
            assertThat(screenshot.path()).endsWith(".png");
            assertThat(screenshot.relativePath()).startsWith("screenshots");
            assertThat(screenshot.sizeBytes()).isGreaterThan(0);
            assertThat(Files.isRegularFile(Path.of(screenshot.path()))).isTrue();
        } catch (PlaywrightException e) {
            abortWhenBrowserIsMissing(e);
            throw e;
        }
    }

    @Test
    void fillsClicksAndReadsStatusWithSnapshot() {
        try {
            sessions.navigate(site.url("/form.html"), "domcontentloaded", null);
            sessions.fill(new LocatorSpec("label", "Name", null, null, true, null, null, null, null), "Alice", null);
            sessions.click(new LocatorSpec("role", null, "button", "Save", true, null, null, null, null), null, null);

            var status = sessions.pageSnapshot(LocatorSpec.css("#status"), null, null, null, null, null, null, null);

            assertThat(status.snapshot()).contains("Saved Alice");
        } catch (PlaywrightException e) {
            abortWhenBrowserIsMissing(e);
            throw e;
        }
    }

    @Test
    void observesDynamicDomChangesWithWaitAndSnapshot() {
        try {
            sessions.navigate(site.url("/dynamic.html"), "domcontentloaded", null);
            sessions.click(new LocatorSpec("role", null, "button", "Add item", true, null, null, null, null), null, null);

            var wait = sessions.waitForLocator(LocatorSpec.css("#created"), "visible", null);
            var item = sessions.pageSnapshot(LocatorSpec.css("#created"), null, null, null, null, null, null, null);

            assertThat(wait.found()).isTrue();
            assertThat(wait.count()).isEqualTo(1);
            assertThat(item.snapshot()).contains("Created item");
        } catch (PlaywrightException e) {
            abortWhenBrowserIsMissing(e);
            throw e;
        }
    }

    @Test
    void waitsForFirstMatchWhenTextLocatorMatchesMultipleElements() {
        try {
            sessions.navigate(site.url("/tables.html"), "domcontentloaded", null);

            var wait = sessions.waitForLocator(
                    new LocatorSpec("text", "Duplicate value", null, null, null, null, null, null, null),
                    "visible",
                    1_000);

            assertThat(wait.found()).isTrue();
            assertThat(wait.count()).isEqualTo(2);
        } catch (PlaywrightException e) {
            abortWhenBrowserIsMissing(e);
            throw e;
        }
    }

    @Test
    void returnsEmptySnapshotWithoutWaitingWhenRootLocatorMatchesNothing() {
        try {
            sessions.navigate(site.url("/index.html"), "domcontentloaded", null);

            long start = System.nanoTime();
            var snapshot = sessions.pageSnapshot(
                    LocatorSpec.css("#no-such-element-anywhere"), null, null, null, null, null, null, 10_000);
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            assertThat(snapshot.matchedCount()).isZero();
            assertThat(snapshot.snapshot()).isEmpty();
            // matchedCount=0 is already self-explanatory via the schema; the diagnostic note is only for the
            // matched-but-empty case, so it stays null here.
            assertThat(snapshot.note()).isNull();
            // A non-matching root must fail fast (count() does not wait) rather than block the 10s timeout.
            assertThat(elapsedMs).isLessThan(5_000);
        } catch (PlaywrightException e) {
            abortWhenBrowserIsMissing(e);
            throw e;
        }
    }

    @Test
    void addsDiagnosticNoteWhenRootMatchesButHasNoAccessibilitySubtree() {
        try {
            sessions.navigate(site.url("/tables.html"), "domcontentloaded", null);

            // A role=presentation table resolves but yields an empty aria snapshot, mirroring ADF layout
            // tables in the real apps where matchedCount>0 but the snapshot string is blank.
            var snapshot = sessions.pageSnapshot(
                    LocatorSpec.css("#layout"), null, null, null, null, null, null, null);

            assertThat(snapshot.matchedCount()).isEqualTo(1);
            assertThat(snapshot.snapshot()).isBlank();
            assertThat(snapshot.note()).contains("no accessibility", "includeControls/includeGrids");
        } catch (PlaywrightException e) {
            abortWhenBrowserIsMissing(e);
            throw e;
        }
    }

    @Test
    void snapshotsFirstMatchAndReportsCountWhenRootMatchesMultipleElements() {
        try {
            sessions.navigate(site.url("/tables.html"), "domcontentloaded", null);

            var snapshot = sessions.pageSnapshot(
                    new LocatorSpec("text", "Duplicate value", null, null, null, null, null, null, null),
                    null, null, null, null, null, null, null);

            assertThat(snapshot.matchedCount()).isEqualTo(2);
            assertThat(snapshot.snapshot()).contains("Duplicate value");
        } catch (PlaywrightException e) {
            abortWhenBrowserIsMissing(e);
            throw e;
        }
    }

    @Test
    void readsScopedLocatorTextAndInputValueWithoutFullSnapshot() {
        try {
            sessions.navigate(site.url("/tables.html"), "domcontentloaded", null);

            var row = sessions.text(LocatorSpec.css("tr[data-payment='target']"), 200, null);
            var input = sessions.text(LocatorSpec.css("#creditor"), 200, null);

            assertThat(row.count()).isEqualTo(1);
            assertThat(row.text()).contains("18.12.2025", "б/н", "55 471,18");
            assertThat(row.truncated()).isFalse();
            assertThat(input.text()).isEqualTo("913690");
        } catch (PlaywrightException e) {
            abortWhenBrowserIsMissing(e);
            throw e;
        }
    }

    private void abortWhenBrowserIsMissing(PlaywrightException e) {
        String message = e.getMessage();
        if (message != null && (message.contains("Executable doesn't exist")
                || message.contains("Please run the following command")
                || message.contains("playwright install"))) {
            throw new TestAbortedException("Playwright browser is not installed. Run .\\gradlew.bat installPlaywrightBrowsers or set PLAYWRIGHT_MCP_EXECUTABLE.", e);
        }
    }
}
