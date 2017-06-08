package ro.pandemonium.expense;

import org.junit.Test;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import static org.junit.Assert.*;
import static ro.pandemonium.expense.Constants.NUMBER_FORMAT_PATTERN;

public class ExampleUnitTest {

    private final NumberFormat numberFormatter = new DecimalFormat("00");

    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals("10", numberFormatter.format(10));
    }
}