package ro.pandemonium.expense.model;

import java.util.Map;

public class YearlyReportResult {

    private Map<String, Double> currentYearData;
    private Map<String, Double> lastYearData;
    private Double totalCurrentYear;
    private Double totalLastYear;

    public YearlyReportResult(final Map<String, Double> currentYearData,
                              final Map<String, Double> lastYearData) {
        this.currentYearData = currentYearData;
        this.lastYearData = lastYearData;
        this.totalCurrentYear = computeTotal(currentYearData);
        this.totalLastYear = computeTotal(lastYearData);
    }

    private Double computeTotal(final Map<String, Double> dataMap) {
        double total = 0;
        for (Double value : dataMap.values()) {
            if (value != null) {
                total += value;
            }
        }
        return total;
    }

    public Map<String, Double> getCurrentYearData() {
        return currentYearData;
    }

    public Map<String, Double> getLastYearData() {
        return lastYearData;
    }

    public Double getTotalCurrentYear() {
        return totalCurrentYear;
    }

    public Double getTotalLastYear() {
        return totalLastYear;
    }
}
