package ro.pandemonium.expense.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static ro.pandemonium.expense.model.ExpenseType.FOOD;
import static ro.pandemonium.expense.model.ExpenseType.PETROL;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import ro.pandemonium.expense.db.ExpenseDao;
import ro.pandemonium.expense.db.ExpenseDatabase;
import ro.pandemonium.expense.db.ExpenseEntity;
import ro.pandemonium.expense.model.Expense;
import ro.pandemonium.expense.util.FileUtil;

@RunWith(AndroidJUnit4.class)
public class ExpenseDatabaseRestoreTest {

    private Context context;
    private ExpenseDatabase database;
    private ExpenseDao expenseDao;
    private ExpenseMainService service;
    private File backupFile;
    private ExpenseEntity original;

    @Before
    public void setUp() throws IOException {
        context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, ExpenseDatabase.class)
                       .allowMainThreadQueries()
                       .build();
        expenseDao = database.expenseDao();
        service = new ExpenseMainService(expenseDao, context);
        backupFile = File.createTempFile("restore-test-", ".csv", context.getCacheDir());
        original = new ExpenseEntity();
        original.expenseType = FOOD;
        original.value = new BigDecimal("12.50");
        original.date = LocalDate.of(2026, 11, 10);
        original.comment = "keep this expense";
        original.id = expenseDao.insert(original);
    }

    @After
    public void tearDown() {
        if (database != null) {
            database.close();
        }
        if (backupFile != null) {
            assertTrue(backupFile.delete());
        }
    }

    @Test
    public void dumpingAnEmptyDatabaseDoesNotCreateABackupFile() throws IOException {
        expenseDao.delete(original.id);
        final Set<Uri> previousBackups = backupUris();

        assertFalse(service.dumpDatabase(ignored -> fail("Unexpected dump progress")));

        assertEquals(previousBackups, backupUris());
        assertTrue(expenseDao.fetchAllExpenses().isEmpty());
    }

    @Test
    public void restoringIntoAnEmptyDatabaseDoesNotCreateARecoveryBackup() throws IOException {
        expenseDao.delete(original.id);
        Files.writeString(backupFile.toPath(), "500,PETROL,2026-11-09,30.25,restored expense\n", StandardCharsets.UTF_8);
        final List<Expense> previewedExpenses = service.previewDatabaseRestore(Uri.fromFile(backupFile));
        final Set<Uri> previousBackups = backupUris();

        service.restoreDatabase(previewedExpenses, progress -> assertTrue(progress.restore()));

        final List<ExpenseEntity> restored = expenseDao.fetchAllExpenses();
        assertEquals(1, restored.size());
        assertEquals(PETROL, restored.getFirst().expenseType);
        assertEquals(0, restored.getFirst().value.compareTo(BigDecimal.valueOf(30.25)));
        assertEquals(LocalDate.of(2026, 11, 9), restored.getFirst().date);
        assertEquals("restored expense", restored.getFirst().comment);
        assertEquals(previousBackups, backupUris());
    }

    @Test
    public void restoresDoubleVersionBackupAfterDatabaseRecreationAndKeepsItAcrossReopens() throws IOException {
        database.close();
        final String databaseName = "legacy-restore-" + System.nanoTime() + ".db";
        final File databaseFile = context.getDatabasePath(databaseName);
        databaseFile.getParentFile().mkdirs();

        try {
            try (SQLiteDatabase legacyDatabase = SQLiteDatabase.openOrCreateDatabase(databaseFile, null)) {
                legacyDatabase.execSQL("""
                    CREATE TABLE EXPENSES (
                        _ID INTEGER PRIMARY KEY AUTOINCREMENT,
                        EXPENSE_TYPE INTEGER NOT NULL,
                        VALUE REAL NOT NULL,
                        DATE TEXT NOT NULL,
                        COMMENT TEXT)
                    """);
                legacyDatabase.execSQL("CREATE INDEX IDX_EXPENSE_DATE ON EXPENSES(DATE)");
                legacyDatabase.execSQL("INSERT INTO EXPENSES VALUES(41, 80, 12.5, '2026-11-09', 'old database')");
                legacyDatabase.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)");
                legacyDatabase.execSQL("INSERT INTO room_master_table VALUES(42, 'bfb3e96626fddc71df47105ab4dd3df7')");
                legacyDatabase.setVersion(1);
            }

            database = openPersistentDatabase(databaseName);
            expenseDao = database.expenseDao();
            service = new ExpenseMainService(expenseDao, context);
            assertTrue(expenseDao.fetchAllExpenses().isEmpty());

            final List<String> amounts = List.of("0.00", "0.01", "0.10", "0.20", "0.29", "1.13", "12.50", "999999999.99");
            final StringBuilder legacyBackup = new StringBuilder();
            for (int index = 0; index < amounts.size(); index++) {
                // Generate the CSV using the old Double export format, including comma-containing comments.
                legacyBackup.append(String.format(
                        Locale.ROOT,
                        "%d,%s,%s,%.2f,%s%n",
                        500L + index,
                        "FOOD",
                        "2026-11-09",
                        Double.valueOf(amounts.get(index)),
                        "Prânz, cafea"
                ));
            }
            Files.writeString(backupFile.toPath(), legacyBackup, StandardCharsets.UTF_8);
            final List<Expense> preview = service.previewDatabaseRestore(Uri.fromFile(backupFile));

            service.restoreDatabase(preview, progress -> assertTrue(progress.restore()));
            database.close();
            database = openPersistentDatabase(databaseName);
            expenseDao = database.expenseDao();

            final List<ExpenseEntity> restored = expenseDao.fetchAllExpenses();
            assertEquals(amounts.size(), restored.size());
            for (int index = 0; index < amounts.size(); index++) {
                final ExpenseEntity expense = restored.get(index);
                assertEquals(new BigDecimal(amounts.get(index)), expense.value);
                assertEquals(FOOD, expense.expenseType);
                assertEquals(LocalDate.of(2026, 11, 9), expense.date);
                assertEquals("Prânz, cafea", expense.comment);
            }
            assertEquals(new BigDecimal("1000000014.22"), expenseDao.getMonthlySummaryInYear(FOOD.getDbId(), "2026").getFirst().total);
        } finally {
            database.close();
            assertTrue(context.deleteDatabase(databaseName));
        }
    }

    private ExpenseDatabase openPersistentDatabase(final String databaseName) {
        return Room.databaseBuilder(context, ExpenseDatabase.class, databaseName)
                   .fallbackToDestructiveMigrationFrom(true, 1)
                   .allowMainThreadQueries()
                   .build();
    }

    @Test
    public void zeroByteBackupCannotDeleteExistingExpenses() throws IOException {
        assertEquals(0, backupFile.length());

        assertEmptyBackupIsSafe();
    }

    @Test
    public void blankOnlyBackupCannotDeleteExistingExpenses() throws IOException {
        Files.writeString(backupFile.toPath(), " \t\r\n\n  \r\n\t", StandardCharsets.UTF_8);

        assertEmptyBackupIsSafe();
    }

    @Test
    public void invalidRecordAfterAValidRecordLeavesOriginalHistoryUnchanged() throws IOException {
        final List<String> invalidRecords = List.of(
                "501,FOOD,2026-11-10,-1.00",
                "501,FOOD,2026-11-10,NaN",
                "501,FOOD,2026-11-10,Infinity",
                "501,FOOD,2026-11-10,-Infinity",
                "501,FOOD,2026-11-10,0.001",
                "501,FOOD,2026-11-10,1000000000.00",
                "501,FOOD,2026-02-30,12.50",
                "501,FOOD,2026-02-29,12.50",
                "501,UNKNOWN,2026-11-10,12.50",
                "501,,2026-11-10,12.50",
                "501,FOOD,,12.50",
                "501,FOOD,2026-11-10,",
                ",FOOD,2026-11-10,12.50",
                "501,FOOD,2026-11-10"
        );

        for (String invalidRecord : invalidRecords) {
            Files.writeString(
                    backupFile.toPath(),
                    "500,PETROL,2026-11-09,30.25,valid expense\n" + invalidRecord + "\n",
                    StandardCharsets.UTF_8
            );

            assertThrows(
                    IOException.class,
                    () -> service.previewDatabaseRestore(Uri.fromFile(backupFile))
            );

            assertOriginalExpenseRemains();
        }
    }

    @Test
    public void invalidPreviewMutationCannotReplaceOriginalHistory() throws IOException {
        Files.writeString(backupFile.toPath(), "500,PETROL,2026-11-09,30.25,restored expense\n", StandardCharsets.UTF_8);
        final List<Expense> previewedExpenses = service.previewDatabaseRestore(Uri.fromFile(backupFile));
        previewedExpenses.getFirst().setValue(new BigDecimal("-0.01"));

        assertThrows(IOException.class, () -> service.restoreDatabase(previewedExpenses, ignored -> fail("Unexpected transfer")));

        assertOriginalExpenseRemains();
    }

    @Test
    public void confirmedRestoreSavesOriginalExpensesAndUsesThePreviewedData() throws IOException {
        Files.writeString(backupFile.toPath(), "500,PETROL,2026-11-09,30.25,restored expense\n", StandardCharsets.UTF_8);
        final List<Expense> previewedExpenses = service.previewDatabaseRestore(Uri.fromFile(backupFile));
        assertEquals(1, previewedExpenses.size());
        assertOriginalExpenseRemains();
        Files.writeString(backupFile.toPath(), "", StandardCharsets.UTF_8);
        final Set<Uri> previousBackups = backupUris();

        try {
            service.restoreDatabase(previewedExpenses, progress -> {
                if (progress.restore() && progress.completed() == 0) {
                    assertOriginalExpenseRemains();
                    final Set<Uri> recoveryBackups = backupUris();
                    recoveryBackups.removeAll(previousBackups);
                    assertEquals(1, recoveryBackups.size());
                }
            });

            final List<ExpenseEntity> restored = expenseDao.fetchAllExpenses();
            assertEquals(1, restored.size());
            assertEquals(PETROL, restored.getFirst().expenseType);
            assertEquals(0, restored.getFirst().value.compareTo(BigDecimal.valueOf(30.25)));
            assertEquals("restored expense", restored.getFirst().comment);
            final Set<Uri> recoveryBackups = backupUris();
            recoveryBackups.removeAll(previousBackups);
            assertEquals(1, recoveryBackups.size());
            final List<Expense> recovered = FileUtil.importExpenses(context, recoveryBackups.iterator().next());
            assertEquals(1, recovered.size());
            final Expense recoveredExpense = recovered.getFirst();
            assertEquals(original.id, recoveredExpense.getId());
            assertEquals(original.expenseType, recoveredExpense.getExpenseType());
            assertEquals(original.value, recoveredExpense.getValue());
            assertEquals(original.date, recoveredExpense.getDate());
            assertEquals(original.comment, recoveredExpense.getComment());
        } finally {
            final Set<Uri> recoveryBackups = backupUris();
            recoveryBackups.removeAll(previousBackups);
            recoveryBackups.forEach(uri -> context.getContentResolver().delete(uri, null, null));
        }
    }

    private Set<Uri> backupUris() {
        final Uri collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        final Set<Uri> uris = new HashSet<>();
        try (Cursor cursor = context.getContentResolver().query(
                collection,
                new String[]{MediaStore.MediaColumns._ID},
                MediaStore.MediaColumns.RELATIVE_PATH + " = ?",
                new String[]{Environment.DIRECTORY_DOCUMENTS + "/expense_db_backup/"},
                null
        )) {
            assertNotNull(cursor);
            while (cursor.moveToNext()) {
                uris.add(ContentUris.withAppendedId(collection, cursor.getLong(0)));
            }
        }
        return uris;
    }

    private void assertEmptyBackupIsSafe() throws IOException {
        final List<Expense> previewedExpenses = service.previewDatabaseRestore(Uri.fromFile(backupFile));

        assertTrue(previewedExpenses.isEmpty());
        assertOriginalExpenseRemains();
        assertThrows(IOException.class, () -> service.restoreDatabase(previewedExpenses, ignored -> fail("Unexpected transfer")));
        assertOriginalExpenseRemains();
    }

    private void assertOriginalExpenseRemains() {
        final List<ExpenseEntity> remaining = expenseDao.fetchAllExpenses();
        assertEquals(1, remaining.size());
        final ExpenseEntity expense = remaining.getFirst();
        assertEquals(original.id, expense.id);
        assertEquals(original.expenseType, expense.expenseType);
        assertEquals(original.value, expense.value);
        assertEquals(original.date, expense.date);
        assertEquals(original.comment, expense.comment);
    }
}
