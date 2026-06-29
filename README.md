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

- one default browser session
- element locator
- accessibility snapshot
- locator strategies: `role`, `text`, `label`, `placeholder`, `altText`, `title`, `testId`, `css`, `xpath`

Host-side arbitrary code execution is intentionally not included. Page JavaScript evaluation,
storage state, network routing, tracing, downloads, and PDF can be added as separate tool groups.

## Tools

| Group | Flag | Tools |
|---|---|---|
| Page | `PLAYWRIGHT_MCP_TOOLS_PAGE` | `pageNavigate`, `pageWaitForLocator`, `pageSnapshot` |
| Locator | `PLAYWRIGHT_MCP_TOOLS_LOCATOR` | `locatorClick`, `locatorFill`, `locatorPress`, `locatorCheck`, `locatorText` |
| Screenshot | `PLAYWRIGHT_MCP_TOOLS_SCREENSHOT` | `pageScreenshot` |

All groups are enabled by default.

`pageNavigate` lazily creates the server-managed default `Browser`, `BrowserContext`, and `Page`, so
a client can start with:

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

with `pageSnapshot`.

`pageSnapshot` is the single page-inspection tool. By default it returns just the structural overview
(`snapshot`): headings, landmarks, links, roles, and locator targets, with grids and tables collapsed
to a one-line summary. Opt into extra sections with flags (each adds tokens and time):

- `includeControls=true` adds the `controls` section: interactive controls with the detail the
  structural snapshot omits — role, type, label/placeholder, enabled/disabled, checked, visible/hidden,
  and a tooltip. Typed field values are not returned.
- `includeTooltips=true` (with `includeControls`) additionally hovers icon-only buttons that expose
  only an icon code (e.g. `account_balance`) and captures their real tooltip.
- `includeGrids=true` adds the `grids` section: the rows and columns inside tables and ARIA
  grids/treegrids (ag-Grid, Angular Material, plain HTML tables). Virtualized grids only keep on-screen
  rows in the DOM, so `renderedRowCount` may be smaller than the real total.

Bound large pages with the `maxControls` and `maxRows` caps, and narrow the read with a root `locator`
(e.g. `nav`, `aside`, `main`) instead of the default `body`.

After a menu click or filter change in a single-page app, use `pageWaitForLocator` (e.g. wait for
`role=row` to become `visible`, or for a loading spinner to become `hidden`) before snapshotting so
asynchronous content has settled.

For a narrow read from a known element, use `locatorText` instead of a full `pageSnapshot`. It reads
the first matching element's compact text, input value, selected option text, `aria-label`, or
`title`, and returns the total match count so ambiguous locators are visible. This is useful for
large table rows and status labels.

`pageScreenshot` captures the current browser page as a PNG file under
`${playwright-mcp.data-dir}/screenshots`. It returns the absolute and relative file paths plus page
metadata; it does not return image bytes in the MCP response. Use it for documentation, bug reports,
and step-by-step evidence.

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

Manual live application exploration:

```powershell
$env:LIVE_APPLICATION_URL="<application-url>"
$env:LIVE_APPLICATION_USERNAME="<username>"
$env:LIVE_APPLICATION_PASSWORD="<password>"
.\gradlew.bat liveApplicationTest
```

`liveApplicationTest` is excluded from `test`, `build`, `integrationTest`, and `smokeTest`. It is a
manual live smoke/exploration check: it opens the configured application URL, logs in with the
configured credentials, opens discovered menu entries, captures compact accessibility snapshots,
and writes `build/reports/live-application/report.md` with page and form descriptions. Required env
vars are `LIVE_APPLICATION_URL`, `LIVE_APPLICATION_USERNAME`, and `LIVE_APPLICATION_PASSWORD`; the
test is skipped when any of them is missing. Optional env vars: `LIVE_APPLICATION_HEADLESS` (default
`true`) and `LIVE_APPLICATION_MAX_MENU_ITEMS` (default `30`).

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
| `PLAYWRIGHT_MCP_TOOLS_SCREENSHOT` | `true` |
| `PLAYWRIGHT_MCP_VIEWPORT_WIDTH` | `1440` |
| `PLAYWRIGHT_MCP_VIEWPORT_HEIGHT` | `1000` |
| `PLAYWRIGHT_MCP_DEFAULT_TIMEOUT_MS` | `30000` |
| `PLAYWRIGHT_MCP_MAX_BROWSERS` | `2` |
| `PLAYWRIGHT_MCP_MAX_CONTEXTS` | `8` |
| `PLAYWRIGHT_MCP_MAX_PAGES` | `16` |

Logs go to stderr and `${playwright-mcp.data-dir}/logs/playwright-mcp-server.log`. stdout is kept
for MCP JSON-RPC.
