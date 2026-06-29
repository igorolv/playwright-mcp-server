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
                "- grid: [2 column(s): ID, Название - rows collapsed, call pageSnapshot with includeGrids=true to read rows]",
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
                "- treegrid \"Дерево\": [rows collapsed - call pageSnapshot with includeGrids=true to read columns and rows]",
                "- button \"ОК\"");
    }

    @Test
    void collapsesHtmlTableWithColumnHeaders() {
        String snapshot = """
                - heading "Реестр" [level=1]
                - table:
                  - rowgroup:
                    - row "ID Наименование Сумма":
                      - columnheader "ID"
                      - columnheader "Наименование"
                      - columnheader "Сумма"
                    - row "1 Альфа 10":
                      - cell "1"
                      - cell "Альфа"
                      - cell "10"
                - button "Обновить\"""";

        String collapsed = PlaywrightSessionManager.collapseGrids(snapshot);

        assertThat(collapsed.lines()).containsExactly(
                "- heading \"Реестр\" [level=1]",
                "- table: [3 column(s): ID, Наименование, Сумма - rows collapsed, call pageSnapshot with includeGrids=true to read rows]",
                "- button \"Обновить\"");
        assertThat(collapsed).doesNotContain("Альфа");
    }

    @Test
    void collapsesLargeHtmlTableWithoutColumnHeaders() {
        StringBuilder snapshot = new StringBuilder("- table:\n  - rowgroup:\n");
        for (int i = 1; i <= 26; i++) {
            snapshot.append("    - row \"").append(i).append(" item\":\n")
                    .append("      - cell \"").append(i).append("\"\n")
                    .append("      - cell \"item\"\n");
        }

        String collapsed = PlaywrightSessionManager.collapseGrids(snapshot.toString());

        assertThat(collapsed).isEqualTo("- table: [rows collapsed - call pageSnapshot with includeGrids=true to read columns and rows]");
    }

    @Test
    void keepsSmallLayoutTablesExpanded() {
        String snapshot = """
                - table:
                  - rowgroup:
                    - row "Данные Реестры":
                      - cell "Данные"
                      - cell "Реестры"
                    - row "Выплаты":
                      - cell "Выплаты\"""";

        assertThat(PlaywrightSessionManager.collapseGrids(snapshot)).isEqualTo(snapshot);
    }

    @Test
    void leavesSnapshotWithoutGridsUnchanged() {
        String snapshot = """
                - button "Меню"
                - text: Привет""";

        assertThat(PlaywrightSessionManager.collapseGrids(snapshot)).isEqualTo(snapshot);
    }
}
