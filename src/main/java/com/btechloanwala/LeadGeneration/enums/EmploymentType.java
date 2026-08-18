package com.btechloanwala.LeadGeneration.enums;

import java.util.Optional;

/**
 * Valid employment types accepted by the public form.
 *
 * <p>The frontend sends {@code "self-employed"} while the Java enum member is
 * SELF_EMPLOYED, so a direct Jackson conversion does not work. The request field is
 * a plain String and is resolved here explicitly.</p>
 */
public enum EmploymentType {

    SALARIED("salaried"),
    SELF_EMPLOYED("self-employed"),
    BUSINESS("business");

    private final String value;

    EmploymentType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Optional<EmploymentType> fromValue(String value) {
        if (value == null) {
            return Optional.empty();
        }
        final String trimmed = value.trim();
        for (EmploymentType type : values()) {
            if (type.value.equalsIgnoreCase(trimmed)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}