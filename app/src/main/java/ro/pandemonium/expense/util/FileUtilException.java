package ro.pandemonium.expense.util;

import java.io.IOException;

class FileUtilException extends IOException {

    FileUtilException(final String message) {
        super(message);
    }
}