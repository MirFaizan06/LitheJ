package lithej.time;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import lithej.core.Validate;

/**
 * Ergonomic wrappers around {@code java.time} for the current moment, parsing,
 * formatting, comparisons and durations.
 *
 * <p>This class is deliberately not a competing date/time framework: every method
 * accepts and returns standard {@code java.time} types ({@link Instant},
 * {@link LocalDate}, {@link LocalDateTime}, {@link ZonedDateTime}, {@link Duration}),
 * and for anything beyond this small surface, drop straight down to {@code java.time}
 * itself.
 *
 * <p><b>Timezone discipline:</b> any method here that must render "the current moment"
 * as a calendar date or date-time takes an explicit {@link ZoneId} parameter; none of
 * them silently default to {@link ZoneId#systemDefault()}. The single documented
 * exception is {@link #today()}, since "today" inherently means "today where I am" —
 * its Javadoc calls out the implicit zone in bold and points to {@link #today(ZoneId)}
 * as the explicit alternative. Methods that operate purely on {@link Instant} (which is
 * a point on the UTC timeline, not a calendar date/time) take no zone parameter because
 * none is needed.
 *
 * <p><b>Thread safety:</b> this class is stateless and thread-safe.
 */
public final class Times {

    private static final String PARAM_ZONE = "zone";

    private Times() {
    }

    /**
     * Returns the current instant.
     *
     * <p>Equivalent to {@link Instant#now()}; provided so callers can stay within the
     * {@code Times} API for consistency and readability alongside the rest of this
     * class's methods.
     *
     * @return the current instant
     */
    public static Instant now() {
        return Instant.now();
    }

    /**
     * Returns today's date using the JVM's default time zone.
     *
     * <p><b>This method uses {@link ZoneId#systemDefault()} implicitly.</b> This is the
     * one deliberate exception to this class's rule of never silently assuming a time
     * zone: "today" inherently means "today where I am." If you know the zone you care
     * about (e.g. a user's stored preference, or a server that must behave the same
     * regardless of the host's configured zone), use {@link #today(ZoneId)} instead.
     *
     * @return today's date in the system default zone
     */
    public static LocalDate today() {
        return LocalDate.now(ZoneId.systemDefault());
    }

    /**
     * Returns today's date in {@code zone}.
     *
     * @param zone the zone to compute today's date in
     * @return today's date in {@code zone}
     * @throws NullPointerException if {@code zone} is {@code null}
     */
    public static LocalDate today(ZoneId zone) {
        Validate.notNull(zone, PARAM_ZONE);
        return LocalDate.now(zone);
    }

    /**
     * Returns the current date-time in {@code zone}.
     *
     * @param zone the zone to compute the current date-time in
     * @return the current date-time in {@code zone}
     * @throws NullPointerException if {@code zone} is {@code null}
     */
    public static LocalDateTime nowAt(ZoneId zone) {
        Validate.notNull(zone, PARAM_ZONE);
        return LocalDateTime.now(zone);
    }

    /**
     * Parses {@code text} as a {@link LocalDate} using {@code formatter}.
     *
     * <p>Failures are not swallowed: a malformed {@code text} propagates the JDK's own
     * {@link DateTimeParseException} rather than being wrapped or converted.
     *
     * <pre>{@code
     * LocalDate date = Times.parseDate("2024-03-15", DateTimeFormatter.ISO_LOCAL_DATE);
     * }</pre>
     *
     * @param text the text to parse
     * @param formatter the formatter to parse with
     * @return the parsed date
     * @throws NullPointerException if {@code text} or {@code formatter} is {@code null}
     * @throws DateTimeParseException if {@code text} cannot be parsed as a
     *     {@link LocalDate} using {@code formatter}
     */
    public static LocalDate parseDate(String text, DateTimeFormatter formatter) {
        Validate.notNull(text, "text");
        Validate.notNull(formatter, "formatter");
        return LocalDate.parse(text, formatter);
    }

    /**
     * Parses {@code text} as a {@link LocalDateTime} using {@code formatter}.
     *
     * <p>Failures are not swallowed: a malformed {@code text} propagates the JDK's own
     * {@link DateTimeParseException} rather than being wrapped or converted.
     *
     * <pre>{@code
     * LocalDateTime dt = Times.parseDateTime("2024-03-15T10:30:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME);
     * }</pre>
     *
     * @param text the text to parse
     * @param formatter the formatter to parse with
     * @return the parsed date-time
     * @throws NullPointerException if {@code text} or {@code formatter} is {@code null}
     * @throws DateTimeParseException if {@code text} cannot be parsed as a
     *     {@link LocalDateTime} using {@code formatter}
     */
    public static LocalDateTime parseDateTime(String text, DateTimeFormatter formatter) {
        Validate.notNull(text, "text");
        Validate.notNull(formatter, "formatter");
        return LocalDateTime.parse(text, formatter);
    }

    /**
     * Formats {@code value} using {@code formatter}.
     *
     * <p>A thin wrapper around {@link DateTimeFormatter#format(TemporalAccessor)},
     * provided for API symmetry and discoverability alongside {@link #parseDate} and
     * {@link #parseDateTime}. Arguments are validated up front with a clear message
     * rather than relying on the JDK's own exception.
     *
     * @param value the value to format
     * @param formatter the formatter to format with
     * @return the formatted text
     * @throws NullPointerException if {@code value} or {@code formatter} is {@code null}
     */
    public static String format(TemporalAccessor value, DateTimeFormatter formatter) {
        Validate.notNull(value, "value");
        Validate.notNull(formatter, "formatter");
        return formatter.format(value);
    }

    /**
     * Returns the number of days between {@code start} and {@code end}.
     *
     * <p>The result is negative if {@code end} is before {@code start}, and zero if the
     * two dates are equal, matching {@link ChronoUnit#DAYS}.
     *
     * @param start the start date
     * @param end the end date
     * @return the number of days from {@code start} to {@code end}
     * @throws NullPointerException if {@code start} or {@code end} is {@code null}
     */
    public static long daysBetween(LocalDate start, LocalDate end) {
        Validate.notNull(start, "start");
        Validate.notNull(end, "end");
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * Returns the duration between {@code start} and {@code end}.
     *
     * <p>Zone-independent: {@link Instant} is a point on the UTC timeline, so no zone is
     * involved. Equivalent to {@link Duration#between(java.time.temporal.Temporal,
     * java.time.temporal.Temporal)}.
     *
     * @param start the start instant
     * @param end the end instant
     * @return the duration from {@code start} to {@code end}; negative if {@code end} is
     *     before {@code start}
     * @throws NullPointerException if {@code start} or {@code end} is {@code null}
     */
    public static Duration between(Instant start, Instant end) {
        Validate.notNull(start, "start");
        Validate.notNull(end, "end");
        return Duration.between(start, end);
    }

    /**
     * Returns the age in whole years of someone born on {@code birthDate}, as of
     * {@code asOf}.
     *
     * @param birthDate the date of birth
     * @param asOf the date to compute the age as of
     * @return the number of whole years between {@code birthDate} and {@code asOf}
     * @throws NullPointerException if {@code birthDate} or {@code asOf} is {@code null}
     * @throws IllegalArgumentException if {@code asOf} is before {@code birthDate}
     */
    public static int age(LocalDate birthDate, LocalDate asOf) {
        Validate.notNull(birthDate, "birthDate");
        Validate.notNull(asOf, "asOf");
        if (asOf.isBefore(birthDate)) {
            throw new IllegalArgumentException("asOf (" + asOf + ") must not be before birthDate (" + birthDate + ")");
        }
        return Period.between(birthDate, asOf).getYears();
    }

    /**
     * Returns the age in whole years of someone born on {@code birthDate}, as of today's
     * date in {@code zone}.
     *
     * <p>The result depends on the current date in {@code zone}, i.e. it is
     * {@code age(birthDate, today(zone))}.
     *
     * @param birthDate the date of birth
     * @param zone the zone to compute today's date in
     * @return the number of whole years between {@code birthDate} and today's date in
     *     {@code zone}
     * @throws NullPointerException if {@code birthDate} or {@code zone} is {@code null}
     * @throws IllegalArgumentException if today's date in {@code zone} is before
     *     {@code birthDate}
     */
    public static int age(LocalDate birthDate, ZoneId zone) {
        Validate.notNull(birthDate, "birthDate");
        Validate.notNull(zone, PARAM_ZONE);
        return age(birthDate, today(zone));
    }

    /**
     * Returns {@code true} if {@code instant} is strictly before the current instant.
     *
     * <p>Zone-independent: equivalent to {@code instant.isBefore(Instant.now())}.
     *
     * @param instant the instant to check
     * @return {@code true} if {@code instant} is before now
     * @throws NullPointerException if {@code instant} is {@code null}
     */
    public static boolean isPast(Instant instant) {
        Validate.notNull(instant, "instant");
        return instant.isBefore(Instant.now());
    }

    /**
     * Returns {@code true} if {@code instant} is strictly after the current instant.
     *
     * <p>Zone-independent: equivalent to {@code instant.isAfter(Instant.now())}. Because
     * both {@link #isPast(Instant)} and this method use strict {@code isBefore}/
     * {@code isAfter} comparisons, an instant that is (in the rare, sub-millisecond
     * case) exactly equal to "now" is reported as neither past nor future — both methods
     * return {@code false} for it. This is expected and matches the strict comparison
     * semantics of {@link Instant#isBefore} and {@link Instant#isAfter}.
     *
     * @param instant the instant to check
     * @return {@code true} if {@code instant} is after now
     * @throws NullPointerException if {@code instant} is {@code null}
     */
    public static boolean isFuture(Instant instant) {
        Validate.notNull(instant, "instant");
        return instant.isAfter(Instant.now());
    }

    /**
     * Returns {@code true} if {@code date} is strictly before today's date in
     * {@code zone}.
     *
     * @param date the date to check
     * @param zone the zone to compute today's date in
     * @return {@code true} if {@code date} is before today's date in {@code zone}
     * @throws NullPointerException if {@code date} or {@code zone} is {@code null}
     */
    public static boolean isPast(LocalDate date, ZoneId zone) {
        Validate.notNull(date, "date");
        Validate.notNull(zone, PARAM_ZONE);
        return date.isBefore(today(zone));
    }

    /**
     * Returns {@code true} if {@code date} is strictly after today's date in
     * {@code zone}.
     *
     * @param date the date to check
     * @param zone the zone to compute today's date in
     * @return {@code true} if {@code date} is after today's date in {@code zone}
     * @throws NullPointerException if {@code date} or {@code zone} is {@code null}
     */
    public static boolean isFuture(LocalDate date, ZoneId zone) {
        Validate.notNull(date, "date");
        Validate.notNull(zone, PARAM_ZONE);
        return date.isAfter(today(zone));
    }

    /**
     * Returns {@code true} if {@code date} falls on a Saturday or Sunday.
     *
     * <p>This uses the ISO/Western convention of a Saturday-Sunday weekend and does not
     * account for locale-specific weekends observed elsewhere (e.g. Friday-Saturday in
     * some countries). Callers with locale-specific requirements should check
     * {@link LocalDate#getDayOfWeek()} directly.
     *
     * @param date the date to check
     * @return {@code true} if {@code date} is a {@link DayOfWeek#SATURDAY} or
     *     {@link DayOfWeek#SUNDAY}
     * @throws NullPointerException if {@code date} is {@code null}
     */
    public static boolean isWeekend(LocalDate date) {
        Validate.notNull(date, "date");
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    /**
     * Returns {@code true} if {@code date} does not fall on a Saturday or Sunday.
     *
     * <p>The inverse of {@link #isWeekend(LocalDate)}; the same ISO/Western-convention
     * caveat applies.
     *
     * @param date the date to check
     * @return {@code true} if {@code date} is not a {@link DayOfWeek#SATURDAY} or
     *     {@link DayOfWeek#SUNDAY}
     * @throws NullPointerException if {@code date} is {@code null}
     */
    public static boolean isWeekday(LocalDate date) {
        return !isWeekend(date);
    }

    /**
     * Combines {@code instant} with {@code zone} into a {@link ZonedDateTime}.
     *
     * <p>A thin wrapper around {@link Instant#atZone(ZoneId)}, provided for API
     * discoverability and symmetry alongside the rest of this class.
     *
     * @param instant the instant to combine
     * @param zone the zone to combine it with
     * @return {@code instant} expressed as a {@link ZonedDateTime} in {@code zone}
     * @throws NullPointerException if {@code instant} or {@code zone} is {@code null}
     */
    public static ZonedDateTime atZone(Instant instant, ZoneId zone) {
        Validate.notNull(instant, "instant");
        Validate.notNull(zone, PARAM_ZONE);
        return instant.atZone(zone);
    }
}
