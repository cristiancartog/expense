package ro.pandemonium.expense.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static ro.pandemonium.expense.model.ExpenseType.FOOD;
import static ro.pandemonium.expense.model.ExpenseType.PETROL;
import static ro.pandemonium.expense.model.ExpenseType.SPECIAL;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import ro.pandemonium.expense.model.ExpenseType;

@RunWith(AndroidJUnit4.class)
public class ExpenseDaoRoomTest {

    private Context context;
    private ExpenseDatabase database;
    private ExpenseDao expenseDao;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, ExpenseDatabase.class)
                       .allowMainThreadQueries()
                       .build();
        expenseDao = database.expenseDao();
    }

    @After
    public void tearDown() {
        if (database != null && database.isOpen()) {
            database.close();
        }
    }

    @Test
    public void performsCrudAndGeneratesIds() {
        final ExpenseEntity expense = entity(null, FOOD, 12.5, LocalDate.of(2026, 11, 9), "lunch");

        expense.id = expenseDao.insert(expense);
        assertTrue(expense.id > 0);

        expense.value = 18.0;
        assertEquals(1, expenseDao.update(expense));
        assertEquals(18.0, expenseDao.fetchAllExpenses().getFirst().value, 0.0);

        assertEquals(1, expenseDao.delete(expense.id));
        assertTrue(expenseDao.fetchAllExpenses().isEmpty());
    }

    @Test
    public void selectsMonthsIntervalsEdgesAndSummaries() {
        final ExpenseEntity octoberFood = persist(FOOD, 10, LocalDate.of(2025, 10, 2), "food");
        final ExpenseEntity novemberFood = persist(FOOD, 20, LocalDate.of(2025, 11, 3), "food");
        persist(PETROL, 30, LocalDate.of(2025, 11, 4), "petrol");
        persist(SPECIAL, 40, LocalDate.of(2025, 11, 5), "special");

        final List<ExpenseEntity> novemberExpenses = expenseDao.fetchExpensesInMonth(SPECIAL.getDbId(), "2025-11");
        assertEquals(2, novemberExpenses.size());
        assertTrue(ids(novemberExpenses).contains(novemberFood.id));
        assertFalse(novemberExpenses.stream().anyMatch(expense -> expense.expenseType == SPECIAL));
        assertEquals(octoberFood.id, expenseDao.getEarliestEntry().id);
        assertEquals(LocalDate.of(2025, 11, 5), expenseDao.getLatestEntry().date);
        assertEquals(2, expenseDao.findExpensesInInterval(LocalDate.of(2025, 11, 3), LocalDate.of(2025, 11, 4)).size());
        assertEquals(3, expenseDao.findExpensesInInterval(null, LocalDate.of(2025, 11, 4)).size());

        final List<YearSummaryRow> yearlySummary = expenseDao.getMonthlySummaryInYear(FOOD.getDbId(), "2025");
        assertEquals(List.of("10", "11"), yearlySummary.stream().map(row -> row.month).toList());
        assertEquals(List.of(10.0, 20.0), yearlySummary.stream().map(row -> row.total).toList());
        final List<MonthlySummaryRow> summaries = expenseDao.getMonthlySummary(List.of(FOOD.getDbId(), PETROL.getDbId()));
        assertEquals(3, summaries.size());
    }

    @Test
    public void bindsFilterValuesAndTreatsLikeMetacharactersLiterally() {
        final ExpenseEntity apostrophe = persist(FOOD, 1, LocalDate.of(2026, 1, 1), "Bob's lunch");
        final ExpenseEntity percent = persist(PETROL, 2, LocalDate.of(2026, 1, 2), "discount 10%_");
        final ExpenseEntity unrelated = persist(SPECIAL, 3, LocalDate.of(2026, 1, 3), "other");

        assertEquals(List.of(apostrophe.id), ids(expenseDao.fetchExpenses(List.of(), Set.of("Bob's"))));
        assertEquals(List.of(percent.id), ids(expenseDao.fetchExpenses(List.of(), Set.of("%_"))));
        assertEquals(Set.of(apostrophe.id, percent.id), Set.copyOf(ids(expenseDao.fetchExpenses(List.of(FOOD.getDbId()), Set.of("10%_")))));
        assertEquals(Set.of(apostrophe.id, percent.id), Set.copyOf(ids(expenseDao.fetchExpenses(List.of(FOOD.getDbId(), PETROL.getDbId()), Set.of()))));
        assertTrue(expenseDao.fetchExpenses(List.of(), Set.of()).isEmpty());
        assertEquals(List.of(percent.id, apostrophe.id), ids(expenseDao.fetchExpenses(List.of(FOOD.getDbId()), Set.of("Bob's", "%_"))));
        assertFalse(ids(expenseDao.fetchExpenses(List.of(), Set.of("Bob's", "%_"))).contains(unrelated.id));
    }

    @Test
    public void replaceAllRollsBackCompletelyWhenProgressPublishingFails() {
        final ExpenseEntity original = persist(FOOD, 10, LocalDate.of(2026, 2, 1), "original");
        final List<ExpenseEntity> restored = List.of(
                entity(null, PETROL, 20, LocalDate.of(2026, 2, 2), "first"),
                entity(null, SPECIAL, 30, LocalDate.of(2026, 2, 3), "second")
        );

        try {
            expenseDao.replaceAll(restored, completed -> {
                throw new IllegalStateException("progress failed");
            });
            fail("Replace should fail");
        } catch (IllegalStateException expected) {
            assertEquals("progress failed", expected.getMessage());
        }

        final List<ExpenseEntity> remaining = expenseDao.fetchAllExpenses();
        assertEquals(1, remaining.size());
        assertEquals(original.id, remaining.getFirst().id);
    }

    @Test
    public void replaceAllReportsProgressAndGeneratesNewIds() {
        persist(FOOD, 10, LocalDate.of(2026, 2, 1), "old");
        final ExpenseEntity imported = entity(500L, PETROL, 20, LocalDate.of(2026, 2, 2), "imported");
        imported.id = null;
        final int[] progress = {0};

        expenseDao.replaceAll(List.of(imported), completed -> progress[0] = completed);

        assertEquals(1, progress[0]);
        assertNotEquals(Long.valueOf(500), imported.id);
        assertEquals("imported", expenseDao.fetchAllExpenses().getFirst().comment);
    }

    @Test
    public void adoptsExactLegacyVersionOneSchemaWithoutLosingData() {
        database.close();
        final String databaseName = "legacy-expenses-" + System.nanoTime() + ".db";
        final File databaseFile = context.getDatabasePath(databaseName);
        databaseFile.getParentFile().mkdirs();

        try (SQLiteDatabase legacyDatabase = SQLiteDatabase.openOrCreateDatabase(databaseFile, null)) {
            legacyDatabase.execSQL("""
                CREATE TABLE EXPENSES(
                    _ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    EXPENSE_TYPE INTEGER NOT NULL,
                    VALUE REAL NOT NULL,
                    DATE TEXT NOT NULL,
                    COMMENT TEXT)
                """);
            legacyDatabase.execSQL("CREATE INDEX IF NOT EXISTS IDX_EXPENSE_DATE ON EXPENSES(DATE)");
            legacyDatabase.execSQL("INSERT INTO EXPENSES(_ID, EXPENSE_TYPE, VALUE, DATE, COMMENT) VALUES(41, 80, 9.5, '2024-03-02', 'legacy')");
            legacyDatabase.setVersion(1);
        }

        database = Room.databaseBuilder(context, ExpenseDatabase.class, databaseName)
                       .allowMainThreadQueries()
                       .build();
        expenseDao = database.expenseDao();

        final ExpenseEntity adopted = expenseDao.fetchAllExpenses().getFirst();
        assertEquals(Long.valueOf(41), adopted.id);
        assertEquals(FOOD, adopted.expenseType);
        assertEquals("legacy", adopted.comment);
        assertSchemaAndIndex(database);

        database.close();
        assertTrue(context.deleteDatabase(databaseName));
    }

    @Test
    public void createsTheLegacyCompatibleSchemaAndIndex() {
        assertSchemaAndIndex(database);
    }

    private void assertSchemaAndIndex(final ExpenseDatabase testedDatabase) {
        try (Cursor columns = testedDatabase.getOpenHelper().getReadableDatabase().query("PRAGMA table_info(EXPENSES)")) {
            assertEquals(5, columns.getCount());
            assertTrue(columns.moveToFirst());
            assertEquals("_ID", columns.getString(columns.getColumnIndexOrThrow("name")));
            assertEquals("INTEGER", columns.getString(columns.getColumnIndexOrThrow("type")));
            assertEquals(1, columns.getInt(columns.getColumnIndexOrThrow("pk")));
        }
        try (Cursor indexes = testedDatabase.getOpenHelper().getReadableDatabase().query("PRAGMA index_list(EXPENSES)")) {
            boolean dateIndexFound = false;
            while (indexes.moveToNext()) {
                dateIndexFound |= "IDX_EXPENSE_DATE".equals(indexes.getString(indexes.getColumnIndexOrThrow("name")));
            }
            assertTrue(dateIndexFound);
        }
    }

    private ExpenseEntity persist(final ExpenseType type, final double value, final LocalDate date, final String comment) {
        final ExpenseEntity expense = entity(null, type, value, date, comment);
        expense.id = expenseDao.insert(expense);
        return expense;
    }

    private ExpenseEntity entity(final Long id, final ExpenseType type, final double value, final LocalDate date, final String comment) {
        final ExpenseEntity expense = new ExpenseEntity();
        expense.id = id;
        expense.expenseType = type;
        expense.value = value;
        expense.date = date;
        expense.comment = comment;
        return expense;
    }

    private List<Long> ids(final List<ExpenseEntity> expenses) {
        return expenses.stream()
                       .map(expense -> expense.id)
                       .toList();
    }
}
