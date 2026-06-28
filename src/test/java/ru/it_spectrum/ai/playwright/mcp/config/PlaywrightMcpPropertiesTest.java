package ru.it_spectrum.ai.playwright.mcp.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlaywrightMcpPropertiesTest {

    @Test
    void appliesDefaultsWhenNestedPropertiesAreMissing() {
        PlaywrightMcpProperties properties = new PlaywrightMcpProperties(null, null, null, null, null);

        assertThat(properties.browser().browserName()).isEqualTo("chromium");
        assertThat(properties.browser().headless()).isTrue();
        assertThat(properties.context().viewportWidth()).isEqualTo(1440);
        assertThat(properties.context().viewportHeight()).isEqualTo(1000);
        assertThat(properties.sessions().defaultBrowserId()).isEqualTo("default");
        assertThat(properties.sessions().defaultContextId()).isEqualTo("default");
        assertThat(properties.sessions().defaultPageId()).isEqualTo("default");
        assertThat(properties.snapshot().defaultRootSelector()).isEqualTo("body");
    }

    @Test
    void normalizesInvalidNumericValues() {
        PlaywrightMcpProperties.Browser browser = new PlaywrightMcpProperties.Browser("CHROMIUM", null, null, true, -10);
        PlaywrightMcpProperties.Context context = new PlaywrightMcpProperties.Context(0, -1, null, null, null, false);
        PlaywrightMcpProperties.Sessions sessions = new PlaywrightMcpProperties.Sessions("", "", "", 0, 0, 0, 0);

        assertThat(browser.browserName()).isEqualTo("chromium");
        assertThat(browser.slowMoMs()).isZero();
        assertThat(context.viewportWidth()).isEqualTo(1440);
        assertThat(context.viewportHeight()).isEqualTo(1000);
        assertThat(sessions.maxBrowsers()).isEqualTo(2);
        assertThat(sessions.maxContexts()).isEqualTo(8);
        assertThat(sessions.maxPages()).isEqualTo(16);
        assertThat(sessions.defaultTimeoutMs()).isEqualTo(30_000);
    }
}
