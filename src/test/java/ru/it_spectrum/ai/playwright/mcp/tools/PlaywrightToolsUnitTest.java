package ru.it_spectrum.ai.playwright.mcp.tools;

import org.junit.jupiter.api.Test;
import ru.it_spectrum.ai.playwright.mcp.api.LocatorActionResult;
import ru.it_spectrum.ai.playwright.mcp.api.LocatorSpec;
import ru.it_spectrum.ai.playwright.mcp.api.LocatorTextResult;
import ru.it_spectrum.ai.playwright.mcp.api.PageNavigationResult;
import ru.it_spectrum.ai.playwright.mcp.api.PageScreenshotResult;
import ru.it_spectrum.ai.playwright.mcp.api.PageSnapshotResult;
import ru.it_spectrum.ai.playwright.mcp.api.SnapshotCollapseInfo;
import ru.it_spectrum.ai.playwright.mcp.playwright.PlaywrightSessionManager;

import java.util.List;

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
        LocatorTextResult text = new LocatorTextResult(button, "Save", false, 1, "http://example.test", "Example");
        PageScreenshotResult screenshot = new PageScreenshotResult("http://example.test", "Example",
                "C:\\tmp\\shot.png", "screenshots\\shot.png", false, 1024);
        LocatorSpec body = LocatorSpec.css("body");
        PageSnapshotResult snapshot = new PageSnapshotResult("http://example.test", "Example",
                body, 1, "- heading \"Example\"", null, null, new SnapshotCollapseInfo(true, 0, false, List.of()));

        when(sessions.navigate("http://example.test", null, null)).thenReturn(navigation);
        when(sessions.pageSnapshot(body, true, null, null, null, 20, null, null)).thenReturn(snapshot);
        when(sessions.pageScreenshot(false, "step", null)).thenReturn(screenshot);
        when(sessions.click(button, null, null)).thenReturn(click);
        when(sessions.text(button, 100, null)).thenReturn(text);

        PlaywrightPageTools pageTools = new PlaywrightPageTools(sessions);
        PlaywrightScreenshotTools screenshotTools = new PlaywrightScreenshotTools(sessions);
        PlaywrightLocatorTools locatorTools = new PlaywrightLocatorTools(sessions);

        assertThat(pageTools.pageNavigate("http://example.test", null, null)).isSameAs(navigation);
        assertThat(pageTools.pageSnapshot(body, true, null, null, null, 20, null, null)).isSameAs(snapshot);
        assertThat(screenshotTools.pageScreenshot(false, "step", null)).isSameAs(screenshot);
        assertThat(locatorTools.locatorClick(button, null, null)).isSameAs(click);
        assertThat(locatorTools.locatorText(button, 100, null)).isSameAs(text);

        verify(sessions).navigate("http://example.test", null, null);
        verify(sessions).pageSnapshot(body, true, null, null, null, 20, null, null);
        verify(sessions).pageScreenshot(false, "step", null);
        verify(sessions).click(button, null, null);
        verify(sessions).text(button, 100, null);
    }
}
