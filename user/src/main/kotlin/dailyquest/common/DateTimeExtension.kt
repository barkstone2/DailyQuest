package dailyquest.common

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjuster
import java.time.temporal.TemporalAdjusters

private val firstDayOfYearAdjusters: TemporalAdjuster = TemporalAdjusters.firstDayOfYear()
private val firstMondayOfYearAdjusters: TemporalAdjuster = TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY)

fun LocalDate.firstDayOfQuarter(): LocalDate {

    val firstMondayOfYear = this.with(firstDayOfYearAdjusters).with(firstMondayOfYearAdjusters)

    if(this.isBefore(firstMondayOfYear)) {
        val previousMonday = this.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return previousMonday.minusWeeks(12)
    }

    val weekDiff = (this.dayOfYear - firstMondayOfYear.dayOfYear) / 7

    val quarter = weekDiff / 13
    val weeks = quarter * 13L

    return firstMondayOfYear.plusWeeks(weeks)
}

fun LocalDate.lastDayOfQuarter(): LocalDate {

    val firstMondayOfYear = this.with(firstDayOfYearAdjusters).with(firstMondayOfYearAdjusters)

    if(this.isBefore(firstMondayOfYear)) {
        return firstMondayOfYear.minusDays(1)
    }

    val weekDiff = (this.dayOfYear - firstMondayOfYear.dayOfYear) / 7

    val quarter = weekDiff / 13
    val weeks = (quarter + 1) * 13L

    return firstMondayOfYear.plusWeeks(weeks).minusDays(1)
}

fun LocalDate.firstDayOfWeek() : LocalDate {
    return LocalDate.from(this.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))
}

fun LocalDate.firstDayOfMonth() : LocalDate {
    return LocalDate.from(this.with(TemporalAdjusters.firstDayOfMonth()))
}

fun LocalDateTime.hoursSinceNow(): Long {
    val now = LocalDateTime.now()
    val difference = Duration.between(now, this)
    return difference.toHours()
}

fun LocalDateTime.minutesSinceNow(): Long {
    val now = LocalDateTime.now()
    val difference = Duration.between(now, this)
    return difference.toMinutes()
}

fun LocalDateTime.timeSinceNowAsString(): String {
    return String.format("%d시간 %d분", this.hoursSinceNow(), this.minutesSinceNow() % 60)
}
