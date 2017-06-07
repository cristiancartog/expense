package ro.pandemonium.expense;

import org.junit.Test;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import static org.junit.Assert.*;
import static ro.pandemonium.expense.Constants.NUMBER_FORMAT_PATTERN;

public class ExampleUnitTest {

    private final NumberFormat numberFormatter = new DecimalFormat(NUMBER_FORMAT_PATTERN);

    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals("1.234,00", numberFormatter.format(1234));
    }
}