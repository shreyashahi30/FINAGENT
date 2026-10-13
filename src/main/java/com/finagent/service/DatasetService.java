
package com.finagent.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finagent.model.CompanyFinancialProfile;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

@Service
public class DatasetService {

    private final Map<String, CompanyFinancialProfile> companies =
            new LinkedHashMap<>();

    private volatile boolean loaded = false;
    private volatile String datasetStatus = "No SEC dataset loaded.";
    private volatile int companyCount = 0;

    private final Path dataDir;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String BUNDLED_PROFILES =
            "sec/company-profiles.json";

    public DatasetService() {
        this(Paths.get("data", "sec-dataset"));
    }

    /**
     * Visible so tests can use an isolated temporary directory.
     */
    public DatasetService(Path dataDir) {
        this.dataDir = dataDir;
    }

    private static final List<String> TAGS = List.of(
            "RevenueFromContractWithCustomerExcludingAssessedTax",
            "Revenues",
            "SalesRevenueNet",
            "OperatingIncomeLoss",
            "NetIncomeLoss",
            "ProfitLoss",
            "CashAndCashEquivalentsAtCarryingValue",
            "CashCashEquivalentsRestrictedCashAndRestrictedCashEquivalents",
            "Assets",
            "AssetsCurrent",
            "Liabilities",
            "LiabilitiesCurrent",
            "StockholdersEquity",
            "StockholdersEquityIncludingPortionAttributableToNoncontrollingInterest",
            "AccountsReceivableNetCurrent",
            "InventoryNet",
            "NetCashProvidedByUsedInOperatingActivities",
            "LongTermDebtCurrent",
            "LongTermDebtNoncurrent",
            "LongTermDebt",
            "LongTermDebtAndFinanceLeaseObligationsCurrent",
            "LongTermDebtAndFinanceLeaseObligationsNoncurrent",
            "ShortTermBorrowings",
            "ShortTermDebt"
    );

    /**
     * Load preprocessed profiles when the application starts.
     * If no bundled export exists, the service falls back to the
     * original local SEC dataset directory.
     */
    @PostConstruct
    public synchronized void initializeDataset() {
        if (loaded) {
            return;
        }

        try {
            ClassPathResource resource =
                    new ClassPathResource(BUNDLED_PROFILES);

            if (resource.exists()) {
                try (InputStream input = resource.getInputStream()) {
                    List<CompanyFinancialProfile> profiles =
                            objectMapper.readValue(
                                    input,
                                    new TypeReference<
                                            List<CompanyFinancialProfile>>() {}
                            );

                    installProfiles(profiles);

                    datasetStatus =
                            "Preprocessed SEC dataset loaded successfully. "
                                    + companyCount
                                    + " company records are available.";

                    return;
                }
            }
        } catch (Exception e) {
            datasetStatus =
                    "Bundled SEC dataset could not be loaded: "
                            + e.getMessage();
        }

        loadIfPresent();
    }

    /**
     * Export the profiles produced by the existing SEC parser.
     *
     * Run this locally after the original SEC dataset has been loaded.
     * The resulting file can then be copied to:
     * src/main/resources/sec/company-profiles.json
     */
    public synchronized void exportProfiles(Path outputFile)
            throws IOException {

        loadIfPresent();

        if (!loaded || companies.isEmpty()) {
            throw new IOException(
                    "No SEC company profiles are loaded. "
                            + "Load the original dataset before exporting."
            );
        }

        Path absolute = outputFile.toAbsolutePath();
        Path parent = absolute.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(absolute.toFile(),
                        new ArrayList<>(companies.values()));
    }

    /**
     * Install exported profiles into the in-memory company index.
     */
    private synchronized void installProfiles(
            List<CompanyFinancialProfile> profiles) {

        Map<String, CompanyFinancialProfile> validated =
                new LinkedHashMap<>();

        for (CompanyFinancialProfile profile : profiles) {
            if (profile == null
                    || profile.cik == null
                    || profile.cik.isBlank()
                    || profile.companyName == null
                    || profile.companyName.isBlank()) {
                continue;
            }

            if (!Double.isFinite(profile.revenue)
                    || !Double.isFinite(profile.operatingProfit)
                    || !Double.isFinite(profile.cash)
                    || !Double.isFinite(profile.debt)
                    || !Double.isFinite(profile.totalAssets)
                    || !Double.isFinite(profile.totalLiabilities)
                    || !Double.isFinite(profile.equity)
                    || !Double.isFinite(profile.currentAssets)
                    || !Double.isFinite(profile.currentLiabilities)
                    || !Double.isFinite(profile.netIncome)
                    || !Double.isFinite(profile.operatingCashFlow)
                    || !Double.isFinite(profile.receivables)
                    || !Double.isFinite(profile.inventory)
                    || !Double.isFinite(profile.currentRatio)
                    || !Double.isFinite(profile.debtToAssets)
                    || !Double.isFinite(profile.operatingMargin)) {
                continue;
            }

            validated.put(profile.cik, profile);
        }

        if (validated.isEmpty()) {
            throw new IllegalArgumentException(
                    "The preprocessed SEC file contains no valid profiles."
            );
        }

        companies.clear();
        companies.putAll(validated);
        companyCount = companies.size();
        loaded = true;
    }

    public synchronized Map<String, Object> upload(
            MultipartFile[] files) throws IOException {

        if (files == null || files.length == 0) {
            throw new IOException(
                    "Upload the unzipped SEC dataset files."
            );
        }

        Files.createDirectories(dataDir);

        for (MultipartFile file : files) {
            String name = Paths.get(
                    Objects.requireNonNullElse(
                            file.getOriginalFilename(), "")
            ).getFileName().toString().toLowerCase(Locale.ROOT);

            if (List.of(
                    "num.txt", "sub.txt", "tag.txt",
                    "pre.txt", "readme.htm"
            ).contains(name)) {
                try (InputStream input = file.getInputStream()) {
                    Files.copy(
                            input,
                            dataDir.resolve(name),
                            StandardCopyOption.REPLACE_EXISTING
                    );
                }
            }
        }

        if (!Files.exists(dataDir.resolve("num.txt"))
                || !Files.exists(dataDir.resolve("sub.txt"))) {
            throw new IOException(
                    "The unzipped dataset must contain at least "
                            + "num.txt and sub.txt."
            );
        }

        loadFromDirectory();
        return status();
    }

    public synchronized void loadIfPresent() {
        if (loaded) {
            return;
        }

        try {
            if (Files.exists(dataDir.resolve("num.txt"))
                    && Files.exists(dataDir.resolve("sub.txt"))) {
                loadFromDirectory();
            }
        } catch (Exception e) {
            datasetStatus =
                    "Dataset found but could not be loaded: "
                            + e.getMessage();
        }
    }

    private void loadFromDirectory() throws IOException {
        Map<String, Filing> filings =
                readSub(dataDir.resolve("sub.txt"));

        Map<String, Map<String, Double>> factsByAdsh =
                readNum(dataDir.resolve("num.txt"), filings);

        Map<String, CompanyFinancialProfile> result =
                new LinkedHashMap<>();

        for (Filing filing : filings.values()) {
            Map<String, Double> facts =
                    factsByAdsh.get(filing.adsh);

            if (facts == null || facts.isEmpty()) {
                continue;
            }

            CompanyFinancialProfile profile =
                    profile(filing, facts);

            if (profile.revenue == 0
                    && profile.totalAssets == 0
                    && profile.totalLiabilities == 0) {
                continue;
            }

            CompanyFinancialProfile old =
                    result.get(profile.cik);

            if (old == null
                    || safe(profile.filed)
                    .compareTo(safe(old.filed)) > 0) {
                result.put(profile.cik, profile);
            }
        }

        companies.clear();
        companies.putAll(result);
        companyCount = companies.size();
        loaded = true;

        datasetStatus =
                "SEC dataset loaded successfully. "
                        + companyCount
                        + " company records are available.";
    }

    private Map<String, Filing> readSub(Path path)
            throws IOException {

        Map<String, Filing> out = new HashMap<>();

        try (BufferedReader br = Files.newBufferedReader(
                path, StandardCharsets.UTF_8)) {

            String line = br.readLine();

            if (line == null) {
                return out;
            }

            Map<String, Integer> idx = indexes(split(line));
            String row;

            while ((row = br.readLine()) != null) {
                String[] a = split(row);

                String adsh = get(a, idx, "adsh");
                String cik = get(a, idx, "cik");
                String name = get(a, idx, "name");
                String filed = get(a, idx, "filed");

                if (adsh.isBlank()
                        || cik.isBlank()
                        || name.isBlank()) {
                    continue;
                }

                String form =
                        get(a, idx, "form").toUpperCase(Locale.ROOT);

                if (!(form.equals("10-K")
                        || form.equals("10-Q")
                        || form.equals("20-F")
                        || form.equals("40-F"))) {
                    continue;
                }

                Filing filing = new Filing(
                        adsh, cik, name, form, filed,
                        get(a, idx, "period")
                );

                Filing old = out.get(adsh);

                if (old == null
                        || safe(filing.filed)
                        .compareTo(safe(old.filed)) >= 0) {
                    out.put(adsh, filing);
                }
            }
        }

        return out;
    }

    private Map<String, Map<String, Double>> readNum(
            Path path, Map<String, Filing> filings)
            throws IOException {

        Map<String, Map<String, Double>> out = new HashMap<>();
        Map<String, Map<String, FactKey>> chosen =
                new HashMap<>();

        try (BufferedReader br = Files.newBufferedReader(
                path, StandardCharsets.UTF_8)) {

            String line = br.readLine();

            if (line == null) {
                return out;
            }

            Map<String, Integer> idx = indexes(split(line));
            String row;

            while ((row = br.readLine()) != null) {
                String[] a = split(row);
                String tag = get(a, idx, "tag");

                if (!TAGS.contains(tag)) {
                    continue;
                }

                String adsh = get(a, idx, "adsh");
                String value = get(a, idx, "value");
                String ddate = get(a, idx, "ddate");
                String uom = get(a, idx, "uom");
                String segments = get(a, idx, "segments");
                String qtrs = get(a, idx, "qtrs");

                Filing filing = filings.get(adsh);

                if (filing == null
                        || value.isBlank()
                        || ddate.isBlank()) {
                    continue;
                }

                if (!"USD".equalsIgnoreCase(uom)) {
                    continue;
                }

                try {
                    double v = Double.parseDouble(value);

                    if (!Double.isFinite(v)) {
                        continue;
                    }

                    int d = Integer.parseInt(ddate);
                    int q = parseInt(qtrs);

                    FactKey now = new FactKey(
                            d, q, segments.isBlank(), v
                    );

                    Map<String, FactKey> facts =
                            chosen.computeIfAbsent(
                                    adsh, key -> new HashMap<>());

                    FactKey old = facts.get(tag);

                    if (old == null
                            || better(now, old, tag, filing)) {
                        facts.put(tag, now);
                    }
                } catch (NumberFormatException ignored) {
                    // Ignore malformed SEC rows.
                }
            }
        }

        for (Map.Entry<String, Map<String, FactKey>> entry
                : chosen.entrySet()) {

            Map<String, Double> facts = new HashMap<>();

            for (Map.Entry<String, FactKey> fact
                    : entry.getValue().entrySet()) {
                facts.put(fact.getKey(), fact.getValue().value);
            }

            out.put(entry.getKey(), facts);
        }

        return out;
    }

    private static final Set<String> INSTANT_TAGS = Set.of(
            "CashAndCashEquivalentsAtCarryingValue",
            "CashCashEquivalentsRestrictedCashAndRestrictedCashEquivalents",
            "Assets", "AssetsCurrent", "Liabilities",
            "LiabilitiesCurrent", "StockholdersEquity",
            "StockholdersEquityIncludingPortionAttributableToNoncontrollingInterest",
            "AccountsReceivableNetCurrent", "InventoryNet",
            "LongTermDebtCurrent", "LongTermDebtNoncurrent",
            "LongTermDebt",
            "LongTermDebtAndFinanceLeaseObligationsCurrent",
            "LongTermDebtAndFinanceLeaseObligationsNoncurrent",
            "ShortTermBorrowings", "ShortTermDebt"
    );

    private boolean better(
            FactKey now, FactKey old, String tag, Filing filing) {

        int targetDate = parseInt(filing.period);

        boolean nowExact = targetDate > 0 && now.date == targetDate;
        boolean oldExact = targetDate > 0 && old.date == targetDate;

        if (nowExact != oldExact) {
            return nowExact;
        }

        boolean nowNotFuture =
                targetDate <= 0 || now.date <= targetDate;
        boolean oldNotFuture =
                targetDate <= 0 || old.date <= targetDate;

        if (nowNotFuture != oldNotFuture) {
            return nowNotFuture;
        }

        if (now.consolidated != old.consolidated) {
            return now.consolidated;
        }

        boolean instant = INSTANT_TAGS.contains(tag);

        int wanted = instant ? 0
                : (filing.form.equals("10-K")
                || filing.form.equals("20-F")
                || filing.form.equals("40-F") ? 4 : 1);

        int nowDuration = durationRank(now.qtrs, wanted, instant);
        int oldDuration = durationRank(old.qtrs, wanted, instant);

        if (nowDuration != oldDuration) {
            return nowDuration < oldDuration;
        }

        if (now.date != old.date) {
            return now.date > old.date;
        }

        return false;
    }

    private int durationRank(
            int qtrs, int wanted, boolean instant) {

        if (qtrs < 0) {
            return 1000;
        }

        if (instant) {
            return qtrs == 0 ? 0 : 100 + qtrs;
        }

        if (qtrs == wanted) {
            return 0;
        }

        if (wanted == 1 && qtrs == 3) {
            return 10;
        }

        return 100 + Math.abs(qtrs - wanted);
    }

    private CompanyFinancialProfile profile(
            Filing filing, Map<String, Double> x) {

        CompanyFinancialProfile p =
                new CompanyFinancialProfile();

        p.cik = filing.cik;
        p.companyName = filing.name;
        p.accessionNumber = filing.adsh;
        p.form = filing.form;
        p.filed = filing.filed;
        p.period = filing.period;

        p.revenue = first(x,
                "RevenueFromContractWithCustomerExcludingAssessedTax",
                "Revenues", "SalesRevenueNet");

        p.operatingProfit = first(x, "OperatingIncomeLoss");
        p.netIncome = first(x, "NetIncomeLoss", "ProfitLoss");

        p.cash = first(x,
                "CashAndCashEquivalentsAtCarryingValue",
                "CashCashEquivalentsRestrictedCashAndRestrictedCashEquivalents");

        p.totalAssets = first(x, "Assets");
        p.totalLiabilities = first(x, "Liabilities");
        p.currentAssets = first(x, "AssetsCurrent");
        p.currentLiabilities = first(x, "LiabilitiesCurrent");

        p.equity = first(x,
                "StockholdersEquityIncludingPortionAttributableToNoncontrollingInterest",
                "StockholdersEquity");

        if (p.totalLiabilities == 0
                && p.totalAssets > 0
                && p.equity != 0) {
            p.totalLiabilities = p.totalAssets - p.equity;
        }

        p.receivables = first(x, "AccountsReceivableNetCurrent");
        p.inventory = first(x, "InventoryNet");

        p.operatingCashFlow =
                first(x, "NetCashProvidedByUsedInOperatingActivities");

        p.debt = first(x,
                "LongTermDebtCurrent",
                "LongTermDebtAndFinanceLeaseObligationsCurrent")
                + first(x,
                "LongTermDebtNoncurrent",
                "LongTermDebtAndFinanceLeaseObligationsNoncurrent",
                "LongTermDebt")
                + first(x, "ShortTermBorrowings", "ShortTermDebt");

        p.currentRatio =
                p.currentLiabilities > 0 && p.currentAssets >= 0
                        ? p.currentAssets / p.currentLiabilities : 0;

        p.debtToAssets =
                p.totalAssets > 0 && p.debt >= 0
                        ? p.debt / p.totalAssets : 0;

        p.operatingMargin =
                p.revenue > 0
                        ? p.operatingProfit / p.revenue * 100 : 0;

        p.debtDataAvailable = anyPresent(x,
                "LongTermDebtCurrent",
                "LongTermDebtAndFinanceLeaseObligationsCurrent",
                "LongTermDebtNoncurrent",
                "LongTermDebtAndFinanceLeaseObligationsNoncurrent",
                "LongTermDebt", "ShortTermBorrowings", "ShortTermDebt");

        p.liquidityDataAvailable =
                anyPresent(x, "AssetsCurrent")
                        && anyPresent(x, "LiabilitiesCurrent");

        p.marginDataAvailable =
                anyPresent(x,
                        "RevenueFromContractWithCustomerExcludingAssessedTax",
                        "Revenues", "SalesRevenueNet")
                        && anyPresent(x, "OperatingIncomeLoss");

        return p;
    }

    private double first(
            Map<String, Double> x, String... tags) {
        for (String tag : tags) {
            Double value = x.get(tag);
            if (value != null) {
                return value;
            }
        }
        return 0;
    }

    private boolean anyPresent(
            Map<String, Double> x, String... tags) {
        for (String tag : tags) {
            if (x.containsKey(tag)) {
                return true;
            }
        }
        return false;
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(
                    value == null ? "" : value.trim());
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private String[] split(String s) {
        return s.split("\\t", -1);
    }

    private Map<String, Integer> indexes(String[] header) {
        Map<String, Integer> result = new HashMap<>();

        for (int i = 0; i < header.length; i++) {
            result.put(header[i].trim().toLowerCase(), i);
        }

        return result;
    }

    private String get(
            String[] values, Map<String, Integer> idx, String key) {
        Integer i = idx.get(key);
        return i == null || i >= values.length
                ? "" : values[i].trim();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    public synchronized List<CompanyFinancialProfile> search(
            String q, int limit) {

        loadIfPresent();

        String query = q == null ? "" : q.trim().toLowerCase();
        List<CompanyFinancialProfile> out = new ArrayList<>();

        for (CompanyFinancialProfile p : companies.values()) {
            if (query.isBlank()
                    || p.companyName.toLowerCase().contains(query)
                    || p.cik.equals(query)) {
                out.add(p);

                if (out.size() >= Math.min(Math.max(limit, 1), 100)) {
                    break;
                }
            }
        }

        return out;
    }

    public CompanyFinancialProfile get(String cik) {
        loadIfPresent();
        return companies.get(cik);
    }

    public Map<String, Object> status() {
        return Map.of(
                "loaded", loaded,
                "companyCount", companyCount,
                "status", datasetStatus
        );
    }

    public boolean isLoaded() {
        loadIfPresent();
        return loaded;
    }

    private record Filing(
            String adsh, String cik, String name,
            String form, String filed, String period) {}

    private record FactKey(
            int date, int qtrs, boolean consolidated, double value) {}
}
