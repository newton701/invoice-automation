package com.invoiceautomation.validation;

import java.util.ArrayList;
import java.util.List;

public record ValidationReport(boolean passed, int checksRun, List<String> errors, List<String> warnings) {
    public ValidationReport {
        errors = List.copyOf(errors);
        warnings = List.copyOf(warnings);
    }
    public static class Builder {
        private int checks;
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        public void check(boolean ok, String message) { checks++; if (!ok) errors.add(message); }
        public void warn(String message) { warnings.add(message); }
        public ValidationReport build() { return new ValidationReport(errors.isEmpty(), checks, errors, warnings); }
    }
}
