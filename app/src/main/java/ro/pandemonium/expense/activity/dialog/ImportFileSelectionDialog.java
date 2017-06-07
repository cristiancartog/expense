package ro.pandemonium.expense.activity.dialog;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import java.util.Collections;
import java.util.List;

import ro.pandemonium.expense.R;
import ro.pandemonium.expense.util.FileUtil;

public class ImportFileSelectionDialog extends Dialog implements View.OnClickListener {

    public interface Callback {
        void fileSelected(String fileName);
    }

    private final Spinner spinner;
    private Callback callback;

    public ImportFileSelectionDialog(final Context context) {
        super(context);

        setContentView(R.layout.import_file_selection);
        setTitle("Import file");

        spinner = (Spinner) findViewById(R.id.importFileDialogSpinner);
        final Button okButton = (Button) findViewById(R.id.importFileDialogOkButton);
        final Button cancelButton = (Button) findViewById(R.id.importFileDialogCancelButton);

        okButton.setOnClickListener(this);
        cancelButton.setOnClickListener(this);
    }

    public void show(final Callback callback) {
        super.show();
        this.callback = callback;

        final List<String> fileNames = FileUtil.fetchAllBackupFiles();
        Collections.sort(fileNames, (s1, s2) -> -s1.compareTo(s2));

        spinner.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, fileNames));
    }

    @Override
    public void onClick(final View view) {
        switch (view.getId()) {
            case R.id.importFileDialogOkButton:
                callback.fileSelected((String) spinner.getSelectedItem());
                hide();
                break;
            case R.id.importFileDialogCancelButton:
                hide();
                break;
        }
    }
}
