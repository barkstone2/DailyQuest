package dailyquest.common

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate

class TimeUtilUnitTest {

    @DisplayName("분기 시작일 조회 시")
    @Nested
    inner class GetQuarterStartDate {

        @DisplayName("시작일이 월요일이 아니면 분기 범위 이전 월요일을 반환한다")
        @Test
        fun `시작일이 월요일이 아니면 분기 범위 이전 월요일을 반환한다`() {
            //given
            // 분기 시작일 - 2023/07/01 일요일
            val date = LocalDate.of(2023, 7, 1)

            //when
            val firstDayOfQuarter = date.firstDayOfQuarter()

            //then
            assertThat(firstDayOfQuarter.dayOfWeek).isEqualTo(DayOfWeek.MONDAY)
            assertThat(firstDayOfQuarter).isBefore(date)
        }

        @DisplayName("시작일이 월요일이면 시작일이 반환된다")
        @Test
        fun `시작일이 월요일이면 시작일이 반환된다`() {
            //given
            // 분기 시작일 - 2019/07/01 월요일
            val date = LocalDate.of(2019, 7, 1)

            //when
            val firstDayOfQuarter = date.firstDayOfQuarter()

            //then
            assertThat(firstDayOfQuarter.dayOfWeek).isEqualTo(DayOfWeek.MONDAY)
            assertThat(firstDayOfQuarter).isEqualTo(date)
        }

        @DisplayName("호출 날짜에 알맞은 분기가 선택된다")
        @Test
        fun `호출 날짜에 알맞은 분기가 선택된다`() {
            //given
            val firstQuarterOfDate1 = LocalDate.of(2023, 1, 1)
            val firstQuarterOfDate2 = firstQuarterOfDate1.plusWeeks(12)

            val secondQuarterOfDate1 = firstQuarterOfDate1.plusWeeks(13)
            val secondQuarterOfDate2 = secondQuarterOfDate1.plusWeeks(13).minusDays(1)
            val thirdQuarterOfDate1 = secondQuarterOfDate1.plusWeeks(13)
            val thirdQuarterOfDate2 = thirdQuarterOfDate1.plusWeeks(13).minusDays(1)
            val fourthQuarterOfDate1 = thirdQuarterOfDate1.plusWeeks(13)
            val fourthQuarterOfDate2 = fourthQuarterOfDate1.plusWeeks(13).minusDays(1)

            //when
            val firstDayOfFirstQuarter1 = firstQuarterOfDate1.firstDayOfQuarter()
            val firstDayOfFirstQuarter2 = firstQuarterOfDate2.firstDayOfQuarter()

            val lastDayOfFirstQuarter1 = firstQuarterOfDate1.lastDayOfQuarter()
            val lastDayOfFirstQuarter2 = firstQuarterOfDate1.lastDayOfQuarter()

            val firstDayOfSecondQuarter1 = secondQuarterOfDate1.firstDayOfQuarter()
            val firstDayOfSecondQuarter2 = secondQuarterOfDate2.firstDayOfQuarter()
            val lastDayOfSecondQuarter1 = secondQuarterOfDate1.lastDayOfQuarter()
            val lastDayOfSecondQuarter2 = secondQuarterOfDate1.lastDayOfQuarter()

            val firstDayOfThirdQuarter1 = thirdQuarterOfDate1.firstDayOfQuarter()
            val firstDayOfThirdQuarter2 = thirdQuarterOfDate2.firstDayOfQuarter()
            val lastDayOfThirdQuarter1 = thirdQuarterOfDate1.lastDayOfQuarter()
            val lastDayOfThirdQuarter2 = thirdQuarterOfDate1.lastDayOfQuarter()

            val firstDayOfFourthQuarter1 = fourthQuarterOfDate1.firstDayOfQuarter()
            val firstDayOfFourthQuarter2 = fourthQuarterOfDate2.firstDayOfQuarter()
            val lastDayOfFourthQuarter1 = fourthQuarterOfDate1.lastDayOfQuarter()
            val lastDayOfFourthQuarter2 = fourthQuarterOfDate1.lastDayOfQuarter()

            //then
            assertThat(firstDayOfFirstQuarter1).isEqualTo(firstDayOfFirstQuarter2)
            assertThat(lastDayOfFirstQuarter1).isEqualTo(lastDayOfFirstQuarter2)

            assertThat(firstDayOfSecondQuarter1).isEqualTo(firstDayOfSecondQuarter2)
            assertThat(lastDayOfSecondQuarter1).isEqualTo(lastDayOfSecondQuarter2)

            assertThat(firstDayOfThirdQuarter1).isEqualTo(firstDayOfThirdQuarter2)
            assertThat(lastDayOfThirdQuarter1).isEqualTo(lastDayOfThirdQuarter2)

            assertThat(firstDayOfFourthQuarter1).isEqualTo(firstDayOfFourthQuarter2)
            assertThat(lastDayOfFourthQuarter1).isEqualTo(lastDayOfFourthQuarter2)

            assertThat(firstDayOfFirstQuarter1)
                .isNotEqualTo(firstDayOfSecondQuarter1)
                .isNotEqualTo(firstDayOfThirdQuarter1)
                .isNotEqualTo(firstDayOfFourthQuarter1)

            assertThat(lastDayOfFirstQuarter1)
                .isNotEqualTo(lastDayOfSecondQuarter1)
                .isNotEqualTo(lastDayOfThirdQuarter1)
                .isNotEqualTo(lastDayOfFourthQuarter1)
        }

    }

}