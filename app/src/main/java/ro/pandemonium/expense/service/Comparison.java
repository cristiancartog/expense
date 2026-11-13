package ro.pandemonium.expense.service;

import static java.util.Objects.requireNonNull;

import java.math.BigDecimal;
import java.util.Optional;

public record Comparison(
        BigDecimal currentValue,
        BigDecimal previousValue,
        Optional<BigDecimal> variationPercentage) {

    public Comparison {
        requireNonNull(currentValue);
        requireNonNull(previousValue);
        requireNonNull(variationPercentage);
    }
}
