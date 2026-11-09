package ro.pandemonium.expense.service;

import static java.util.Objects.requireNonNull;

import java.time.YearMonth;
import java.util.List;

import ro.pandemonium.expense.model.Expense;

public record ExpenseMainState(YearMonth yearMonth, List<Expense> expenses) {

    public ExpenseMainState {
        requireNonNull(yearMonth);

        expenses = List.copyOf(requireNonNull(expenses));
    }
}
