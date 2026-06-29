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
    void collapsesAdfBodyTableWithNestedRowTables() {
        StringBuilder snapshot = new StringBuilder("- table:\n  - rowgroup:\n");
        for (int i = 1; i <= 6; i++) {
            snapshot.append("    - row \"").append(i).append(" 8 324 3475 АКБ \\\"ПРО\\\"\":\n")
                    .append("      - cell \"").append(i).append("\"\n")
                    .append("      - cell \"8 324 3475 АКБ \\\"ПРО\\\"\":\n")
                    .append("        - table:\n")
                    .append("          - rowgroup:\n")
                    .append("            - row \"8 324 3475 АКБ \\\"ПРО\\\"\":\n")
                    .append("              - cell \"8 324\"\n")
                    .append("              - cell \"3475\"\n")
                    .append("              - cell \"АКБ \\\"ПРО\\\"\"\n");
        }

        var result = PlaywrightSessionManager.collapseSnapshot(snapshot.toString(), true);
        String collapsed = result.snapshot();

        assertThat(collapsed).isEqualTo("- table: [rows collapsed - call pageSnapshot with includeGrids=true to read columns and rows]");
        assertThat(collapsed).doesNotContain("АКБ");
        assertThat(result.info().enabled()).isTrue();
        assertThat(result.info().collapsedCount()).isEqualTo(1);
        assertThat(result.info().nodes()).hasSize(1);
        var node = result.info().nodes().get(0);
        assertThat(node.kind()).isEqualTo("table");
        assertThat(node.reason()).isEqualTo("nestedBodyRows");
        assertThat(node.directRows()).isEqualTo(6);
    }

    @Test
    void collapsesSingleAdfBodyRowWhenItFollowsCollapsedColumnHeaders() {
        String snapshot = """
                - table "Заголовки":
                  - rowgroup:
                    - row "ID Рег.номер Наименование":
                      - columnheader "ID"
                      - columnheader "Рег.номер"
                      - columnheader "Наименование"
                - table:
                  - rowgroup:
                    - row "1 8 324 3475 АЙСИБИСИ БАНК (АО)":
                      - cell "1"
                      - cell "8 324 3475 АЙСИБИСИ БАНК (АО)":
                        - table:
                          - rowgroup:
                            - row "8 324 3475 АЙСИБИСИ БАНК (АО)":
                              - cell "8 324"
                              - cell "3475"
                              - cell "АЙСИБИСИ БАНК (АО)"
                - table:
                  - rowgroup:
                    - row "Всего записей: 1":
                      - cell "Всего записей: 1\"""";

        var result = PlaywrightSessionManager.collapseSnapshot(snapshot, true);

        assertThat(result.snapshot().lines()).containsExactly(
                "- table \"Заголовки\": [3 column(s): ID, Рег.номер, Наименование - rows collapsed, call pageSnapshot with includeGrids=true to read rows]",
                "- table: [rows collapsed - call pageSnapshot with includeGrids=true to read columns and rows]",
                "- table:",
                "  - rowgroup:",
                "    - row \"Всего записей: 1\":",
                "      - cell \"Всего записей: 1\"");
        assertThat(result.snapshot()).doesNotContain("АЙСИБИСИ");
        assertThat(result.info().collapsedCount()).isEqualTo(2);
        assertThat(result.info().nodes()).extracting("reason")
                .containsExactly("columnHeaders", "nestedBodyRows");
        assertThat(result.info().nodes().get(1).directRows()).isEqualTo(1);
    }

    @Test
    void keepsSingleNestedLayoutTableExpandedWhenItDoesNotFollowColumnHeaders() {
        String snapshot = """
                - table:
                  - rowgroup:
                    - row "Данные Операции Отчеты Настройки":
                      - cell "Данные Операции Отчеты Настройки":
                        - table:
                          - rowgroup:
                            - row "Данные Операции Отчеты Настройки":
                              - cell "Данные"
                              - cell "Операции"
                              - cell "Отчеты"
                              - cell "Настройки\"""";

        assertThat(PlaywrightSessionManager.collapseGrids(snapshot)).isEqualTo(snapshot);
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
    void canDisableSnapshotCollapse() {
        String snapshot = """
                - table:
                  - rowgroup:
                    - row "ID Наименование":
                      - columnheader "ID"
                      - columnheader "Наименование"
                    - row "1 Альфа":
                      - cell "1"
                      - cell "Альфа\"""";

        var result = PlaywrightSessionManager.collapseSnapshot(snapshot, false);

        assertThat(result.snapshot()).isEqualTo(snapshot);
        assertThat(result.info().enabled()).isFalse();
        assertThat(result.info().collapsedCount()).isZero();
        assertThat(result.info().nodes()).isEmpty();
    }

    @Test
    void leavesSnapshotWithoutGridsUnchanged() {
        String snapshot = """
                - button "Меню"
                - text: Привет""";

        assertThat(PlaywrightSessionManager.collapseGrids(snapshot)).isEqualTo(snapshot);
    }
}
