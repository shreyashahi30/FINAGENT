package com.finagent;

import com.finagent.model.CompanyFinancialProfile;
import com.finagent.service.DatasetService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises DatasetService against small, hand-built SEC Financial Statement Data Set files
 * (num.txt / sub.txt) instead of a real multi-gigabyte SEC quarterly package. This is the one
 * part of the app that never had a test: everything else was covered indirectly through the
 * decision-rule tests, but the tab-delimited parsing, "most recent filing per company" merge,
 * and the balance-sheet-identity fallback for Liabilities had no coverage at all.
 */
class DatasetServiceTests {

    @TempDir
    Path tempDir;

    private static final String SUB_HEADER = "adsh\tcik\tname\tform\tperiod\tfiled";
    private static final String NUM_HEADER = "adsh\ttag\tddate\tqtrs\tuom\tsegments\tvalue";

    @Test
    void parsesACompanyWithAFullSetOfTags(@TempDir Path dir) throws Exception {
        DatasetService dataset = new DatasetService(dir);

        String sub = String.join("\n", SUB_HEADER,
                "0001-01\t0000320193\tTest Corp\t10-K\t20251231\t20260201") + "\n";
        String num = String.join("\n", NUM_HEADER,
                "0001-01\tAssets\t20251231\t0\tUSD\t\t1000000",
                "0001-01\tAssetsCurrent\t20251231\t0\tUSD\t\t400000",
                "0001-01\tLiabilitiesCurrent\t20251231\t0\tUSD\t\t200000",
                "0001-01\tStockholdersEquity\t20251231\t0\tUSD\t\t600000",
                "0001-01\tRevenues\t20251231\t4\tUSD\t\t900000",
                "0001-01\tOperatingIncomeLoss\t20251231\t4\tUSD\t\t90000",
                "0001-01\tLongTermDebtNoncurrent\t20251231\t0\tUSD\t\t150000") + "\n";

        Map<String, Object> status = dataset.upload(new org.springframework.web.multipart.MultipartFile[]{
                multipart("sub.txt", sub), multipart("num.txt", num)});

        assertEquals(Boolean.TRUE, status.get("loaded"));
        assertEquals(1, status.get("companyCount"));

        List<CompanyFinancialProfile> found = dataset.search("Test Corp", 10);
        assertEquals(1, found.size());

        CompanyFinancialProfile p = dataset.get("0000320193");
        assertNotNull(p);
        assertEquals("Test Corp", p.companyName);
        assertEquals(2.0, p.currentRatio, 0.0001);
        assertEquals(0.15, p.debtToAssets, 0.0001);
        assertEquals(10.0, p.operatingMargin, 0.0001);
        // Liabilities was never tagged directly; it must fall back to Assets - Equity = 400000,
        // not silently read as zero.
        assertEquals(400000.0, p.totalLiabilities, 0.0001);
        assertTrue(p.debtDataAvailable);
        assertTrue(p.liquidityDataAvailable);
        assertTrue(p.marginDataAvailable);
    }

    @Test
    void aCompanyWithNoDebtTagsIsFlaggedAsMissingNotAsZeroDebt() throws Exception {
        DatasetService dataset = new DatasetService(tempDir);

        String sub = String.join("\n", SUB_HEADER,
                "0002-01\t0000111111\tNo Debt Tags Inc\t10-K\t20251231\t20260201") + "\n";
        // Deliberately omit every LongTermDebt*/ShortTerm* tag.
        String num = String.join("\n", NUM_HEADER,
                "0002-01\tAssets\t20251231\t0\tUSD\t\t500000",
                "0002-01\tAssetsCurrent\t20251231\t0\tUSD\t\t300000",
                "0002-01\tLiabilitiesCurrent\t20251231\t0\tUSD\t\t100000",
                "0002-01\tStockholdersEquity\t20251231\t0\tUSD\t\t450000",
                "0002-01\tRevenues\t20251231\t4\tUSD\t\t700000",
                "0002-01\tOperatingIncomeLoss\t20251231\t4\tUSD\t\t70000") + "\n";

        dataset.upload(new org.springframework.web.multipart.MultipartFile[]{
                multipart("sub.txt", sub), multipart("num.txt", num)});

        CompanyFinancialProfile p = dataset.get("0000111111");
        assertNotNull(p);
        assertEquals(0.0, p.debt, 0.0001);
        // The key assertion: debt reads as 0 the same either way, but debtDataAvailable must
        // distinguish "no debt tags filed" from "debt tags filed and summed to zero".
        assertFalse(p.debtDataAvailable, "debt tags were never filed for this company; it must not be treated as a verified zero-debt company");
        assertTrue(p.liquidityDataAvailable);
        assertTrue(p.marginDataAvailable);
    }

    @Test
    void keepsOnlyTheMostRecentlyFiledSubmissionPerCompany() throws Exception {
        DatasetService dataset = new DatasetService(tempDir);

        String sub = String.join("\n", SUB_HEADER,
                "0003-01\t0000222222\tDup Corp\t10-Q\t20250331\t20250501",
                "0003-02\t0000222222\tDup Corp\t10-K\t20251231\t20260210") + "\n";
        String num = String.join("\n", NUM_HEADER,
                "0003-01\tAssets\t20250331\t0\tUSD\t\t100",
                "0003-01\tAssetsCurrent\t20250331\t0\tUSD\t\t50",
                "0003-01\tLiabilitiesCurrent\t20250331\t0\tUSD\t\t25",
                "0003-02\tAssets\t20251231\t0\tUSD\t\t9999999",
                "0003-02\tAssetsCurrent\t20251231\t0\tUSD\t\t400000",
                "0003-02\tLiabilitiesCurrent\t20251231\t0\tUSD\t\t200000") + "\n";

        dataset.upload(new org.springframework.web.multipart.MultipartFile[]{
                multipart("sub.txt", sub), multipart("num.txt", num)});

        assertEquals(1, dataset.status().get("companyCount"));
        CompanyFinancialProfile p = dataset.get("0000222222");
        assertNotNull(p);
        assertEquals(9999999.0, p.totalAssets, 0.0001);
    }

    @Test
    void rejectsUploadMissingRequiredFiles() {
        DatasetService dataset = new DatasetService(tempDir);
        assertThrows(java.io.IOException.class, () -> dataset.upload(
                new org.springframework.web.multipart.MultipartFile[]{multipart("sub.txt", SUB_HEADER + "\n")}));
    }

    private MockMultipartFile multipart(String filename, String content) {
        return new MockMultipartFile("files", filename, "text/plain", content.getBytes(StandardCharsets.UTF_8));
    }
}
