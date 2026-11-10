package ro.pandemonium.expense.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static ro.pandemonium.expense.model.ExpenseType.FOOD;
import static ro.pandemonium.expense.model.ExpenseType.PETROL;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
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
        original.value = 12.5;
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
            assertEquals(30.25, restored.getFirst().value, 0.0);
            assertEquals("restored expense", restored.getFirst().comment);
            final Set<Uri> recoveryBackups = backupUris();
            recoveryBackups.removeAll(previousBackups);
            assertEquals(1, recoveryBackups.size());
            final List<Expense> recovered = FileUtil.importExpenses(context, recoveryBackups.iterator().next());
            assertEquals(1, recovered.size());
            final Expense recoveredExpense = recovered.getFirst();
            assertEquals(original.id, recoveredExpense.getId());
            assertEquals(original.expenseType, recoveredExpense.getExpenseType());
            assertEquals(original.value, recoveredExpense.getValue(), 0.0);
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
        assertEquals(original.value, expense.value, 0.0);
        assertEquals(original.date, expense.date);
        assertEquals(original.comment, expense.comment);
    }
}
