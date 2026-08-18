package com.btechloanwala.LeadGeneration.enums;

import java.util.Optional;

/**
 * Valid loan types accepted by the public form.
 *
 * <p>The frontend sends the lowercase value (e.g. {@code "home-bt"}), which does not
 * match Java's enum naming (HOME_BT). Jackson therefore cannot map it automatically,
 * so we keep the request field as a plain String and resolve it here explicitly.</p>
 */
public enum LoanType {

    PERSONAL("personal"),
    HOME("home"),
    BUSINESS("business"),
    LAP("lap"),
    CAR("car"),
    HOME_BT("home-bt"),
    EDUCATION("education"),
    PROJECT_FUNDING("project-funding");

    private final String value;

    LoanType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * Case-insensitive lookup from the frontend string value, ignoring surrounding
     * whitespace. Returns {@link Optional#empty()} when the value is not a valid type.
     */
    public static Optional<LoanType> fromValue(String value) {
        if (value == null) {
            return Optional.empty();
        }
        final String trimmed = value.trim();
        for (LoanType type : values()) {
            if (type.value.equalsIgnoreCase(trimmed)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}