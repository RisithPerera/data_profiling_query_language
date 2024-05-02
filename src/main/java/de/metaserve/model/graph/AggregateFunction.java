package de.metaserve.model.graph;

import java.util.List;
import java.util.function.Function;

public enum AggregateFunction {
    FIRST(values -> values.get(0)),
    SIZE(values -> String.valueOf(values.size())),
    CARD(values -> String.valueOf(values.size())),
    SUM(values -> {
        double sum = 0;
        for (String value : values) {
            sum += Double.parseDouble(value);
        }
        return String.valueOf(sum);
    }),
    MINIMUM(values -> {
        double min = Double.POSITIVE_INFINITY;
        for (String value : values) {
            double currentValue = Double.parseDouble(value);
            if (currentValue < min) {
                min = currentValue;
            }
        }
        return String.valueOf(min);
    }),
    MEAN(values -> {
        double sum = 0;
        for (String value : values) {
            sum += Double.parseDouble(value);
        }
        double mean = sum / values.size();
        return String.valueOf(mean);
    }),
    MAXIMUM(values -> {
        double max = Double.NEGATIVE_INFINITY;
        for (String value : values) {
            double currentValue = Double.parseDouble(value);
            if (currentValue > max) {
                max = currentValue;
            }
        }
        return String.valueOf(max);
    }),
    AVG(values -> {
        double sum = 0;
        for (String value : values) {
            sum += Double.parseDouble(value);
        }
        double avg = sum / values.size();
        return String.valueOf(avg);
    }),
    TYPE(values -> {
        if (values.isEmpty()) {
            return "NULL";
        }
        String type = values.get(0).getClass().getSimpleName();
        return type;
    }),
    NULL(values -> values.isEmpty() ? "NULL" : "NOT NULL"),
    UNIQUENESS(values -> {
        double uniqueCount = values.stream().distinct().count();
        double totalCount = values.size();
        double uniqueness = uniqueCount / totalCount * 100.0;
        return String.format("%.2f%%", uniqueness);
    }),
    OVERLAP(values -> {
        StringBuilder result = new StringBuilder();
        for (String value : values) {
            if (result.length() > 0) {
                result.append(", ");
            }
            result.append(value);
        }
        return result.toString();
    });

    private Function<List<String>, String> aggregationFunction;

    AggregateFunction(Function<List<String>, String> aggregationFunction) {
        this.aggregationFunction = aggregationFunction;
    }

    public String apply(List<String> values) {
        return aggregationFunction.apply(values);
    }
}

