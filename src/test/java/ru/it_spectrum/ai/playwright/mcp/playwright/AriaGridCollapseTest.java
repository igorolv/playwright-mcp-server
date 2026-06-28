package ru.it_spectrum.ai.playwright.mcp.playwright;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AriaGridCollapseTest {

    @Test
    void collapsesGridBodyKeepingSurroundingStructureAndColumns() {
        String snapshot = """
                - heading "Каталог" [level=3]
                - grid:
                  - rowgroup:
                    - row "ID Название":
                      - columnheader "ID"
                      - columnheader "Название"
                    - row "Open Filter Menu":
                      - gridcell "Open Filter Menu":
                        - textbox "Фильтр"
                  - rowgroup:
                    - row "1 Альфа":
                      - gridcell "1"
                      - gridcell "Альфа"
                - contentinfo: © 2026""";

        String collapsed = PlaywrightSessionManager.collapseGrids(snapshot);

        assertThat(collapsed.lines()).containsExactly(
                "- heading \"Каталог\" [level=3]",
                "- grid: [2 column(s): ID, Название - rows collapsed, call pageGridSnapshot to read rows]",
                "- contentinfo: © 2026");
        assertThat(collapsed).doesNotContain("gridcell", "rowgroup", "Альфа");
    }

    @Test
    void collapsesNamedTreegridWithoutColumnHeaders() {
        String snapshot = """
                - treegrid "Дерево":
                  - row:
                    - gridcell "узел"
                - button "ОК\"""";

        String collapsed = PlaywrightSessionManager.collapseGrids(snapshot);

        assertThat(collapsed.lines()).containsExactly(
                "- treegrid \"Дерево\": [rows collapsed - call pageGridSnapshot to read columns and rows]",
                "- button \"ОК\"");
    }

    @Test
    void leavesSnapshotWithoutGridsUnchanged() {
        String snapshot = """
                - button "Меню"
                - text: Привет""";

        assertThat(PlaywrightSessionManager.collapseGrids(snapshot)).isEqualTo(snapshot);
    }
}
