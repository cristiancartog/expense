package ro.pandemonium.expense.util;

import android.os.Environment;
import android.util.Log;

import ro.pandemonium.expense.Constants;
import ro.pandemonium.expense.model.Expense;
import ro.pandemonium.expense.model.ExpenseType;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class FileUtil {

    private static final String CLASS_NAME = FileUtil.class.getSimpleName();
    private static final String EXPENSE_DB_BACKUP_FOLDER = "/expense_db_backup";
    private static final String DEFAULT_FILE_NAME_PREFIX = "expenses-";
    private static final String DEFAULT_FILE_NAME_SUFFIX = ".csv";

    private static final SimpleDateFormat FILE_NAME_DATE_FORMAT = new SimpleDateFormat(Constants.DATE_FORMAT_PATTERN_FILE_TIMESTAMP, Locale.getDefault());
    private static final SimpleDateFormat CSV_DATE_DATE_FORMAT = new SimpleDateFormat(Constants.DATE_FORMAT_PATTERN_DB, Locale.getDefault());

    public static void exportExpenses(final List<Expense> expenses) throws IOException {
        final String fileName = DEFAULT_FILE_NAME_PREFIX + FILE_NAME_DATE_FORMAT.format(new Date()) + DEFAULT_FILE_NAME_SUFFIX;
        final File storageFolder = ensureStorageFolder();
        final File exportFile = new File(storageFolder.getAbsolutePath() + "/" + fileName);

        boolean fileCreated = exportFile.createNewFile();
        if (!fileCreated) {
            throw new FileUtilException("File already exists, wait a second and try again.");
        }

        final FileOutputStream outputStream = new FileOutputStream(exportFile);
        for (Expense expense : expenses) {
            outputStream.write(serializeExpenseToCsvItem(expense));
        }

        outputStream.flush();
        outputStream.close();
    }

    public static List<Expense> importExpenses(String fileName) throws IOException {
        final List<Expense> expenses = new ArrayList<>();

        try {
            final BufferedReader in = new BufferedReader(new FileReader(getStorageFolder().getAbsolutePath() + File.separator + fileName));
            String line;

            do {
                line = in.readLine();
                if (!StringUtils.isEmpty(line)) {
                    expenses.add(deserializeExpense(line));
                }
            } while (line != null);

        } catch (ParseException e) {
            Log.w(CLASS_NAME, e.getMessage());
            throw new IOException(e.getMessage());
        }

        return expenses;
    }

    public static List<String> fetchAllBackupFiles() {
        final String[] fileNames = getStorageFolder().list(new FilenameFilter() {
            @Override
            public boolean accept(final File folder, final String fileName) {
                return fileName.endsWith(DEFAULT_FILE_NAME_SUFFIX);
            }
        });

        return fileNames != null ? Arrays.asList(fileNames) : new ArrayList<String>();
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

    private static byte[] serializeExpenseToCsvItem(final Expense expense) {
        final StringBuilder sb = new StringBuilder();

        sb.append(expense.getId());
        sb.append(",");
        sb.append(expense.getExpenseType().name());
        sb.append(",");
        sb.append(CSV_DATE_DATE_FORMAT.format(expense.getDate()));
        sb.append(",");
        sb.append(expense.getValue());
        sb.append(",");
        sb.append(expense.getComment());
        sb.append("\n");

        return sb.toString().getBytes();
    }

    private static Expense deserializeExpense(final String line) throws ParseException {
        final String[] parts = line.split(",");

        final Expense expense = new Expense();
        expense.setId(Long.valueOf(parts[0]));
        expense.setExpenseType(ExpenseType.valueOf(parts[1]));
        expense.setDate(CSV_DATE_DATE_FORMAT.parse(parts[2]));
        expense.setValue(Double.valueOf(parts[3]));
        expense.setComment(parts.length == 5 ? parts[4] : "");

        Log.i(Constants.APPLICATION_NAME, expense.toString());

        return expense;
    }

}
