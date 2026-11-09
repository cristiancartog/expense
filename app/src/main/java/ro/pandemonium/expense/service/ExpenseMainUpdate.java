package ro.pandemonium.expense.service;

import static java.util.Objects.requireNonNull;

import ro.pandemonium.expense.model.Expense;

public record ExpenseMainUpdate(Expense expense, OperationType operationType, boolean clearFilters) {

    public ExpenseMainUpdate {
        requireNonNull(expense);
        requireNonNull(operationType);
    }

    public enum OperationType {
        ADD,
        UPDATE,
        REMOVE,
        NONE
    }
}
