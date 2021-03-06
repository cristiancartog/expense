package ro.pandemonium.expense.util;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ro.pandemonium.expense.Constants;
import ro.pandemonium.expense.model.Expense;

public class FileUtil {

    private static final String CLASS_NAME = FileUtil.class.getSimpleName();
    private static final String EXPENSE_DB_BACKUP_FOLDER = "/expense_db_backup";
    private static final String DEFAULT_FILE_NAME_PREFIX = "expenses-";
    private static final String DEFAULT_FILE_NAME_SUFFIX = ".csv";

    public static void exportExpenses(final List<Expense> expenses) throws IOException {
        final SimpleDateFormat fileNameDateFormat = new SimpleDateFormat(Constants.DATE_FORMAT_PATTERN_FILE_TIMESTAMP, Locale.getDefault());
        final String fileName = DEFAULT_FILE_NAME_PREFIX + fileNameDateFormat.format(new Date()) + DEFAULT_FILE_NAME_SUFFIX;
        final File storageFolder = ensureStorageFolder();
        final File exportFile = new File(storageFolder.getAbsolutePath() + "/" + fileName);

        boolean fileCreated = exportFile.createNewFile();
        if (!fileCreated) {
            throw new FileUtilException("File already exists, wait a second and try again.");
        }

        final SimpleDateFormat csvDateFormat = new SimpleDateFormat(Constants.DATE_FORMAT_PATTERN_DB, Locale.getDefault());
        try (OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(exportFile), "UTF-8")) {
            for (Expense expense : expenses) {
                fileWriter.write(expense.toCsv(csvDateFormat));
                fileWriter.write("\n");
            }

            fileWriter.flush();
            fileWriter.close();
        }
    }

    public static List<Expense> importExpenses(String fileName) throws IOException {
        final List<Expense> expenses = new ArrayList<>();

        try {
            final String filePath = getStorageFolder().getAbsolutePath() + File.separator + fileName;

            try (final BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "UTF-8"))) {
                final SimpleDateFormat csvDateFormat = new SimpleDateFormat(Constants.DATE_FORMAT_PATTERN_DB, Locale.getDefault());
                String line;

                do {
                    line = in.readLine();
                    if (!StringUtils.isEmpty(line)) {
                        expenses.add(Expense.fromCsv(line, csvDateFormat));
                    }
                } while (line != null);
            }

        } catch (ParseException e) {
            Log.w(CLASS_NAME, e.getMessage());
            throw new IOException(e);
        }

        return expenses;
    }

    public static List<String> fetchAllBackupFiles() {
        final String[] fileNames = getStorageFolder()
                .list((folder, fileName) -> fileName.endsWith(DEFAULT_FILE_NAME_SUFFIX));

        return fileNames != null ? Arrays.asList(fileNames) : new ArrayList<>();
    }

    private static File getStorageFolder() {
        return new File(Environment.getExternalStorageDirectory().toString() + EXPENSE_DB_BACKUP_FOLDER);
    }

    private static File ensureStorageFolder() throws IOException {
        final File storageFolder = getStorageFolder();

        if (!storageFolder.exists()) {
            boolean folderCreated = storageFolder.mkdirs();

            if (!folderCreated) {
                throw new FileUtilException("Could not create folder for backup.");
            }
        }

        return storageFolder;
    }
}
