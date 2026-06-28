package ru.it_spectrum.ai.playwright.mcp.testsupport;

import ru.it_spectrum.ai.playwright.mcp.config.PlaywrightMcpProperties;

import java.nio.file.Path;

public final class TestProperties {

    private TestProperties() {
    }

    public static PlaywrightMcpProperties playwrightMcp(Path dataDir, String executablePath) {
        return new PlaywrightMcpProperties(
                dataDir.toString(),
                new PlaywrightMcpProperties.Browser("chromium", null, executablePath, true, 0),
                new PlaywrightMcpProperties.Context(1024, 768, null, "en-US", null, false),
                new PlaywrightMcpProperties.Sessions("default", "default", "default", 1, 2, 4, 5_000),
                new PlaywrightMcpProperties.Snapshot("body"));
    }
}
