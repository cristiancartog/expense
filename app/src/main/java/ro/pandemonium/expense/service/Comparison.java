package ro.pandemonium.expense.service;

import static java.util.Objects.requireNonNull;

import java.util.OptionalDouble;

public record Comparison(
        double currentValue,
        double previousValue,
        OptionalDouble variation) {

    public Comparison {
        requireNonNull(variation);
    }
}
