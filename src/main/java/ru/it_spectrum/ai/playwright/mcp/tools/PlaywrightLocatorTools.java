package ru.it_spectrum.ai.playwright.mcp.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.it_spectrum.ai.playwright.mcp.api.LocatorActionResult;
import ru.it_spectrum.ai.playwright.mcp.api.LocatorSpec;
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
            description = "Click an element. Use pageSnapshot first, then prefer role/name, text, label, or testId locators; use css/xpath as fallback. For menu exploration, click link, menuitem, or navigation button names from the snapshot, then wait if needed and call pageSnapshot again.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = false)
    )
    public LocatorActionResult locatorClick(
            @McpToolParam(description = "Element locator. Example: kind=role, role=button, name=Save, exact=true") LocatorSpec locator,
            @McpToolParam(description = "Click timeout in milliseconds", required = false) Integer timeoutMs,
            @McpToolParam(description = "Bypass safety checks such as visibility and enabled state", required = false) Boolean force
    ) {
        log.info("Tool call: locatorClick (locator={})", locator);
        long start = System.nanoTime();
        try {
            LocatorActionResult result = sessions.click(locator, timeoutMs, force);
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
            @McpToolParam(description = "Editable element locator") LocatorSpec locator,
            @McpToolParam(description = "Text to put into the element") String value,
            @McpToolParam(description = "Fill timeout in milliseconds", required = false) Integer timeoutMs
    ) {
        log.info("Tool call: locatorFill (locator={})", locator);
        long start = System.nanoTime();
        try {
            LocatorActionResult result = sessions.fill(locator, value, timeoutMs);
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
            @McpToolParam(description = "Element locator to focus before pressing") LocatorSpec locator,
            @McpToolParam(description = "Key name, e.g. Enter, Escape, Control+A") String key,
            @McpToolParam(description = "Press timeout in milliseconds", required = false) Integer timeoutMs
    ) {
        log.info("Tool call: locatorPress (locator={}, key={})", locator, key);
        long start = System.nanoTime();
        try {
            LocatorActionResult result = sessions.press(locator, key, timeoutMs);
            ToolLogger.completed(log, "locatorPress", start);
            return result;
        } catch (RuntimeException e) {
            ToolLogger.failed(log, "locatorPress", start, e.getMessage());
            throw e;
        }
    }

    @McpTool(
            description = "Check or uncheck a checkbox or radio element.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = false)
    )
    public LocatorActionResult locatorCheck(
            @McpToolParam(description = "Checkbox or radio element locator") LocatorSpec locator,
            @McpToolParam(description = "true or null to check; false to uncheck", required = false) Boolean checked,
            @McpToolParam(description = "Check/uncheck timeout in milliseconds", required = false) Integer timeoutMs
    ) {
        log.info("Tool call: locatorCheck (locator={}, checked={})", locator, checked);
        long start = System.nanoTime();
        try {
            LocatorActionResult result = sessions.check(locator, checked, timeoutMs);
            ToolLogger.completed(log, "locatorCheck", start);
            return result;
        } catch (RuntimeException e) {
            ToolLogger.failed(log, "locatorCheck", start, e.getMessage());
            throw e;
        }
    }
}
