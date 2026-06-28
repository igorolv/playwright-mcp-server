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
            var snapshot = sessions.pageSnapshot(LocatorSpec.css("body"), null, null, null, null, null, null);

            assertThat(navigation.status()).isEqualTo(200);
            assertThat(navigation.title()).isEqualTo("Playwright MCP Test Site");
            assertThat(snapshot.snapshot()).contains("Playwright MCP Test Site", "Form page", "Say hello");
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

            var status = sessions.pageSnapshot(LocatorSpec.css("#status"), null, null, null, null, null, null);

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
            var item = sessions.pageSnapshot(LocatorSpec.css("#created"), null, null, null, null, null, null);

            assertThat(wait.found()).isTrue();
            assertThat(wait.count()).isEqualTo(1);
            assertThat(item.snapshot()).contains("Created item");
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
