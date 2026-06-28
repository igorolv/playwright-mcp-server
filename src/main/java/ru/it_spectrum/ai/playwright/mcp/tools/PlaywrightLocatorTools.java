package ru.it_spectrum.ai.playwright.mcp.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.it_spectrum.ai.playwright.mcp.api.LocatorActionResult;
import ru.it_spectrum.ai.playwright.mcp.api.LocatorCountResult;
import ru.it_spectrum.ai.playwright.mcp.api.LocatorSpec;
import ru.it_spectrum.ai.playwright.mcp.api.LocatorTextResult;
import ru.it_spectrum.ai.playwright.mcp.playwright.PlaywrightSessionManager;

@Service
@ConditionalOnProperty(prefix = "playwright-mcp.tools", name = "locator", havingValue = "true", matchIfMissing = true)
public class PlaywrightLocatorTools {

    private static final Logger log = LoggerFactory.getLogger(PlaywrightLocatorTools.class);

    private final PlaywrightSessionManager sessions;

    public PlaywrightLocatorTools(PlaywrightSessionManager sessions) {
        this.sessions = sessions;
    }

    @McpTool(
            description = "Click an element. Use pageAriaSnapshot first, then prefer role/name, text, label, or testId locators; use css/xpath as fallback. For menu exploration, click link, menuitem, or navigation button names from the snapshot, then wait if needed and call pageAriaSnapshot again.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = false)
    )
    public LocatorActionResult locatorClick(
            @McpToolParam(description = "Logical browser tab id", required = false) String pageId,
            @McpToolParam(description = "Element locator. Example: kind=role, role=button, name=Save, exact=true") LocatorSpec locator,
            @McpToolParam(description = "Click timeout in milliseconds", required = false) Integer timeoutMs,
            @McpToolParam(description = "Bypass safety checks such as visibility and enabled state", required = false) Boolean force
    ) {
        log.info("Tool call: locatorClick (pageId={}, locator={})", pageId, locator);
        long start = System.nanoTime();
        try {
            LocatorActionResult result = sessions.click(pageId, locator, timeoutMs, force);
            ToolLogger.completed(log, "locatorClick", start);
            return result;
        } catch (RuntimeException e) {
            ToolLogger.failed(log, "locatorClick", start, e.getMessage());
            throw e;
        }
    }

    @McpTool(
            description = "Fill an editable element with text. Prefer label, placeholder, textbox role, or testId locators; for password fields use label/placeholder or css such as input[type='password'] when no accessible role is exposed.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = false)
    )
    public LocatorActionResult locatorFill(
            @McpToolParam(description = "Logical browser tab id", required = false) String pageId,
            @McpToolParam(description = "Editable element locator") LocatorSpec locator,
            @McpToolParam(description = "Text to put into the element") String value,
            @McpToolParam(description = "Fill timeout in milliseconds", required = false) Integer timeoutMs
    ) {
        log.info("Tool call: locatorFill (pageId={}, locator={})", pageId, locator);
        long start = System.nanoTime();
        try {
            LocatorActionResult result = sessions.fill(pageId, locator, value, timeoutMs);
            ToolLogger.completed(log, "locatorFill", start);
            return result;
        } catch (RuntimeException e) {
            ToolLogger.failed(log, "locatorFill", start, e.getMessage());
            throw e;
        }
    }

    @McpTool(
            description = "Focus an element and press a keyboard key, e.g. Enter, Escape, Control+A.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = false)
    )
    public LocatorActionResult locatorPress(
            @McpToolParam(description = "Logical browser tab id", required = false) String pageId,
            @McpToolParam(description = "Element locator to focus before pressing") LocatorSpec locator,
            @McpToolParam(description = "Key name, e.g. Enter, Escape, Control+A") String key,
            @McpToolParam(description = "Press timeout in milliseconds", required = false) Integer timeoutMs
    ) {
        log.info("Tool call: locatorPress (pageId={}, locator={}, key={})", pageId, locator, key);
        long start = System.nanoTime();
        try {
            LocatorActionResult result = sessions.press(pageId, locator, key, timeoutMs);
            ToolLogger.completed(log, "locatorPress", start);
            return result;
        } catch (RuntimeException e) {
            ToolLogger.failed(log, "locatorPress", start, e.getMessage());
            throw e;
        }
    }

    @McpTool(
            description = "Move the mouse over an element.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = false)
    )
    public LocatorActionResult locatorHover(
            @McpToolParam(description = "Logical browser tab id", required = false) String pageId,
            @McpToolParam(description = "Element locator") LocatorSpec locator,
            @McpToolParam(description = "Hover timeout in milliseconds", required = false) Integer timeoutMs
    ) {
        log.info("Tool call: locatorHover (pageId={}, locator={})", pageId, locator);
        long start = System.nanoTime();
        try {
            LocatorActionResult result = sessions.hover(pageId, locator, timeoutMs);
            ToolLogger.completed(log, "locatorHover", start);
            return result;
        } catch (RuntimeException e) {
            ToolLogger.failed(log, "locatorHover", start, e.getMessage());
            throw e;
        }
    }

    @McpTool(
            description = "Check or uncheck a checkbox or radio element.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = false)
    )
    public LocatorActionResult locatorCheck(
            @McpToolParam(description = "Logical browser tab id", required = false) String pageId,
            @McpToolParam(description = "Checkbox or radio element locator") LocatorSpec locator,
            @McpToolParam(description = "true or null to check; false to uncheck", required = false) Boolean checked,
            @McpToolParam(description = "Check/uncheck timeout in milliseconds", required = false) Integer timeoutMs
    ) {
        log.info("Tool call: locatorCheck (pageId={}, locator={}, checked={})", pageId, locator, checked);
        long start = System.nanoTime();
        try {
            LocatorActionResult result = sessions.check(pageId, locator, checked, timeoutMs);
            ToolLogger.completed(log, "locatorCheck", start);
            return result;
        } catch (RuntimeException e) {
            ToolLogger.failed(log, "locatorCheck", start, e.getMessage());
            throw e;
        }
    }

    @McpTool(
            description = "Read text and common state from matching elements: count, textContent, innerText, inputValue, visible, enabled, allTextContents.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public LocatorTextResult locatorText(
            @McpToolParam(description = "Logical browser tab id", required = false) String pageId,
            @McpToolParam(description = "Element locator to inspect") LocatorSpec locator,
            @McpToolParam(description = "Timeout for first-locator text/state operations in milliseconds", required = false) Integer timeoutMs
    ) {
        log.info("Tool call: locatorText (pageId={}, locator={})", pageId, locator);
        long start = System.nanoTime();
        try {
            LocatorTextResult result = sessions.locatorText(pageId, locator, timeoutMs);
            ToolLogger.completed(log, "locatorText", start);
            return result;
        } catch (RuntimeException e) {
            ToolLogger.failed(log, "locatorText", start, e.getMessage());
            throw e;
        }
    }

    @McpTool(
            description = "Count elements matching a locator.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public LocatorCountResult locatorCount(
            @McpToolParam(description = "Logical browser tab id", required = false) String pageId,
            @McpToolParam(description = "Element locator to count") LocatorSpec locator
    ) {
        log.info("Tool call: locatorCount (pageId={}, locator={})", pageId, locator);
        long start = System.nanoTime();
        try {
            LocatorCountResult result = sessions.locatorCount(pageId, locator);
            ToolLogger.completed(log, "locatorCount", start);
            return result;
        } catch (RuntimeException e) {
            ToolLogger.failed(log, "locatorCount", start, e.getMessage());
            throw e;
        }
    }
}
