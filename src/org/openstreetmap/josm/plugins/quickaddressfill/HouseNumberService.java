package org.openstreetmap.josm.plugins.quickaddressfill;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class HouseNumberService {

    private static final Pattern NUMERIC_HOUSE_NUMBER_PATTERN = Pattern.compile("^(\\d+)$");
    private static final Pattern NUMERIC_WITH_LETTER_SUFFIX_PATTERN = Pattern.compile("^(\\d+)([A-Za-z]+)$");
    private static final Pattern LETTER_HOUSE_NUMBER_PATTERN = Pattern.compile("^([A-Za-z]+)$");
    private static final Pattern HOUSE_NUMBER_WITH_OPTIONAL_SUFFIX_PATTERN = Pattern.compile("^(\\d+)([A-Za-z]+)?$");

    String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    int normalizeIncrementStep(int step) {
        return step == -2 || step == -1 || step == 1 || step == 2 ? step : 1;
    }

    int sanitizeIncrementStepForHouseNumber(String houseNumber, int step) {
        int normalizedStep = normalizeIncrementStep(step);
        if (containsLetter(houseNumber) && normalizedStep != 1) {
            return 1;
        }
        return normalizedStep;
    }

    boolean containsLetter(String value) {
        return value != null && value.matches(".*[A-Za-z].*");
    }

    boolean hasLetterSuffix(String value) {
        Matcher matcher = HOUSE_NUMBER_WITH_OPTIONAL_SUFFIX_PATTERN.matcher(normalize(value));
        return matcher.matches() && matcher.group(2) != null && !matcher.group(2).isEmpty();
    }

    String incrementAfterSuccessfulApply(String current, int incrementStep) {
        String normalized = normalize(current);
        Matcher numericWithLetters = NUMERIC_WITH_LETTER_SUFFIX_PATTERN.matcher(normalized);
        if (numericWithLetters.matches()) {
            String prefix = numericWithLetters.group(1);
            String letters = numericWithLetters.group(2);
            return prefix + incrementLetters(letters);
        }

        Matcher onlyLetters = LETTER_HOUSE_NUMBER_PATTERN.matcher(normalized);
        if (onlyLetters.matches()) {
            return incrementLetters(onlyLetters.group(1));
        }

        Matcher onlyNumber = NUMERIC_HOUSE_NUMBER_PATTERN.matcher(normalized);
        if (!onlyNumber.matches()) {
            return null;
        }

        try {
            long number = Long.parseLong(onlyNumber.group(1));
            long incremented = number + normalizeIncrementStep(incrementStep);
            if (incremented < 0) {
                return null;
            }
            return Long.toString(incremented);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    String incrementNumberPartByOne(String current) {
        String normalized = normalize(current);
        Matcher matcher = HOUSE_NUMBER_WITH_OPTIONAL_SUFFIX_PATTERN.matcher(normalized);
        if (!matcher.matches()) {
            return null;
        }

        String prefix = matcher.group(1);
        String suffix = matcher.group(2);
        try {
            long number = Long.parseLong(prefix);
            return Long.toString(number + 1) + (suffix == null ? "" : suffix);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    String decrementNumberPartByOne(String current) {
        String normalized = normalize(current);
        Matcher matcher = HOUSE_NUMBER_WITH_OPTIONAL_SUFFIX_PATTERN.matcher(normalized);
        if (!matcher.matches()) {
            return null;
        }

        String prefix = matcher.group(1);
        String suffix = matcher.group(2);
        String decrementedPrefix = decrementNumericString(prefix);
        if (decrementedPrefix == null) {
            return null;
        }
        return decrementedPrefix + (suffix == null ? "" : suffix);
    }

    String incrementLetterPartByOne(String current) {
        String normalized = normalize(current);
        Matcher matcher = HOUSE_NUMBER_WITH_OPTIONAL_SUFFIX_PATTERN.matcher(normalized);
        if (!matcher.matches()) {
            return null;
        }

        String prefix = matcher.group(1);
        String suffix = matcher.group(2);
        String incrementedSuffix = (suffix == null || suffix.isEmpty()) ? "a" : incrementLetters(suffix);
        return prefix + incrementedSuffix;
    }

    String decrementLetterPartByOne(String current) {
        String normalized = normalize(current);
        Matcher matcher = HOUSE_NUMBER_WITH_OPTIONAL_SUFFIX_PATTERN.matcher(normalized);
        if (!matcher.matches()) {
            return null;
        }

        String prefix = matcher.group(1);
        String suffix = matcher.group(2);
        if (suffix == null || suffix.isEmpty()) {
            return null;
        }

        String decrementedSuffix = decrementLetters(suffix);
        if (decrementedSuffix == null) {
            return null;
        }
        return prefix + decrementedSuffix;
    }

    String toggleLetterSuffix(String current) {
        String normalized = normalize(current);
        Matcher matcher = HOUSE_NUMBER_WITH_OPTIONAL_SUFFIX_PATTERN.matcher(normalized);
        if (!matcher.matches()) {
            return null;
        }

        String prefix = matcher.group(1);
        String suffix = matcher.group(2);
        return (suffix == null || suffix.isEmpty()) ? prefix + "a" : prefix;
    }

    private String decrementNumericString(String value) {
        try {
            long number = Long.parseLong(value);
            if (number <= 0) {
                return null;
            }
            return Long.toString(number - 1);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String incrementLetters(String letters) {
        if (letters == null || letters.isEmpty()) {
            return letters;
        }

        char[] chars = letters.toCharArray();
        for (int i = chars.length - 1; i >= 0; i--) {
            char current = chars[i];
            boolean upperCase = Character.isUpperCase(current);
            char min = upperCase ? 'A' : 'a';
            char max = upperCase ? 'Z' : 'z';

            if (current == max) {
                chars[i] = min;
            } else {
                chars[i] = (char) (current + 1);
                return new String(chars);
            }
        }

        char leading = Character.isUpperCase(chars[0]) ? 'A' : 'a';
        return leading + new String(chars);
    }

    private String decrementLetters(String letters) {
        if (letters == null || letters.isEmpty()) {
            return null;
        }

        char[] chars = letters.toCharArray();
        boolean allMinimum = true;
        for (char current : chars) {
            char min = Character.isUpperCase(current) ? 'A' : 'a';
            if (current != min) {
                allMinimum = false;
                break;
            }
        }

        if (allMinimum) {
            if (chars.length == 1) {
                return "";
            }
            char max = Character.isUpperCase(chars[0]) ? 'Z' : 'z';
            return String.valueOf(max).repeat(chars.length - 1);
        }

        for (int i = chars.length - 1; i >= 0; i--) {
            char current = chars[i];
            char min = Character.isUpperCase(current) ? 'A' : 'a';
            if (current == min) {
                continue;
            }
            chars[i] = (char) (current - 1);
            for (int j = i + 1; j < chars.length; j++) {
                chars[j] = Character.isUpperCase(chars[j]) ? 'Z' : 'z';
            }
            return new String(chars);
        }
        return null;
    }
}

