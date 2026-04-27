package com.openrecords.api.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility for calculating dates that skip weekends and federal holidays.
 *
 * Used for FOIA SLA due date calculation, which must be 20 business days
 * after the request is acknowledged (5 U.S.C. § 552(a)(6)).
 *
 * Holiday handling: a starter set of fixed federal holidays. For production
 * a full year-aware holiday calendar would be needed (e.g., Easter,
 * floating holidays like MLK Day = third Monday of January). This implementation
 * is intentionally pragmatic — covers ~80% of cases for portfolio purposes.
 */
public final class BusinessDayCalculator {

    private BusinessDayCalculator() {}  // utility class — no instances

    /**
     * Add N business days to a starting date.
     * Business days = Monday through Friday, excluding federal holidays.
     */
    public static LocalDate addBusinessDays(LocalDate startDate, int businessDays) {
        if (businessDays < 0) {
            throw new IllegalArgumentException("businessDays must be non-negative");
        }

        LocalDate result = startDate;
        int added = 0;

        while (added < businessDays) {
            result = result.plusDays(1);
            if (isBusinessDay(result)) {
                added++;
            }
        }

        return result;
    }

    /**
     * @return true if the date is a Monday-through-Friday, non-federal-holiday date.
     */
    public static boolean isBusinessDay(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false;
        }
        return !isFederalHoliday(date);
    }

    /**
     * Returns true if the date is a recognized US federal holiday.
     *
     * Covers fixed-date holidays + a few common observance rules.
     * For a full implementation, consider a holiday library or a database table.
     */
    public static boolean isFederalHoliday(LocalDate date) {
        return getFederalHolidaysForYear(date.getYear()).contains(date);
    }

    /**
     * Computes US federal holidays for a given year.
     * Federal employees observe these per 5 U.S.C. § 6103.
     */
    private static Set<LocalDate> getFederalHolidaysForYear(int year) {
        Set<LocalDate> holidays = new HashSet<>();

        // ============================================================
        // Fixed-date federal holidays (with weekend-roll-forward/back rules)
        // ============================================================
        holidays.add(observed(LocalDate.of(year, 1, 1)));   // New Year's Day
        holidays.add(observed(LocalDate.of(year, 6, 19)));  // Juneteenth
        holidays.add(observed(LocalDate.of(year, 7, 4)));   // Independence Day
        holidays.add(observed(LocalDate.of(year, 11, 11))); // Veterans Day
        holidays.add(observed(LocalDate.of(year, 12, 25))); // Christmas Day

        // ============================================================
        // Floating holidays (specific weekdays in specific months)
        // ============================================================

        // Martin Luther King Jr. Day — third Monday in January
        holidays.add(LocalDate.of(year, 1, 1)
            .with(TemporalAdjusters.dayOfWeekInMonth(3, DayOfWeek.MONDAY)));

        // Presidents Day — third Monday in February
        holidays.add(LocalDate.of(year, 2, 1)
            .with(TemporalAdjusters.dayOfWeekInMonth(3, DayOfWeek.MONDAY)));

        // Memorial Day — last Monday in May
        holidays.add(LocalDate.of(year, 5, 1)
            .with(TemporalAdjusters.lastInMonth(DayOfWeek.MONDAY)));

        // Labor Day — first Monday in September
        holidays.add(LocalDate.of(year, 9, 1)
            .with(TemporalAdjusters.dayOfWeekInMonth(1, DayOfWeek.MONDAY)));

        // Columbus Day — second Monday in October
        holidays.add(LocalDate.of(year, 10, 1)
            .with(TemporalAdjusters.dayOfWeekInMonth(2, DayOfWeek.MONDAY)));

        // Thanksgiving — fourth Thursday in November
        holidays.add(LocalDate.of(year, 11, 1)
            .with(TemporalAdjusters.dayOfWeekInMonth(4, DayOfWeek.THURSDAY)));

        return holidays;
    }

    /**
     * If a fixed-date holiday falls on a weekend, the federal observance day shifts:
     *   - Saturday → preceding Friday
     *   - Sunday → following Monday
     */
    private static LocalDate observed(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY) {
            return date.minusDays(1);
        }
        if (day == DayOfWeek.SUNDAY) {
            return date.plusDays(1);
        }
        return date;
    }
}