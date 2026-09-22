package lithej.time;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.junit.jupiter.api.Test;

class TimesTest {

    private static final ZoneId ZONE_MINUS_11 = ZoneOffset.ofHours(-11);
    private static final ZoneId ZONE_PLUS_13 = ZoneOffset.ofHours(13);

    // Known fixed dates: 2024-03-16 is a Saturday, 2024-03-17 a Sunday, 2024-03-19 a Tuesday.
    private static final LocalDate KNOWN_SATURDAY = LocalDate.of(2024, 3, 16);
    private static final LocalDate KNOWN_SUNDAY = LocalDate.of(2024, 3, 17);
    private static final LocalDate KNOWN_TUESDAY = LocalDate.of(2024, 3, 19);

    @Test
    void nowReturnsCurrentInstant() {
        Instant before = Instant.now();
        Instant result = Times.now();
        Instant after = Instant.now().plusSeconds(5);
        assertThat(result).isAfterOrEqualTo(before).isBefore(after);
    }

    @Test
    void todayWithoutArgsMatchesSystemDefaultZone() {
        assertThat(Times.today()).isEqualTo(LocalDate.now(ZoneId.systemDefault()));
    }

    @Test
    void todayWithZoneMatchesJdkForTwoDistinctZones() {
        assertThat(Times.today(ZONE_MINUS_11)).isEqualTo(LocalDate.now(ZONE_MINUS_11));
        assertThat(Times.today(ZONE_PLUS_13)).isEqualTo(LocalDate.now(ZONE_PLUS_13));
    }

    @Test
    void todayWithZoneRejectsNull() {
        assertThatNullPointerException().isThrownBy(() -> Times.today(null));
    }

    @Test
    void nowAtMatchesJdkForTwoDistinctZones() {
        LocalDateTime before = LocalDateTime.now(ZONE_PLUS_13);
        LocalDateTime result = Times.nowAt(ZONE_PLUS_13);
        LocalDateTime after = LocalDateTime.now(ZONE_PLUS_13).plusSeconds(5);
        assertThat(result).isAfterOrEqualTo(before).isBefore(after);

        LocalDateTime otherZoneResult = Times.nowAt(ZONE_MINUS_11);
        assertThat(otherZoneResult.toLocalDate()).isEqualTo(LocalDate.now(ZONE_MINUS_11));
    }

    @Test
    void nowAtRejectsNull() {
        assertThatNullPointerException().isThrownBy(() -> Times.nowAt(null));
    }

    @Test
    void parseDateParsesValidInput() {
        assertThat(Times.parseDate("2024-03-15", DateTimeFormatter.ISO_LOCAL_DATE))
                .isEqualTo(LocalDate.of(2024, 3, 15));
    }

    @Test
    void parseDateRejectsMalformedInput() {
        assertThatExceptionOfType(DateTimeParseException.class)
                .isThrownBy(() -> Times.parseDate("not-a-date", DateTimeFormatter.ISO_LOCAL_DATE));
    }

    @Test
    void parseDateRejectsNullArguments() {
        assertThatNullPointerException().isThrownBy(() -> Times.parseDate(null, DateTimeFormatter.ISO_LOCAL_DATE));
        assertThatNullPointerException().isThrownBy(() -> Times.parseDate("2024-03-15", null));
    }

    @Test
    void parseDateTimeParsesValidInput() {
        assertThat(Times.parseDateTime("2024-03-15T10:30:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .isEqualTo(LocalDateTime.of(2024, 3, 15, 10, 30, 0));
    }

    @Test
    void parseDateTimeRejectsMalformedInput() {
        assertThatExceptionOfType(DateTimeParseException.class)
                .isThrownBy(() -> Times.parseDateTime("nope", DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

    @Test
    void parseDateTimeRejectsNullArguments() {
        assertThatNullPointerException()
                .isThrownBy(() -> Times.parseDateTime(null, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        assertThatNullPointerException()
                .isThrownBy(() -> Times.parseDateTime("2024-03-15T10:30:00", null));
    }

    @Test
    void formatRoundTripsWithParseDate() {
        LocalDate date = LocalDate.of(2024, 3, 15);
        String formatted = Times.format(date, DateTimeFormatter.ISO_LOCAL_DATE);
        assertThat(Times.parseDate(formatted, DateTimeFormatter.ISO_LOCAL_DATE)).isEqualTo(date);
    }

    @Test
    void formatRoundTripsWithParseDateTime() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 3, 15, 10, 30, 0);
        String formatted = Times.format(dateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        assertThat(Times.parseDateTime(formatted, DateTimeFormatter.ISO_LOCAL_DATE_TIME)).isEqualTo(dateTime);
    }

    @Test
    void formatRejectsNullArguments() {
        LocalDate date = LocalDate.of(2024, 3, 15);
        assertThatNullPointerException().isThrownBy(() -> Times.format(null, DateTimeFormatter.ISO_LOCAL_DATE));
        assertThatNullPointerException().isThrownBy(() -> Times.format(date, null));
    }

    @Test
    void daysBetweenIsPositiveWhenStartBeforeEnd() {
        assertThat(Times.daysBetween(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 11))).isEqualTo(10L);
    }

    @Test
    void daysBetweenIsNegativeWhenStartAfterEnd() {
        assertThat(Times.daysBetween(LocalDate.of(2024, 1, 11), LocalDate.of(2024, 1, 1))).isEqualTo(-10L);
    }

    @Test
    void daysBetweenIsZeroWhenStartEqualsEnd() {
        LocalDate date = LocalDate.of(2024, 1, 1);
        assertThat(Times.daysBetween(date, date)).isZero();
    }

    @Test
    void daysBetweenRejectsNullArguments() {
        LocalDate date = LocalDate.of(2024, 1, 1);
        assertThatNullPointerException().isThrownBy(() -> Times.daysBetween(null, date));
        assertThatNullPointerException().isThrownBy(() -> Times.daysBetween(date, null));
    }

    @Test
    void betweenComputesPositiveDuration() {
        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        Instant end = Instant.parse("2024-01-01T01:30:00Z");
        assertThat(Times.between(start, end)).isEqualTo(Duration.ofMinutes(90));
    }

    @Test
    void betweenRejectsNullArguments() {
        Instant instant = Instant.now();
        assertThatNullPointerException().isThrownBy(() -> Times.between(null, instant));
        assertThatNullPointerException().isThrownBy(() -> Times.between(instant, null));
    }

    @Test
    void ageComputesNormalCase() {
        assertThat(Times.age(LocalDate.of(2000, 1, 1), LocalDate.of(2024, 6, 1))).isEqualTo(24);
    }

    @Test
    void ageAccountsForBirthdayNotYetOccurredThisYear() {
        // Birthday is Dec 1; asOf is June 1, so the birthday has not yet occurred this
        // year and the age must be one less than naive year subtraction (2024 - 2000 = 24).
        assertThat(Times.age(LocalDate.of(2000, 12, 1), LocalDate.of(2024, 6, 1))).isEqualTo(23);
    }

    @Test
    void ageOnExactBirthdayMatch() {
        assertThat(Times.age(LocalDate.of(2000, 6, 1), LocalDate.of(2024, 6, 1))).isEqualTo(24);
    }

    @Test
    void ageRejectsAsOfBeforeBirthDate() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Times.age(LocalDate.of(2024, 6, 1), LocalDate.of(2000, 1, 1)));
    }

    @Test
    void ageRejectsNullArguments() {
        LocalDate date = LocalDate.of(2000, 1, 1);
        assertThatNullPointerException().isThrownBy(() -> Times.age(null, date));
        assertThatNullPointerException().isThrownBy(() -> Times.age(date, (LocalDate) null));
    }

    @Test
    void ageWithZoneUsesTodayInThatZone() {
        LocalDate birthDate = LocalDate.of(1990, 1, 1);
        int expected = Times.age(birthDate, Times.today(ZONE_PLUS_13));
        assertThat(Times.age(birthDate, ZONE_PLUS_13)).isEqualTo(expected);
    }

    @Test
    void ageWithZoneRejectsNullArguments() {
        LocalDate date = LocalDate.of(1990, 1, 1);
        assertThatNullPointerException().isThrownBy(() -> Times.age(null, ZONE_PLUS_13));
        assertThatNullPointerException().isThrownBy(() -> Times.age(date, (ZoneId) null));
    }

    @Test
    void isPastInstantDetectsPastAndFuture() {
        assertThat(Times.isPast(Instant.EPOCH)).isTrue();
        assertThat(Times.isPast(Instant.now().plusSeconds(3600L * 24 * 365 * 10))).isFalse();
    }

    @Test
    void isPastInstantRejectsNull() {
        assertThatNullPointerException().isThrownBy(() -> Times.isPast((Instant) null));
    }

    @Test
    void isFutureInstantDetectsPastAndFuture() {
        assertThat(Times.isFuture(Instant.EPOCH)).isFalse();
        assertThat(Times.isFuture(Instant.now().plusSeconds(3600L * 24 * 365 * 10))).isTrue();
    }

    @Test
    void isFutureInstantRejectsNull() {
        assertThatNullPointerException().isThrownBy(() -> Times.isFuture((Instant) null));
    }

    @Test
    void isPastDateComparesAgainstTodayInZone() {
        LocalDate farPast = LocalDate.of(1900, 1, 1);
        LocalDate farFuture = LocalDate.of(2999, 1, 1);
        assertThat(Times.isPast(farPast, ZONE_PLUS_13)).isTrue();
        assertThat(Times.isPast(farFuture, ZONE_PLUS_13)).isFalse();
    }

    @Test
    void isPastDateRejectsNullArguments() {
        LocalDate date = LocalDate.of(1900, 1, 1);
        assertThatNullPointerException().isThrownBy(() -> Times.isPast(null, ZONE_PLUS_13));
        assertThatNullPointerException().isThrownBy(() -> Times.isPast(date, (ZoneId) null));
    }

    @Test
    void isFutureDateComparesAgainstTodayInZone() {
        LocalDate farPast = LocalDate.of(1900, 1, 1);
        LocalDate farFuture = LocalDate.of(2999, 1, 1);
        assertThat(Times.isFuture(farPast, ZONE_PLUS_13)).isFalse();
        assertThat(Times.isFuture(farFuture, ZONE_PLUS_13)).isTrue();
    }

    @Test
    void isFutureDateRejectsNullArguments() {
        LocalDate date = LocalDate.of(2999, 1, 1);
        assertThatNullPointerException().isThrownBy(() -> Times.isFuture(null, ZONE_PLUS_13));
        assertThatNullPointerException().isThrownBy(() -> Times.isFuture(date, (ZoneId) null));
    }

    @Test
    void isWeekendRecognizesSaturdayAndSunday() {
        assertThat(Times.isWeekend(KNOWN_SATURDAY)).isTrue();
        assertThat(Times.isWeekend(KNOWN_SUNDAY)).isTrue();
        assertThat(Times.isWeekend(KNOWN_TUESDAY)).isFalse();
    }

    @Test
    void isWeekendRejectsNull() {
        assertThatNullPointerException().isThrownBy(() -> Times.isWeekend(null));
    }

    @Test
    void isWeekdayIsInverseOfWeekend() {
        assertThat(Times.isWeekday(KNOWN_SATURDAY)).isFalse();
        assertThat(Times.isWeekday(KNOWN_SUNDAY)).isFalse();
        assertThat(Times.isWeekday(KNOWN_TUESDAY)).isTrue();
    }

    @Test
    void isWeekdayRejectsNull() {
        assertThatNullPointerException().isThrownBy(() -> Times.isWeekday(null));
    }

    @Test
    void atZoneCombinesInstantAndZone() {
        Instant instant = Instant.parse("2024-03-15T10:30:00Z");
        ZonedDateTime result = Times.atZone(instant, ZoneOffset.UTC);
        assertThat(result).isEqualTo(instant.atZone(ZoneOffset.UTC));
    }

    @Test
    void atZoneRejectsNullArguments() {
        Instant instant = Instant.now();
        assertThatNullPointerException().isThrownBy(() -> Times.atZone(null, ZoneOffset.UTC));
        assertThatNullPointerException().isThrownBy(() -> Times.atZone(instant, null));
    }
}
