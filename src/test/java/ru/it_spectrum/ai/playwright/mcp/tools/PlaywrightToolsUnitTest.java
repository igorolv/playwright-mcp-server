package ru.it_spectrum.ai.playwright.mcp.tools;

import org.junit.jupiter.api.Test;
import ru.it_spectrum.ai.playwright.mcp.api.LocatorActionResult;
import ru.it_spectrum.ai.playwright.mcp.api.LocatorSpec;
import ru.it_spectrum.ai.playwright.mcp.api.PageNavigationResult;
import ru.it_spectrum.ai.playwright.mcp.api.PageSnapshotResult;
import ru.it_spectrum.ai.playwright.mcp.playwright.PlaywrightSessionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlaywrightToolsUnitTest {

    @Test
    void pageAndLocatorToolsDelegateToSessionManager() {
        PlaywrightSessionManager sessions = mock(PlaywrightSessionManager.class);
        LocatorSpec button = new LocatorSpec("role", null, "button", "Save", true, null, null, null, null);
        PageNavigationResult navigation = new PageNavigationResult("http://example.test", "Example", 200, "OK", true);
        LocatorActionResult click = new LocatorActionResult("click", button, "http://example.test", "Example");
        LocatorSpec body = LocatorSpec.css("body");
        PageSnapshotResult snapshot = new PageSnapshotResult("http://example.test", "Example",
                body, "- heading \"Example\"", null, null);

        when(sessions.navigate("http://example.test", null, null)).thenReturn(navigation);
        when(sessions.pageSnapshot(body, true, null, null, 20, null, null)).thenReturn(snapshot);
        when(sessions.click(button, null, null)).thenReturn(click);

        PlaywrightPageTools pageTools = new PlaywrightPageTools(sessions);
        PlaywrightLocatorTools locatorTools = new PlaywrightLocatorTools(sessions);

        assertThat(pageTools.pageNavigate("http://example.test", null, null)).isSameAs(navigation);
        assertThat(pageTools.pageSnapshot(body, true, null, null, 20, null, null)).isSameAs(snapshot);
        assertThat(locatorTools.locatorClick(button, null, null)).isSameAs(click);

        verify(sessions).navigate("http://example.test", null, null);
        verify(sessions).pageSnapshot(body, true, null, null, 20, null, null);
        verify(sessions).click(button, null, null);
    }
}
