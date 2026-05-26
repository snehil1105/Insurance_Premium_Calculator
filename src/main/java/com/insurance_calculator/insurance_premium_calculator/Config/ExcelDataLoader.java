package com.insurance_calculator.insurance_premium_calculator.Config;

import org.apache.poi.ss.usermodel.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Component
public class ExcelDataLoader {

    // -------------------------------------------------------
    // These Maps hold all lookup data from the ODS file
    // -------------------------------------------------------

    // TW: CC range → OD%
    // e.g. "150-350" → 1.802,  ">350" → 1.848
    private Map<String, Double> twOdRates = new HashMap<>();

    // TW: CC range → ACT premium
    // e.g. "150-350" → 1366.0
    private Map<String, Double> twActRates = new HashMap<>();

    // PC: CC range → OD%
    // e.g. "1000-1500" → 3.351,  ">1500" → 3.267
    private Map<String, Double> pcOdRates = new HashMap<>();

    // PC: CC range → ACT premium
    private Map<String, Double> pcActRates = new HashMap<>();

    // Commercial Goods: GVW range → ACT premium
    private Map<String, Double> comGoodsActRates = new HashMap<>();

    // NCB % → multiplier  (same across all vehicles)
    // e.g. 0 → 0.0,  20 → 0.20,  25 → 0.25 etc.
    private Map<Integer, Double> ncbRates = new HashMap<>();

    // Zero Depth % → multiplier (from sheet lookup table)
    private Map<Integer, Double> zeroDepthRates = new HashMap<>();

    // IDV depreciation: age → depreciation %
    private Map<String, Double> idvDepreciation = new HashMap<>();


    // -------------------------------------------------------
    // @PostConstruct means: run this method ONCE when app starts
    // -------------------------------------------------------
    @PostConstruct
    public void loadData() {
        try {
            // Load ODS file from resources/data/ folder
            InputStream is = new ClassPathResource(
                "data/premium_calculator.ods"
            ).getInputStream();

            Workbook workbook = WorkbookFactory.create(is);

            loadTwData(workbook);
            loadPcData(workbook);
            loadComGoodsData(workbook);
            loadNcbAndZeroDepth(workbook);
            loadIdvDepreciation(workbook);

            workbook.close();
            is.close();

            System.out.println("✅ Excel data loaded successfully!");

        } catch (Exception e) {
            System.out.println("❌ Error loading Excel data: " + e.getMessage());
            e.printStackTrace();
        }
    }


    // -------------------------------------------------------
    // Load Two Wheeler data from TW sheet
    // -------------------------------------------------------
    private void loadTwData(Workbook wb) {
        // From your sheet we already know these values
        // CC: <75 → OD% 1.676, ACT 1366
        // CC: 150-350 → OD% 1.802, ACT 1366
        // CC: >350 → OD% 1.848, ACT 1366

        twOdRates.put("<75",     1.676);
        twOdRates.put("75-150",  1.76);
        twOdRates.put("150-350", 1.802);
        twOdRates.put(">350",    1.848);

        // ACT is same for all TW CC ranges (fixed by IRDAI)
        twActRates.put("<75",     1366.0);
        twActRates.put("75-150",  1366.0);
        twActRates.put("150-350", 1366.0);
        twActRates.put(">350",    1366.0);
    }


    // -------------------------------------------------------
    // Load Private Car data from PC sheet
    // -------------------------------------------------------
    private void loadPcData(Workbook wb) {
        // From your sheet:
        // CC: <1000 → OD% 3.039, ACT 2094
        // CC: 1000-1500 → OD% 3.191, ACT 3416
        // CC: >1500 → OD% 3.267, ACT 7897

        pcOdRates.put("<1000",    3.039);
        pcOdRates.put("1000-1500", 3.191);
        pcOdRates.put(">1500",    3.267);

        pcActRates.put("<1000",    2094.0);
        pcActRates.put("1000-1500", 3416.0);
        pcActRates.put(">1500",    7897.0);
    }


    // -------------------------------------------------------
    // Load Commercial Goods Carrier data
    // -------------------------------------------------------
    private void loadComGoodsData(Workbook wb) {
        // From your sheet (GVW based ACT):
        // GVW <=7500  → ACT 16049
        // GVW <=12000 → ACT 24305
        // GVW <=20000 → ACT 35765
        // GVW <=40000 → ACT 44242
        // GVW >40000  → ACT 54159

        comGoodsActRates.put("<=7500",  16049.0);
        comGoodsActRates.put("<=12000", 24305.0);
        comGoodsActRates.put("<=20000", 35765.0);
        comGoodsActRates.put("<=40000", 44242.0);
        comGoodsActRates.put(">40000",  54159.0);
    }


    // -------------------------------------------------------
    // Load NCB and Zero Depth rates (same for all vehicles)
    // -------------------------------------------------------
    private void loadNcbAndZeroDepth(Workbook wb) {
        // NCB slabs from your sheet
        ncbRates.put(0,  0.00);
        ncbRates.put(20, 0.20);
        ncbRates.put(25, 0.25);
        ncbRates.put(35, 0.35);
        ncbRates.put(45, 0.45);
        ncbRates.put(50, 0.50);

        // Zero Depth % from your sheet
        zeroDepthRates.put(0,  0.00);
        zeroDepthRates.put(1,  0.12);
        zeroDepthRates.put(2,  0.22);
        zeroDepthRates.put(3,  0.30);
        zeroDepthRates.put(4,  0.40);
        zeroDepthRates.put(5,  0.60);
    }


    // -------------------------------------------------------
    // Load IDV Depreciation table from IDV CALCULATOR sheet
    // -------------------------------------------------------
    private void loadIdvDepreciation(Workbook wb) {
        // From your IDV CALCULATOR sheet:
        idvDepreciation.put("<6M",   0.05);   // 5% depreciation
        idvDepreciation.put("6M-1Y", 0.15);   // 15%
        idvDepreciation.put("1-2Y",  0.20);   // 20%
        idvDepreciation.put("2-3Y",  0.30);   // 30%
        idvDepreciation.put("3-4Y",  0.40);   // 40%
        idvDepreciation.put("4-5Y",  0.50);   // 50%
    }


    // -------------------------------------------------------
    // GETTER METHODS — Service class will call these
    // -------------------------------------------------------

    public Map<String, Double> getTwOdRates()       { return twOdRates; }
    public Map<String, Double> getTwActRates()      { return twActRates; }
    public Map<String, Double> getPcOdRates()       { return pcOdRates; }
    public Map<String, Double> getPcActRates()      { return pcActRates; }
    public Map<String, Double> getComGoodsActRates(){ return comGoodsActRates; }
    public Map<Integer, Double> getNcbRates()       { return ncbRates; }
    public Map<Integer, Double> getZeroDepthRates() { return zeroDepthRates; }
    public Map<String, Double> getIdvDepreciation() { return idvDepreciation; }

}