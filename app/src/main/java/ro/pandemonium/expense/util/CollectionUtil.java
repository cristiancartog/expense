package ro.pandemonium.expense.util;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public class CollectionUtil {

    private CollectionUtil() {
    }

    public static <T, U> List<T> convertList(final Collection<U> source, final Function<U, T> converter) {
        if (source == null) {
            return null;
        }

        return source.stream()
                .map(converter)
                .toList();
    }

}
