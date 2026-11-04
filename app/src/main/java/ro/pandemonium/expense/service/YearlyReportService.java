package ro.pandemonium.expense.service;

import static java.time.Month.DECEMBER;
import static java.time.Month.JANUARY;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summingDouble;
import static ro.pandemonium.expense.model.ExpenseType.SPECIAL;
import static ro.pandemonium.expense.service.YearlyReportState.PeriodMode.FULL_YEAR;
import static ro.pandemonium.expense.service.YearlyReportState.PeriodMode.YEAR_FROM_DATE;
import static ro.pandemonium.expense.service.YearlyReportState.PeriodMode.YEAR_TO_DATE;
import static ro.pandemonium.expense.util.ExpenseMathUtil.variation;

import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.scopes.ActivityScoped;
import ro.pandemonium.expense.db.ExpenseDao;
import ro.pandemonium.expense.model.Expense;
import ro.pandemonium.expense.model.ExpenseType;
import ro.pandemonium.expense.service.YearlyReportState.PeriodMode;

@ActivityScoped
public final class YearlyReportService {

    private final ExpenseDao expenseDao;
    private final Clock clock;

    private boolean initialized;

    private int year;
    private LocalDate earliestEntryDate;
    private LocalDate latestEntryDate;
    private int earliestEntryYear;
    private int latestEntryYear;
    private PeriodMode periodMode;

    @Inject
    public YearlyReportService(final ExpenseDao expenseDao) {
        this(expenseDao, Clock.systemDefaultZone());
    }

    YearlyReportService(final ExpenseDao expenseDao, final Clock clock) {
        this.expenseDao = requireNonNull(expenseDao);
        this.clock = requireNonNull(clock);
    }

    public synchronized YearlyReportState initialize(final int requestedYear) {
        final LocalDate today = LocalDate.now(clock);
        earliestEntryDate = expenseDao.getEarliestEntry()
                .map(Expense::getDate)
                .orElse(today);
        latestEntryDate = expenseDao.getLatestEntry()
                .map(Expense::getDate)
                .orElse(today);
        earliestEntryYear = earliestEntryDate.getYear();
        latestEntryYear = latestEntryDate.getYear();
        final PeriodMode initialPeriodMode = requestedYear == latestEntryYear
                ? YEAR_TO_DATE
                : FULL_YEAR;

        return load(requestedYear, initialPeriodMode);
    }

    public synchronized YearlyReportState nextYear() {
        requireInitialized();
        final int requestedYear = year < latestEntryYear ? year + 1 : year;
        final PeriodMode requestedPeriodMode = requestedYear == year ? periodMode : FULL_YEAR;
        return load(requestedYear, requestedPeriodMode);
    }

    public synchronized YearlyReportState previousYear() {
        requireInitialized();
        final int requestedYear = (year - 1) > earliestEntryYear ? year - 1 : year;
        final PeriodMode requestedPeriodMode = requestedYear == year ? periodMode : FULL_YEAR;
        return load(requestedYear, requestedPeriodMode);
    }

    public synchronized YearlyReportState toggleComparisonPeriod() {
        requireInitialized();
        final PeriodMode requestedPeriodMode;
        if ((year - 1) == earliestEntryYear) {
            requestedPeriodMode = periodMode == YEAR_FROM_DATE
                    ? FULL_YEAR
                    : YEAR_FROM_DATE;
        } else if (year == latestEntryYear) {
            requestedPeriodMode = periodMode == YEAR_TO_DATE
                    ? FULL_YEAR
                    : YEAR_TO_DATE;
        } else {
            requestedPeriodMode = FULL_YEAR;
        }
        return load(year, requestedPeriodMode);
    }

    private YearlyReportState load(final int requestedYear, final PeriodMode requestedPeriodMode) {
        final YearlyReportState state = createReport(requestedYear, requestedPeriodMode);
        year = requestedYear;
        periodMode = requestedPeriodMode;
        initialized = true;
        return state;
    }

    private YearlyReportState createReport(final int reportYear, final PeriodMode reportPeriodMode) {
        final LocalDate currentStart = reportPeriodMode == YEAR_FROM_DATE
                ? earliestEntryDate.plusYears(1)
                : LocalDate.of(reportYear, JANUARY, 1);
        final LocalDate currentEnd = reportPeriodMode == YEAR_TO_DATE
                ? latestEntryDate
                : LocalDate.of(reportYear, DECEMBER, 31);
        final LocalDate previousStart = reportPeriodMode == YEAR_FROM_DATE
                ? earliestEntryDate
                : LocalDate.of(reportYear - 1, JANUARY, 1);
        final LocalDate previousEnd = reportPeriodMode == YEAR_TO_DATE
                ? latestEntryDate.minusYears(1)
                : LocalDate.of(reportYear - 1, DECEMBER, 31);

        final List<Expense> currentExpenses = expenseDao.findExpensesInInterval(currentStart, currentEnd);
        final List<Expense> previousExpenses = expenseDao.findExpensesInInterval(previousStart, previousEnd);
        final Map<ExpenseType, Double> currentTotals = totalsByExpenseType(currentExpenses);
        final Map<ExpenseType, Double> previousTotals = totalsByExpenseType(previousExpenses);
        final Map<ExpenseType, Comparison> comparisons = new EnumMap<>(ExpenseType.class);

        for (ExpenseType expenseType : ExpenseType.values()) {
            comparisons.put(expenseType, comparison(
                    currentTotals.get(expenseType),
                    previousTotals.get(expenseType)
                )
            );
        }

        return new YearlyReportState(
                reportYear,
                reportPeriodMode,
                (reportYear - 1) > earliestEntryYear,
                reportYear < latestEntryYear,
                comparison(total(currentExpenses, true), total(previousExpenses, true)),
                comparison(total(currentExpenses, false), total(previousExpenses, false)),
                comparisons
        );
    }

    private Map<ExpenseType, Double> totalsByExpenseType(final List<Expense> expenses) {
        final Map<ExpenseType, Double> totals = expenses.stream()
                .collect(
                    groupingBy(
                        Expense::getExpenseType,
                        () -> new EnumMap<>(ExpenseType.class),
                        summingDouble(Expense::getValue)
                    )
                );
        for (ExpenseType expenseType : ExpenseType.values()) {
            totals.putIfAbsent(expenseType, 0D);
        }
        return totals;
    }

    private double total(final List<Expense> expenses, final boolean includeSpecial) {
        return expenses.stream()
                .filter(expense -> includeSpecial || expense.getExpenseType() != SPECIAL)
                .mapToDouble(Expense::getValue)
                .sum();
    }

    private Comparison comparison(final double currentValue, final double previousValue) {
        return new Comparison(
                currentValue,
                previousValue,
                variation(currentValue, previousValue)
        );
    }

    private void requireInitialized() {
        if (!initialized) {
            throw new IllegalStateException("Yearly report service has not been initialized");
        }
    }
}
