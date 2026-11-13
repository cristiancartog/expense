package ro.pandemonium.expense.service;

import static java.util.Objects.requireNonNull;
import static ro.pandemonium.expense.util.ExpenseMathUtil.ZERO;
import static ro.pandemonium.expense.util.ExpenseMathUtil.variationPercentage;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import javax.inject.Inject;

import dagger.hilt.android.scopes.ActivityScoped;
import ro.pandemonium.expense.db.ExpenseDao;
import ro.pandemonium.expense.db.YearSummaryRow;
import ro.pandemonium.expense.model.ExpenseType;

@ActivityScoped
public final class YearComparisonService {

    private final ExpenseDao expenseDao;
    private final Clock clock;

    private boolean initialized;
    private ExpenseType expenseType;
    private int year;
    private int earliestYear;
    private int latestYear;

    @Inject
    public YearComparisonService(final ExpenseDao expenseDao) {
        this(expenseDao, Clock.systemDefaultZone());
    }

    YearComparisonService(final ExpenseDao expenseDao, final Clock clock) {
        this.expenseDao = requireNonNull(expenseDao);
        this.clock = requireNonNull(clock);
    }

    public YearComparisonState initialize(final ExpenseType requestedExpenseType, final int requestedYear) {
        requireNonNull(requestedExpenseType);
        final int currentYear = LocalDate.now(clock).getYear();
        final int requestedEarliestYear = Optional.ofNullable(expenseDao.getEarliestEntry())
                .map(entity -> entity.date)
                .map(LocalDate::getYear)
                .orElse(currentYear);
        final int requestedLatestYear = Optional.ofNullable(expenseDao.getLatestEntry())
                .map(entity -> entity.date)
                .map(LocalDate::getYear)
                .orElse(currentYear);

        final YearComparisonState state = createState(
                requestedExpenseType,
                requestedYear,
                requestedEarliestYear,
                requestedLatestYear
        );
        expenseType = requestedExpenseType;
        year = requestedYear;
        earliestYear = requestedEarliestYear;
        latestYear = requestedLatestYear;
        initialized = true;
        return state;
    }

    public YearComparisonState nextYear() {
        requireInitialized();
        final int requestedYear = year < latestYear ? year + 1 : year;
        return load(requestedYear);
    }

    public YearComparisonState previousYear() {
        requireInitialized();
        final int requestedYear = (year - 1) > earliestYear ? year - 1 : year;
        return load(requestedYear);
    }

    private YearComparisonState load(final int requestedYear) {
        final YearComparisonState state = createState(expenseType, requestedYear, earliestYear, latestYear);
        year = requestedYear;
        return state;
    }

    private YearComparisonState createState(final ExpenseType reportExpenseType,
                                            final int reportYear,
                                            final int firstExpenseYear,
                                            final int lastExpenseYear) {
        final Map<String, BigDecimal> currentValues = monthlyValues(reportExpenseType, reportYear);
        final Map<String, BigDecimal> previousValues = monthlyValues(reportExpenseType, reportYear - 1);
        final Map<Month, Comparison> comparisons = new EnumMap<>(Month.class);
        BigDecimal currentTotal = ZERO;
        BigDecimal previousTotal = ZERO;

        for (Month month : Month.values()) {
            final String monthKey = "%02d".formatted(month.getValue());
            final BigDecimal currentValue = currentValues.getOrDefault(monthKey, ZERO);
            final BigDecimal previousValue = previousValues.getOrDefault(monthKey, ZERO);
            currentTotal = currentTotal.add(currentValue);
            previousTotal = previousTotal.add(previousValue);
            comparisons.put(month, new Comparison(
                    currentValue,
                    previousValue,
                    variationPercentage(currentValue, previousValue)
            ));
        }

        return new YearComparisonState(
                reportExpenseType,
                reportYear,
                (reportYear - 1) > firstExpenseYear,
                reportYear < lastExpenseYear,
                currentTotal,
                previousTotal,
                comparisons
        );
    }

    private Map<String, BigDecimal> monthlyValues(final ExpenseType reportExpenseType, final int reportYear) {
        final Map<String, BigDecimal> values = new TreeMap<>();
        for (YearSummaryRow row : expenseDao.getMonthlySummaryInYear(reportExpenseType.getDbId(), Integer.toString(reportYear))) {
            values.put(row.month, row.total);
        }
        return values;
    }

    private void requireInitialized() {
        if (!initialized) {
            throw new IllegalStateException("Year comparison service has not been initialized");
        }
    }
}
