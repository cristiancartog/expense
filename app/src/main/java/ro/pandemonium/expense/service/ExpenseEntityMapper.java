package ro.pandemonium.expense.service;

import static ro.pandemonium.expense.util.CollectionUtil.convertList;

import java.util.List;

import ro.pandemonium.expense.db.ExpenseEntity;
import ro.pandemonium.expense.model.Expense;

final class ExpenseEntityMapper {

    private ExpenseEntityMapper() {
    }

    static ExpenseEntity toEntity(final Expense expense, final boolean includeId) {
        final ExpenseEntity entity = new ExpenseEntity();
        entity.id = includeId ? expense.getId() : null;
        entity.expenseType = expense.getExpenseType();
        entity.value = expense.getValue();
        entity.date = expense.getDate();
        entity.comment = expense.getComment();
        return entity;
    }

    static Expense toExpense(final ExpenseEntity entity) {
        final Expense expense = new Expense();
        expense.setId(entity.id);
        expense.setExpenseType(entity.expenseType);
        expense.setValue(entity.value);
        expense.setDate(entity.date);
        expense.setComment(entity.comment);
        return expense;
    }

    static List<Expense> toExpenses(final List<ExpenseEntity> entities) {
        return convertList(entities, ExpenseEntityMapper::toExpense);
    }
}
