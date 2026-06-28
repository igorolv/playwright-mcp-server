# Playwright MCP Server

Local MCP server that exposes Playwright Java (`com.microsoft.playwright`) through Spring AI MCP
tools over stdio.

The project follows the same stack as the sibling MCP servers:

- Java + Gradle Kotlin DSL
- Spring Boot 4
- Spring AI MCP Server over stdio
- Playwright Java
- Java records for structured MCP responses
- `@McpTool` services with feature flags under `playwright-mcp.tools.*`

## Scope

This implementation is backed by Playwright Java and keeps its browser automation concepts visible
where they matter, but tool descriptions are written for an agent that may only see the MCP
contract:

- browser process
- isolated browser context
- browser tab
- element locator
- accessibility snapshot
- locator strategies: `role`, `text`, `label`, `placeholder`, `altText`, `title`, `testId`, `css`, `xpath`

Host-side arbitrary code execution is intentionally not included. Page JavaScript evaluation,
storage state, network routing, tracing, screenshots, downloads, and PDF can be added as separate
tool groups.

## Tools

| Group | Flag | Tools |
|---|---|---|
| Lifecycle | `PLAYWRIGHT_MCP_TOOLS_LIFECYCLE` | `launchBrowser`, `listBrowsers`, `closeBrowser`, `newBrowserContext`, `listBrowserContexts`, `closeBrowserContext`, `newPage`, `listPages`, `closePage` |
| Page | `PLAYWRIGHT_MCP_TOOLS_PAGE` | `pageNavigate`, `pageReload`, `pageWaitForLoadState`, `pageAriaSnapshot` |
| Locator | `PLAYWRIGHT_MCP_TOOLS_LOCATOR` | `locatorClick`, `locatorFill`, `locatorPress`, `locatorHover`, `locatorCheck`, `locatorText`, `locatorCount` |

All groups are enabled by default.

Most page tools lazily create the default `Browser`, `BrowserContext`, and `Page`, so a client can
start with:

```json
{
  "url": "https://example.com"
}
```

for `pageNavigate`, then inspect:

```json
{
  "locator": {
    "kind": "css",
    "value": "body"
  }
}
```

with `pageAriaSnapshot`.

## Locator Object

The locator object is the main input shape for element actions and reads:

```json
{
  "kind": "role",
  "role": "button",
  "name": "Save",
  "exact": true,
  "hasText": null,
  "nth": 0,
  "visible": true,
  "frameSelector": null
}
```

Supported `kind` values:

- `css`: CSS selector in `value`
- `xpath`: XPath selector in `value`
- `role`: accessible role in `role`, accessible name in `name`
- `text`: visible text in `value`
- `label`: form field label in `value`
- `placeholder`: placeholder text in `value`
- `altText`: image/media alt text in `value`
- `title`: title attribute text in `value`
- `testId`: test id in `value`

`hasText`, `visible`, and `nth` narrow the match. `frameSelector` searches inside an iframe selected
by CSS.

## Build

```powershell
.\gradlew.bat build
```

Fast unit tests:

```powershell
.\gradlew.bat test
```

Install Chromium browser binaries for Playwright Java:

```powershell
.\gradlew.bat installPlaywrightBrowsers
```

Integration and smoke checks:

```powershell
.\gradlew.bat integrationTest
.\gradlew.bat smokeTest
```

`integrationTest` runs against an in-process local HTTP site and a real headless Chromium. It uses
`PLAYWRIGHT_MCP_EXECUTABLE`, local Chrome/Edge, or an already-installed Playwright Chromium cache.
If none is available, the test is skipped before Playwright is started.

## Configuration

The server is stdio-only.

| Variable | Default |
|---|---|
| `PLAYWRIGHT_MCP_DATA_DIR` | `${user.home}/.playwright-mcp-server` |
| `PLAYWRIGHT_MCP_BROWSER` | `chromium` |
| `PLAYWRIGHT_MCP_CHANNEL` | empty |
| `PLAYWRIGHT_MCP_EXECUTABLE` | empty |
| `PLAYWRIGHT_MCP_HEADLESS` | `true` |
| `PLAYWRIGHT_MCP_SLOW_MO_MS` | `0` |
| `PLAYWRIGHT_MCP_VIEWPORT_WIDTH` | `1440` |
| `PLAYWRIGHT_MCP_VIEWPORT_HEIGHT` | `1000` |
| `PLAYWRIGHT_MCP_DEFAULT_TIMEOUT_MS` | `30000` |
| `PLAYWRIGHT_MCP_MAX_BROWSERS` | `2` |
| `PLAYWRIGHT_MCP_MAX_CONTEXTS` | `8` |
| `PLAYWRIGHT_MCP_MAX_PAGES` | `16` |

Logs go to stderr and `${playwright-mcp.data-dir}/logs/playwright-mcp-server.log`. stdout is kept
for MCP JSON-RPC.
