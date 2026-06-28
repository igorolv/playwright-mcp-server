package ru.it_spectrum.ai.playwright.mcp.tools;

import org.junit.jupiter.api.Test;
import ru.it_spectrum.ai.playwright.mcp.api.BrowserInfo;
import ru.it_spectrum.ai.playwright.mcp.api.FormSnapshotResult;
import ru.it_spectrum.ai.playwright.mcp.api.LocatorActionResult;
import ru.it_spectrum.ai.playwright.mcp.api.LocatorSpec;
import ru.it_spectrum.ai.playwright.mcp.api.PageNavigationResult;
import ru.it_spectrum.ai.playwright.mcp.playwright.PlaywrightSessionManager;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlaywrightToolsUnitTest {

    @Test
    void lifecyclePageAndLocatorToolsDelegateToSessionManager() {
        PlaywrightSessionManager sessions = mock(PlaywrightSessionManager.class);
        LocatorSpec button = new LocatorSpec("role", null, "button", "Save", true, null, null, null, null);
        BrowserInfo browser = new BrowserInfo("default", "chromium", null, true, true, Instant.now());
        PageNavigationResult navigation = new PageNavigationResult("default", "http://example.test", "Example", 200, "OK", true);
        LocatorActionResult click = new LocatorActionResult("default", "click", button, "http://example.test", "Example");
        LocatorSpec body = LocatorSpec.css("body");
        FormSnapshotResult forms = new FormSnapshotResult("default", "http://example.test", "Example",
                body, 20, 0, 0, List.of(), List.of());

        when(sessions.launchBrowser(null, null, null, null, null, null)).thenReturn(browser);
        when(sessions.navigate(null, "http://example.test", null, null)).thenReturn(navigation);
        when(sessions.formSnapshot(null, body, 20, null, null)).thenReturn(forms);
        when(sessions.click(null, button, null, null)).thenReturn(click);

        PlaywrightLifecycleTools lifecycleTools = new PlaywrightLifecycleTools(sessions);
        PlaywrightPageTools pageTools = new PlaywrightPageTools(sessions);
        PlaywrightLocatorTools locatorTools = new PlaywrightLocatorTools(sessions);

        assertThat(lifecycleTools.launchBrowser(null, null, null, null, null, null)).isSameAs(browser);
        assertThat(pageTools.pageNavigate(null, "http://example.test", null, null)).isSameAs(navigation);
        assertThat(pageTools.pageFormSnapshot(null, body, 20, null, null)).isSameAs(forms);
        assertThat(locatorTools.locatorClick(null, button, null, null)).isSameAs(click);

        verify(sessions).launchBrowser(null, null, null, null, null, null);
        verify(sessions).navigate(null, "http://example.test", null, null);
        verify(sessions).formSnapshot(null, body, 20, null, null);
        verify(sessions).click(null, button, null, null);
    }
}
