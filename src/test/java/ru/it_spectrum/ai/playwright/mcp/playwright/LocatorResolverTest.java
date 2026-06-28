package ru.it_spectrum.ai.playwright.mcp.playwright;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.it_spectrum.ai.playwright.mcp.api.LocatorSpec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocatorResolverTest {

    private final LocatorResolver resolver = new LocatorResolver();

    @Test
    void resolvesRoleLocatorWithNameAndExactOptions() {
        Page page = mock(Page.class);
        Locator locator = mock(Locator.class);
        when(page.getByRole(eq(AriaRole.BUTTON), any(Page.GetByRoleOptions.class))).thenReturn(locator);

        Locator result = resolver.resolve(page, new LocatorSpec(
                "role", null, "button", "Save", true, null, null, null, null));

        ArgumentCaptor<Page.GetByRoleOptions> options = ArgumentCaptor.forClass(Page.GetByRoleOptions.class);
        verify(page).getByRole(eq(AriaRole.BUTTON), options.capture());
        assertThat(result).isSameAs(locator);
        assertThat(options.getValue().name).isEqualTo("Save");
        assertThat(options.getValue().exact).isTrue();
    }

    @Test
    void appliesCssFilterAndNthRefinements() {
        Page page = mock(Page.class);
        Locator base = mock(Locator.class);
        Locator textFiltered = mock(Locator.class);
        Locator visibleFiltered = mock(Locator.class);
        Locator nth = mock(Locator.class);
        when(page.locator("#items button")).thenReturn(base);
        when(base.filter(any(Locator.FilterOptions.class))).thenReturn(textFiltered);
        when(textFiltered.filter(any(Locator.FilterOptions.class))).thenReturn(visibleFiltered);
        when(visibleFiltered.nth(1)).thenReturn(nth);

        Locator result = resolver.resolve(page, new LocatorSpec(
                "css", "#items button", null, null, null, "Save", 1, true, null));

        assertThat(result).isSameAs(nth);
        verify(page).locator("#items button");
        verify(base).filter(any(Locator.FilterOptions.class));
        verify(textFiltered).filter(any(Locator.FilterOptions.class));
        verify(visibleFiltered).nth(1);
    }
}
