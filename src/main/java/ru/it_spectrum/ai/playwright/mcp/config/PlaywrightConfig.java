package ru.it_spectrum.ai.playwright.mcp.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PlaywrightMcpProperties.class)
public class PlaywrightConfig {
}
