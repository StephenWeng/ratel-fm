package com.ratel.fm.service.knowledge;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 智能检索回归用例目录测试。
 *
 * <p>该测试先固化必须覆盖的业务检索场景，后续可在此基础上接入真实 H2/Qdrant 检索断言。</p>
 */
class KnowledgeSearchRegressionCatalogTest {

    @Test
    void regressionCatalogShouldCoverCoreBusinessModules() throws Exception {
        List<String> lines = readLines();
        assertThat(lines).hasSizeGreaterThan(8);
        assertThat(lines.get(0)).isEqualTo("caseCode,query,expectedTypes,expectedTokens");

        Set<String> caseCodes = new HashSet<>();
        Set<String> expectedTypes = new HashSet<>();
        for (String line : lines.subList(1, lines.size())) {
            String[] columns = line.split(",", -1);
            assertThat(columns).hasSize(4);
            assertThat(columns[0]).isNotBlank();
            assertThat(caseCodes).doesNotContain(columns[0]);
            caseCodes.add(columns[0]);
            assertThat(columns[1]).isNotBlank();
            assertThat(columns[2]).isNotBlank();
            assertThat(columns[3]).isNotBlank();
            expectedTypes.add(columns[2].trim());
        }

        assertThat(expectedTypes).contains(
                "BASIC_DICTIONARY",
                "SUBJECT",
                "VOUCHER",
                "PURCHASE_ORDER",
                "SHIPMENT",
                "INVENTORY_LEDGER",
                "AR_AP_BILL",
                "CASHIER_TRANSACTION",
                "ATTACHMENT",
                "SYSTEM_MODULE"
        );
    }

    private List<String> readLines() throws Exception {
        try (InputStream inputStream = getClass().getResourceAsStream("/ai-search-regression-cases.csv")) {
            assertThat(inputStream).isNotNull();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                return reader.lines()
                        .filter(line -> !line.isBlank())
                        .toList();
            }
        }
    }
}
