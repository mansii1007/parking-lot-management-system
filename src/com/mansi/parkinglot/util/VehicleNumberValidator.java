package com.mansi.parkinglot.util;

import com.mansi.parkinglot.exception.InvalidInputException;

import java.util.regex.Pattern;

/**
 * Checks that a registration number looks like a real plate before it is stored.
 * Accepts a standard Indian format (e.g. MH12AB1234) and a slightly flexible
 * alphanumeric form with an optional hyphen (e.g. MH-01-XY-9988).
 */
public final class VehicleNumberValidator {
    // MH12AB1234, MH01X1234 (Bharat series varies a little)
    private static final Pattern INDIAN =
            Pattern.compile("^[A-Z]{2}[0-9]{1,2}[A-Z]{1,3}[0-9]{4}$");
    private static final Pattern FLEXIBLE =
            Pattern.compile("^[A-Z0-9][A-Z0-9-]{3,13}[A-Z0-9]$");

    private VehicleNumberValidator() {
    }

    public static String normalizeAndValidate(String raw) throws InvalidInputException {
        if (raw == null || raw.trim().isEmpty()) {
            throw new InvalidInputException("Registration number cannot be empty.");
        }
        String value = raw.trim().toUpperCase().replaceAll("\s+", "");
        if (value.length() < 5 || value.length() > 15) {
            throw new InvalidInputException(
                    "Registration number must be between 5 and 15 characters (e.g. MH12AB1234).");
        }
        if (!INDIAN.matcher(value.replace("-", "")).matches()
                && !FLEXIBLE.matcher(value).matches()) {
            throw new InvalidInputException(
                    "Invalid registration number '" + raw + "'. Use a format like MH12AB1234.");
        }
        return value;
    }
}
