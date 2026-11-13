package ro.pandemonium.expense.util;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import ro.pandemonium.expense.model.Expense;

class FileUtilTest {

    private final Context context = mock(Context.class);
    private final ContentResolver resolver = mock(ContentResolver.class);
    private final Uri backupUri = mock(Uri.class);

    @ParameterizedTest
    @ValueSource(doubles = {0, 0.01, 0.10, 0.20, 0.29, 1.13, 12.50, 1234.56, 999999999.99})
    void importsBackupsWrittenByTheDoubleVersion(final double value) throws IOException {
        // This is the exact formatter used by the version whose Expense.value was Double.
        prepareBackup(String.format(Locale.ROOT, "%d,%s,%s,%.2f,%s%n", 500L, "FOOD", "2024-02-29", value, "Prânz, cafea"));

        final Expense expense = FileUtil.importExpenses(context, backupUri).getFirst();

        assertEquals(BigDecimal.valueOf(value).setScale(2), expense.getValue());
        assertEquals(LocalDate.of(2024, 2, 29), expense.getDate());
        assertEquals("Prânz, cafea", expense.getComment());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "1,FOOD,2026-11-10", ",FOOD,2026-11-10,12.50", "x,FOOD,2026-11-10,12.50",
            "1,,2026-11-10,12.50", "1,UNKNOWN,2026-11-10,12.50", "1,FOOD,2026-11-10,",
            "1,FOOD,2026-11-10,-0.01", "1,FOOD,2026-11-10,NaN", "1,FOOD,2026-11-10,Infinity",
            "1,FOOD,2026-11-10,-Infinity", "1,FOOD,2026-11-10,0.001", "1,FOOD,2026-11-10,12.501",
            "1,FOOD,2026-11-10,12.5000001", "1,FOOD,2026-11-10,1000000000.00", "1,FOOD,2026-11-10,1e309",
            "1,FOOD,2026-02-30,12.50", "1,FOOD,2026-02-29,12.50", "1,FOOD,,12.50"
    })
    void rejectsTheEntireBackupWhenARecordIsInvalid(final String record) throws IOException {
        prepareBackup("2,PETROL,2026-11-10,30.25,valid expense\n" + record + "\n");

        assertThrows(IOException.class, () -> FileUtil.importExpenses(context, backupUri));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.01", "0.29", "1.13", "12.50", "12.5000", "999999999.99"})
    void importsSupportedAmountsAndPreservesTheComment(final String amount) throws IOException {
        prepareBackup("1,FOOD,2024-02-29," + amount + ",Lunch, coffee\n");

        final List<Expense> expenses = FileUtil.importExpenses(context, backupUri);

        assertEquals(1, expenses.size());
        assertEquals(new BigDecimal(amount).setScale(2), expenses.getFirst().getValue());
        assertEquals(LocalDate.of(2024, 2, 29), expenses.getFirst().getDate());
        assertEquals("Lunch, coffee", expenses.getFirst().getComment());
    }

    private void prepareBackup(final String contents) throws IOException {
        when(context.getContentResolver()).thenReturn(resolver);
        when(resolver.openInputStream(backupUri)).thenReturn(new ByteArrayInputStream(contents.getBytes(UTF_8)));
    }
}
