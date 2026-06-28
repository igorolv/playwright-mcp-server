package ru.it_spectrum.ai.playwright.mcp.smoke;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ru.it_spectrum.ai.playwright.mcp.config.JsonConfig;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("smoke")
class McpStdioSmokeTest {

    private final ObjectMapper mapper = new JsonConfig().playwrightMcpObjectMapper();

    @Test
    void packagedJarRespondsToInitializeOverStdio() throws Exception {
        Process process = startPackagedServer();

        try {
            BufferedWriter stdin = new BufferedWriter(new OutputStreamWriter(
                    process.getOutputStream(), StandardCharsets.UTF_8));
            BufferedReader stdout = new BufferedReader(new InputStreamReader(
                    process.getInputStream(), StandardCharsets.UTF_8));

            Map<String, Object> response = initialize(stdin, stdout);

            assertThat(response.get("id")).isEqualTo(1);
            assertThat(response.get("result")).isInstanceOf(Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) response.get("result");
            @SuppressWarnings("unchecked")
            Map<String, Object> serverInfo = (Map<String, Object>) result.get("serverInfo");
            assertThat(serverInfo.get("name")).isEqualTo("playwright-mcp-server");
            assertThat(result.get("capabilities")).isInstanceOf(Map.class);
        } finally {
            stop(process);
        }
    }

    @Test
    void toolsListContractIsSelfDescribing() throws Exception {
        Process process = startPackagedServer();

        try {
            BufferedWriter stdin = new BufferedWriter(new OutputStreamWriter(
                    process.getOutputStream(), StandardCharsets.UTF_8));
            BufferedReader stdout = new BufferedReader(new InputStreamReader(
                    process.getInputStream(), StandardCharsets.UTF_8));

            initialize(stdin, stdout);
            writeJsonLine(stdin, Map.of(
                    "jsonrpc", "2.0",
                    "method", "notifications/initialized",
                    "params", Map.of()));
            writeJsonLine(stdin, Map.of(
                    "jsonrpc", "2.0",
                    "id", 2,
                    "method", "tools/list",
                    "params", Map.of()));

            Map<String, Object> response = readJsonLineWithId(stdout, 2, Duration.ofSeconds(15));
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) response.get("result");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> tools = (List<Map<String, Object>>) result.get("tools");
            assertThat(tools).extracting(tool -> tool.get("name"))
                    .containsExactlyInAnyOrder(
                            "pageNavigate",
                            "pageSnapshot",
                            "pageWaitForLocator",
                            "pageScreenshot",
                            "locatorClick",
                            "locatorFill",
                            "locatorPress",
                            "locatorCheck");

            String toolsJson = mapper.writeValueAsString(tools);
            assertThat(toolsJson)
                    .doesNotContain("pageId")
                    .doesNotContain("Playwright")
                    .doesNotContain("BrowserType")
                    .doesNotContain("LocatorSpec")
                    .doesNotContain("Optional")
                    .doesNotContain("optional")
                    .doesNotContain("Page.")
                    .doesNotContain("Locator.");

            Map<String, Object> pageNavigate = tool(tools, "pageNavigate");
            @SuppressWarnings("unchecked")
            Map<String, Object> pageNavigateSchema = (Map<String, Object>) pageNavigate.get("inputSchema");
            assertThat(pageNavigateSchema.get("required")).isEqualTo(List.of("url"));

            Map<String, Object> locatorClick = tool(tools, "locatorClick");
            @SuppressWarnings("unchecked")
            Map<String, Object> locatorClickSchema = (Map<String, Object>) locatorClick.get("inputSchema");
            assertThat(locatorClickSchema.get("required")).isEqualTo(List.of("locator"));
            @SuppressWarnings("unchecked")
            Map<String, Object> locatorClickProperties = (Map<String, Object>) locatorClickSchema.get("properties");
            @SuppressWarnings("unchecked")
            Map<String, Object> locatorSchema = (Map<String, Object>) locatorClickProperties.get("locator");
            assertThat(locatorSchema).doesNotContainKey("required");
            @SuppressWarnings("unchecked")
            Map<String, Object> locatorProperties = (Map<String, Object>) locatorSchema.get("properties");
            @SuppressWarnings("unchecked")
            Map<String, Object> kindSchema = (Map<String, Object>) locatorProperties.get("kind");
            assertThat(kindSchema.get("enum"))
                    .isEqualTo(List.of("css", "xpath", "role", "text", "label", "placeholder", "altText", "title", "testId"));
            @SuppressWarnings("unchecked")
            List<Object> kindType = (List<Object>) kindSchema.get("type");
            assertThat(kindType).containsExactlyInAnyOrder("string", "null");
        } finally {
            stop(process);
        }
    }

    private Process startPackagedServer() throws IOException {
        Path jar = Path.of("build", "libs", "playwright-mcp-server.jar");
        assertThat(Files.isRegularFile(jar)).as("bootJar must run before smokeTest").isTrue();

        return new ProcessBuilder(javaExecutable(), "-jar", jar.toString())
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectInput(ProcessBuilder.Redirect.PIPE)
                .start();
    }

    private Map<String, Object> initialize(BufferedWriter stdin, BufferedReader stdout) throws Exception {
        writeJsonLine(stdin, Map.of(
                "jsonrpc", "2.0",
                "id", 1,
                "method", "initialize",
                "params", Map.of(
                        "protocolVersion", "2024-11-05",
                        "capabilities", Map.of(),
                        "clientInfo", Map.of("name", "playwright-mcp-smoke", "version", "0.0.0"))));

        return readJsonLineWithId(stdout, 1, Duration.ofSeconds(15));
    }

    private Map<String, Object> tool(List<Map<String, Object>> tools, String name) {
        return tools.stream()
                .filter(tool -> name.equals(tool.get("name")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing tool: " + name));
    }

    private void stop(Process process) throws InterruptedException {
        process.destroy();
        if (!process.waitFor(3, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            process.waitFor(3, TimeUnit.SECONDS);
        }
    }

    private void writeJsonLine(BufferedWriter writer, Map<String, Object> payload) throws IOException {
        writer.write(mapper.writeValueAsString(payload));
        writer.newLine();
        writer.flush();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readJsonLineWithId(BufferedReader reader, int id, Duration timeout) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (reader.ready()) {
                String line = reader.readLine();
                if (line != null && !line.isBlank()) {
                    Map<String, Object> response = mapper.readValue(line, Map.class);
                    if (Integer.valueOf(id).equals(response.get("id"))) {
                        return response;
                    }
                }
            }
            Thread.sleep(50);
        }
        throw new AssertionError("Timed out waiting for MCP stdio response with id " + id);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readJsonLine(BufferedReader reader, Duration timeout) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (reader.ready()) {
                String line = reader.readLine();
                if (line != null && !line.isBlank()) {
                    return mapper.readValue(line, Map.class);
                }
            }
            Thread.sleep(50);
        }
        throw new AssertionError("Timed out waiting for MCP stdio response");
    }

    private String javaExecutable() {
        Path javaHome = Path.of(System.getProperty("java.home"));
        String executable = System.getProperty("os.name", "").toLowerCase().contains("win")
                ? "java.exe"
                : "java";
        return javaHome.resolve("bin").resolve(executable).toString();
    }
}
