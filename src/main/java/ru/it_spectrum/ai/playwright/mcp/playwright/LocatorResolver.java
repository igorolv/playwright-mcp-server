package ru.it_spectrum.ai.playwright.mcp.playwright;

import com.microsoft.playwright.FrameLocator;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import ru.it_spectrum.ai.playwright.mcp.api.LocatorSpec;

import java.util.Locale;

public class LocatorResolver {

    public Locator resolve(Page page, LocatorSpec spec) {
        LocatorSpec actual = spec == null ? LocatorSpec.css("body") : spec;
        Locator locator = baseLocator(page, actual);
        if (actual.hasText() != null && !actual.hasText().isBlank()) {
            locator = locator.filter(new Locator.FilterOptions().setHasText(actual.hasText()));
        }
        if (actual.visible() != null) {
            locator = locator.filter(new Locator.FilterOptions().setVisible(actual.visible()));
        }
        if (actual.nth() != null) {
            locator = locator.nth(actual.nth());
        }
        return locator;
    }

    private Locator baseLocator(Page page, LocatorSpec spec) {
        String kind = normalize(spec.kind(), "css");
        if (spec.frameSelector() != null && !spec.frameSelector().isBlank()) {
            return baseFrameLocator(page.frameLocator(spec.frameSelector()), kind, spec);
        }
        return switch (kind) {
            case "css" -> page.locator(required(spec.value(), "value"));
            case "xpath" -> page.locator(xpathSelector(required(spec.value(), "value")));
            case "role" -> page.getByRole(role(spec), roleOptions(spec));
            case "text" -> page.getByText(required(spec.value(), "value"), textOptions(spec));
            case "label" -> page.getByLabel(required(spec.value(), "value"), labelOptions(spec));
            case "placeholder" -> page.getByPlaceholder(required(spec.value(), "value"), placeholderOptions(spec));
            case "alttext", "alt-text", "alt_text" -> page.getByAltText(required(spec.value(), "value"), altTextOptions(spec));
            case "title" -> page.getByTitle(required(spec.value(), "value"), titleOptions(spec));
            case "testid", "test-id", "test_id" -> page.getByTestId(required(spec.value(), "value"));
            default -> throw new IllegalArgumentException("Unsupported locator kind: " + spec.kind());
        };
    }

    private Locator baseFrameLocator(FrameLocator frame, String kind, LocatorSpec spec) {
        return switch (kind) {
            case "css" -> frame.locator(required(spec.value(), "value"));
            case "xpath" -> frame.locator(xpathSelector(required(spec.value(), "value")));
            case "role" -> frame.getByRole(role(spec), frameRoleOptions(spec));
            case "text" -> frame.getByText(required(spec.value(), "value"), frameTextOptions(spec));
            case "label" -> frame.getByLabel(required(spec.value(), "value"), frameLabelOptions(spec));
            case "placeholder" -> frame.getByPlaceholder(required(spec.value(), "value"), framePlaceholderOptions(spec));
            case "alttext", "alt-text", "alt_text" -> frame.getByAltText(required(spec.value(), "value"), frameAltTextOptions(spec));
            case "title" -> frame.getByTitle(required(spec.value(), "value"), frameTitleOptions(spec));
            case "testid", "test-id", "test_id" -> frame.getByTestId(required(spec.value(), "value"));
            default -> throw new IllegalArgumentException("Unsupported locator kind: " + spec.kind());
        };
    }

    private AriaRole role(LocatorSpec spec) {
        String value = spec.role() != null && !spec.role().isBlank() ? spec.role() : spec.value();
        value = required(value, "role").replace("-", "").replace("_", "").toUpperCase(Locale.ROOT);
        return AriaRole.valueOf(value);
    }

    private Page.GetByRoleOptions roleOptions(LocatorSpec spec) {
        var options = new Page.GetByRoleOptions();
        if (spec.name() != null && !spec.name().isBlank()) {
            options.setName(spec.name());
        }
        if (spec.exact() != null) {
            options.setExact(spec.exact());
        }
        return options;
    }

    private FrameLocator.GetByRoleOptions frameRoleOptions(LocatorSpec spec) {
        var options = new FrameLocator.GetByRoleOptions();
        if (spec.name() != null && !spec.name().isBlank()) {
            options.setName(spec.name());
        }
        if (spec.exact() != null) {
            options.setExact(spec.exact());
        }
        return options;
    }

    private Page.GetByTextOptions textOptions(LocatorSpec spec) {
        var options = new Page.GetByTextOptions();
        if (spec.exact() != null) {
            options.setExact(spec.exact());
        }
        return options;
    }

    private FrameLocator.GetByTextOptions frameTextOptions(LocatorSpec spec) {
        var options = new FrameLocator.GetByTextOptions();
        if (spec.exact() != null) {
            options.setExact(spec.exact());
        }
        return options;
    }

    private Page.GetByLabelOptions labelOptions(LocatorSpec spec) {
        var options = new Page.GetByLabelOptions();
        if (spec.exact() != null) {
            options.setExact(spec.exact());
        }
        return options;
    }

    private FrameLocator.GetByLabelOptions frameLabelOptions(LocatorSpec spec) {
        var options = new FrameLocator.GetByLabelOptions();
        if (spec.exact() != null) {
            options.setExact(spec.exact());
        }
        return options;
    }

    private Page.GetByPlaceholderOptions placeholderOptions(LocatorSpec spec) {
        var options = new Page.GetByPlaceholderOptions();
        if (spec.exact() != null) {
            options.setExact(spec.exact());
        }
        return options;
    }

    private FrameLocator.GetByPlaceholderOptions framePlaceholderOptions(LocatorSpec spec) {
        var options = new FrameLocator.GetByPlaceholderOptions();
        if (spec.exact() != null) {
            options.setExact(spec.exact());
        }
        return options;
    }

    private Page.GetByAltTextOptions altTextOptions(LocatorSpec spec) {
        var options = new Page.GetByAltTextOptions();
        if (spec.exact() != null) {
            options.setExact(spec.exact());
        }
        return options;
    }

    private FrameLocator.GetByAltTextOptions frameAltTextOptions(LocatorSpec spec) {
        var options = new FrameLocator.GetByAltTextOptions();
        if (spec.exact() != null) {
            options.setExact(spec.exact());
        }
        return options;
    }

    private Page.GetByTitleOptions titleOptions(LocatorSpec spec) {
        var options = new Page.GetByTitleOptions();
        if (spec.exact() != null) {
            options.setExact(spec.exact());
        }
        return options;
    }

    private FrameLocator.GetByTitleOptions frameTitleOptions(LocatorSpec spec) {
        var options = new FrameLocator.GetByTitleOptions();
        if (spec.exact() != null) {
            options.setExact(spec.exact());
        }
        return options;
    }

    private String xpathSelector(String value) {
        return value.startsWith("xpath=") ? value : "xpath=" + value;
    }

    private String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Locator " + name + " is required");
        }
        return value;
    }

    private String normalize(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
