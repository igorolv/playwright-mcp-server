package ru.it_spectrum.ai.playwright.mcp.config;

import org.springframework.ai.mcp.customizer.McpSyncServerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Stdio MCP uses stdout as the JSON-RPC transport. Immediate execution keeps sync tool responses
 * ordered and avoids concurrent completions racing on the shared stream.
 */
@Configuration
public class McpServerConfig {

    @Bean
    McpSyncServerCustomizer stdioSyncServerCustomizer() {
        return serverBuilder -> serverBuilder.immediateExecution(true);
    }
}
