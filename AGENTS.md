# AGENTS.md - Engineering Guide for AI Coding Agents

This repository is a local MCP server that exposes Playwright Java through Spring AI MCP tools.
It is intentionally Playwright-native: public tools and records may use Browser, BrowserContext,
Page, Locator, and LocatorSpec concepts.

## Core Invariants

1. Stdio only. Keep `spring.main.web-application-type: none`.
2. Do not write application logs to stdout. stdout is the MCP JSON-RPC transport.
3. Use Playwright Java on the dedicated runtime thread through `PlaywrightDispatcher`.
4. Keep tool classes thin. Tools log, delegate to `PlaywrightSessionManager`, and return `api/*`
   records.
5. Prefer structured Playwright operations over arbitrary host-side code execution. Do not add an
   unsafe host code tool unless it is explicitly requested and disabled by default.
6. Keep response sizes controlled. Do not append screenshots, traces, network logs, or full HTML to
   default responses.

## Stack

- Java 25 toolchain
- Gradle Kotlin DSL
- Spring Boot 4
- Spring AI MCP Server over stdio
- Playwright Java
- Jackson 3 mapper from `JsonConfig`

Dependency versions live in `gradle/libs.versions.toml`.

## Source Layout

Package root: `ru.it_spectrum.ai.playwright.mcp`.

```text
tools/       -> thin @McpTool adapters
playwright/  -> Playwright runtime/session manager/locator resolver
api/         -> MCP response/input records
config/      -> Spring properties, JSON mapper, MCP stdio customizer
```

## Tool Conventions

Use the existing pattern:

```java
@McpTool(
        description = "...",
        generateOutputSchema = true,
        annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
)
public SomeResult someTool(...) {
    log.info("Tool call: someTool (...)");
    long start = System.nanoTime();
    try {
        SomeResult result = service.doWork(...);
        ToolLogger.completed(log, "someTool", start);
        return result;
    } catch (RuntimeException e) {
        ToolLogger.failed(log, "someTool", start, e.getMessage());
        throw e;
    }
}
```

Tool descriptions are written for the model that will call them. Be concrete about Playwright
semantics: BrowserContext isolation, Page navigation, Locator kinds, waitUntil/load states, and
timeouts.

## Build And Test

Use the Gradle wrapper:

```powershell
.\gradlew.bat build --console=plain
.\gradlew.bat test --console=plain
.\gradlew.bat integrationTest --console=plain
.\gradlew.bat smokeTest --console=plain
```

`test` is for fast unit tests and must not require launching a browser. Browser-backed tests should
be tagged `integration`.
