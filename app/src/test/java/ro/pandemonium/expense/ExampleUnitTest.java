package ro.pandemonium.expense;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.text.DecimalFormat;
import java.text.NumberFormat;


public class ExampleUnitTest {

    private final NumberFormat numberFormatter = new DecimalFormat("00");

    @Test
    public void addition_isCorrect() {
        assertEquals("10", numberFormatter.format(10));
    }
}
