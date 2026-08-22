package ro.pandemonium.expense.util;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ro.pandemonium.expense.Constants;
import ro.pandemonium.expense.model.Expense;

public class FileUtil {

    private static final String CLASS_NAME = FileUtil.class.getSimpleName();
    private static final String EXPENSE_DB_BACKUP_FOLDER = "expense_db_backup";
    private static final String DEFAULT_FILE_NAME_PREFIX = "expenses-";
    private static final String DEFAULT_FILE_NAME_SUFFIX = ".csv";
    private static final String CSV_MIME_TYPE = "text/csv";

    public static void exportExpenses(final Context context, final List<Expense> expenses) throws IOException {
        final SimpleDateFormat fileNameDateFormat = new SimpleDateFormat(Constants.DATE_FORMAT_PATTERN_FILE_TIMESTAMP, Locale.getDefault());
        final String fileName = DEFAULT_FILE_NAME_PREFIX + fileNameDateFormat.format(new Date()) + DEFAULT_FILE_NAME_SUFFIX;

        exportExpensesToMediaStore(context, expenses, fileName);
    }

    public static List<Expense> importExpenses(final Context context, final Uri backupUri) throws IOException {
        try {
            final InputStream inputStream = context.getContentResolver().openInputStream(backupUri);
            if (inputStream == null) {
                throw new FileUtilException("Could not open the selected backup file.");
            }
            return readExpenses(inputStream);
        } catch (ParseException e) {
            Log.w(CLASS_NAME, e.getMessage());
            throw new IOException(e);
        }
    }

    public static List<String> fetchAllBackupFiles(final Context context) {
        final List<String> fileNames = new ArrayList<>();
        final String[] projection = {MediaStore.MediaColumns.DISPLAY_NAME};
        final String selection = MediaStore.MediaColumns.RELATIVE_PATH + " = ? AND "
                + MediaStore.MediaColumns.DISPLAY_NAME + " LIKE ?";
        final String[] selectionArgs = {backupRelativePath(), "%" + DEFAULT_FILE_NAME_SUFFIX};

        try (Cursor cursor = context.getContentResolver().query(
                backupCollection(), projection, selection, selectionArgs, null)) {
            if (cursor != null) {
                final int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME);
                while (cursor.moveToNext()) {
                    fileNames.add(cursor.getString(nameColumn));
                }
            }
        }
        return fileNames;
    }

    private static void exportExpensesToMediaStore(final Context context,
                                                   final List<Expense> expenses,
                                                   final String fileName) throws IOException {
        if (findBackupUri(context, fileName) != null) {
            throw new FileUtilException("File already exists, wait a second and try again.");
        }

        final ContentResolver resolver = context.getContentResolver();
        final ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, CSV_MIME_TYPE);
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, backupRelativePath());
        values.put(MediaStore.MediaColumns.IS_PENDING, 1);

        final Uri exportUri = resolver.insert(backupCollection(), values);
        if (exportUri == null) {
            throw new FileUtilException("Could not create backup file.");
        }

        try {
            final OutputStream outputStream = resolver.openOutputStream(exportUri);
            if (outputStream == null) {
                throw new FileUtilException("Could not open backup file for writing.");
            }

            final SimpleDateFormat csvDateFormat = new SimpleDateFormat(Constants.DATE_FORMAT_PATTERN_DB, Locale.getDefault());
            try (OutputStreamWriter fileWriter = new OutputStreamWriter(outputStream, UTF_8)) {
                for (Expense expense : expenses) {
                    fileWriter.write(expense.toCsv(csvDateFormat));
                    fileWriter.write("\n");
                }
            }

            values.clear();
            values.put(MediaStore.MediaColumns.IS_PENDING, 0);
            resolver.update(exportUri, values, null, null);
        } catch (IOException | RuntimeException e) {
            resolver.delete(exportUri, null, null);
            throw e;
        }
    }

    private static Uri findBackupUri(final Context context, final String fileName) {
        final String[] projection = {MediaStore.MediaColumns._ID};
        final String selection = MediaStore.MediaColumns.RELATIVE_PATH + " = ? AND "
                + MediaStore.MediaColumns.DISPLAY_NAME + " = ?";
        final String[] selectionArgs = {backupRelativePath(), fileName};

        try (Cursor cursor = context.getContentResolver().query(
                backupCollection(), projection, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                final long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID));
                return ContentUris.withAppendedId(backupCollection(), id);
            }
        }
        return null;
    }

    private static Uri backupCollection() {
        return MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
    }

    private static String backupRelativePath() {
        return Environment.DIRECTORY_DOCUMENTS + "/" + EXPENSE_DB_BACKUP_FOLDER + "/";
    }

    private static List<Expense> readExpenses(final InputStream inputStream) throws IOException, ParseException {
        final List<Expense> expenses = new ArrayList<>();
        try (final BufferedReader in = new BufferedReader(new InputStreamReader(inputStream, UTF_8))) {
            final SimpleDateFormat csvDateFormat = new SimpleDateFormat(Constants.DATE_FORMAT_PATTERN_DB, Locale.getDefault());
            String line;

            while ((line = in.readLine()) != null) {
                if (!StringUtils.isEmpty(line)) {
                    expenses.add(Expense.fromCsv(line, csvDateFormat));
                }
            }
        }
        return expenses;
    }
}
