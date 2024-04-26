package dailyquest.achievement.repository

import dailyquest.achievement.entity.Achievement
import dailyquest.achievement.entity.AchievementAchieveLog
import dailyquest.achievement.entity.AchievementType.QUEST_COMPLETION
import dailyquest.achievement.entity.AchievementType.QUEST_REGISTRATION
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.domain.Pageable

@ExtendWith(MockKExtension::class)
@DisplayName("업적 리포지토리 유닛 테스트")
@DataJpaTest
class AchievementRepositoryUnitTest {

    @Autowired
    private lateinit var achievementRepository: AchievementRepository

    @Autowired
    private lateinit var achieveLogRepository: AchievementAchieveLogRepository

    @DisplayName("findNotAchievedAchievement 호출 시")
    @Nested
    inner class TestFindNotAchievedAchievement {
        @DisplayName("등록된 업적이 없으면 null이 반환된다")
        @Test
        fun `등록된 업적이 없으면 null이 반환된다`() {
            //when
            val result = achievementRepository.findNotAchievedAchievement(QUEST_REGISTRATION, 1L)

            //then
            assertThat(result).isNull()
        }

        @DisplayName("인자로 넘어온 타입의 업적이 조회된다")
        @Test
        fun `인자로 넘어온 타입의 업적이 조회된다`() {
            //given
            val achievementType = QUEST_REGISTRATION
            achievementRepository.save(Achievement("", "", achievementType, 1))
            achievementRepository.save(Achievement("", "", QUEST_COMPLETION, 1))

            //when
            val result = achievementRepository.findNotAchievedAchievement(achievementType, 1L)

            //then
            assertThat(result?.type).isEqualTo(achievementType)
        }

        @DisplayName("다른 타입 업적의 목표 횟수가 더 작아도 인자로 넘어온 타입 업적이 조회된다")
        @Test
        fun `다른 타입 업적의 목표 횟수가 더 작아도 인자로 넘어온 타입 업적이 조회된다`() {
            //given
            val achievementType = QUEST_REGISTRATION
            achievementRepository.save(Achievement("", "", achievementType, 10))
            achievementRepository.save(Achievement("", "", QUEST_COMPLETION, 1))

            //when
            val result = achievementRepository.findNotAchievedAchievement(achievementType, 1L)

            //then
            assertThat(result?.type).isEqualTo(achievementType)
        }

        @DisplayName("인자로 넘어온 유저가 달성하지 않은 업적이 조회된다")
        @Test
        fun `인자로 넘어온 유저가 달성하지 않은 업적이 조회된다`() {
            //given
            val type = QUEST_REGISTRATION
            val userId = 1L
            val achievedAchievement = Achievement("", "", type, 1)
            val notAchievedAchievement = Achievement("", "", type, 2)
            achievementRepository.save(achievedAchievement)
            achievementRepository.save(notAchievedAchievement)
            achieveLogRepository.save(AchievementAchieveLog.of(achievedAchievement, userId))

            //when
            val result = achievementRepository.findNotAchievedAchievement(type, userId)

            //then
            assertThat(result).isEqualTo(notAchievedAchievement)
        }

        @DisplayName("다른 유저가 달성한 업적이라도 현재 유저가 달성하지 않았다면 조회된다")
        @Test
        fun `다른 유저가 달성한 업적이라도 현재 유저가 달성하지 않았다면 조회된다`() {
            //given
            val type = QUEST_REGISTRATION
            val userId = 1L
            val achievedAchievementByOther = Achievement("", "", type, 1)
            achievementRepository.save(achievedAchievementByOther)
            achieveLogRepository.save(AchievementAchieveLog.of(achievedAchievementByOther, userId + 1))

            //when
            val result = achievementRepository.findNotAchievedAchievement(type, userId)

            //then
            assertThat(result).isEqualTo(achievedAchievementByOther)
        }

        @DisplayName("달성한 업적의 목표 횟수가 더 작아도 달성하지 않은 업적이 조회된다")
        @Test
        fun `달성한 업적의 목표 횟수가 더 작아도 달성하지 않은 업적이 조회된다`() {
            //given
            val type = QUEST_REGISTRATION
            val userId = 1L
            val achievedAchievement = Achievement("", "", type, 1)
            val notAchievedAchievement = Achievement("", "", type, 10)
            achievementRepository.save(achievedAchievement)
            achievementRepository.save(notAchievedAchievement)
            achieveLogRepository.save(AchievementAchieveLog.of(achievedAchievement, userId))

            //when
            val result = achievementRepository.findNotAchievedAchievement(type, userId)

            //then
            assertThat(result).isEqualTo(notAchievedAchievement)
        }

        @DisplayName("달성한 업적의 목표 횟수가 더 커도 달성하지 않은 업적이 조회된다")
        @Test
        fun `달성한 업적의 목표 횟수가 더 커도 달성하지 않은 업적이 조회된다`() {
            //given
            val type = QUEST_REGISTRATION
            val userId = 1L
            val achievedAchievement = Achievement("", "", type, 10)
            val notAchievedAchievement = Achievement("", "", type, 1)
            achievementRepository.save(achievedAchievement)
            achievementRepository.save(notAchievedAchievement)
            achieveLogRepository.save(AchievementAchieveLog.of(achievedAchievement, userId))

            //when
            val result = achievementRepository.findNotAchievedAchievement(type, userId)

            //then
            assertThat(result).isEqualTo(notAchievedAchievement)
        }

        @DisplayName("달성한 업적 뿐이면 null이 반환된다")
        @Test
        fun `달성한 업적 뿐이면 null이 반환된다`() {
            //given
            val type = QUEST_REGISTRATION
            val userId = 1L
            val achievedAchievement = Achievement("", "", type, 1)
            achievementRepository.save(achievedAchievement)
            achieveLogRepository.save(AchievementAchieveLog.of(achievedAchievement, userId))

            //when
            val result = achievementRepository.findNotAchievedAchievement(type, userId)

            //then
            assertThat(result).isNull()
        }

        @DisplayName("달성하지 않은 업적 중 목표 횟수가 가장 적은 업적이 조회된다")
        @Test
        fun `달성하지 않은 업적 중 목표 횟수가 가장 적은 업적이 조회된다`() {
            //given
            val type = QUEST_REGISTRATION
            val userId = 1L
            val smallestTargetAchievement = Achievement("", "", type, 1)
            val otherAchievement = Achievement("", "", type, 2)
            achievementRepository.save(smallestTargetAchievement)
            achievementRepository.save(otherAchievement)

            //when
            val result = achievementRepository.findNotAchievedAchievement(type, userId)

            //then
            assertThat(result).isEqualTo(smallestTargetAchievement)
        }

        @DisplayName("비활성화 상태의 업적은 조회되지 않는다")
        @Test
        fun `비활성화 상태의 업적은 조회되지 않는다`() {
            //given
            val type = QUEST_REGISTRATION
            val userId = 1L
            val inactiveAchievement = Achievement("", "", type, 1)
            inactiveAchievement.inactivateAchievement()
            val activeAchievement = Achievement("", "", type, 2)
            achievementRepository.save(inactiveAchievement)
            achievementRepository.save(activeAchievement)

            //when
            val result = achievementRepository.findNotAchievedAchievement(type, userId)

            //then
            assertThat(result).isEqualTo(activeAchievement)
        }
    }

    @DisplayName("getAllActivatedOfType 호출 시")
    @Nested
    inner class TestGetAllActivatedOfType {
        @DisplayName("조회한 타입의 업적만 조회된다")
        @Test
        fun `조회한 타입의 업적만 조회된다`() {
            //given
            val achievementType = QUEST_REGISTRATION
            val sameTypeAchievement = Achievement("", "", achievementType, 1)
            val otherTypeAchievement = Achievement("", "", QUEST_COMPLETION, 2)
            achievementRepository.save(sameTypeAchievement)
            achievementRepository.save(otherTypeAchievement)

            //when
            val result = achievementRepository.getAllActivatedOfType(achievementType)

            //then
            assertThat(result).doesNotContain(otherTypeAchievement)
        }

        @DisplayName("활성 상태의 업적만 조회된다")
        @Test
        fun `활성 상태의 업적만 조회된다`() {
            //given
            val achievementType = QUEST_REGISTRATION
            val inactiveAchievement = Achievement("", "", achievementType, 1)
            inactiveAchievement.inactivateAchievement()
            val activeAchievement = Achievement("", "", QUEST_COMPLETION, 2)
            achievementRepository.save(inactiveAchievement)
            achievementRepository.save(activeAchievement)

            //when
            val result = achievementRepository.getAllActivatedOfType(achievementType)

            //then
            assertThat(result).doesNotContain(inactiveAchievement)
        }
    }

    @DisplayName("getNotAchievedAchievements 호출 시")
    @Nested
    inner class TestGetNotAchievedAchievements {
        @DisplayName("해당 유저가 달성하지 않은 업적만 조회된다")
        @Test
        fun `해당 유저가 달성하지 않은 업적만 조회된다`() {
            //given
            val userId = 1L
            val achievementType = QUEST_REGISTRATION
            val achievedAchievement = Achievement("", "", achievementType, 1)
            achievementRepository.save(achievedAchievement)
            achieveLogRepository.save(AchievementAchieveLog(achievedAchievement, userId))
            val notAchievedAchievement = Achievement("", "", QUEST_COMPLETION, 2)
            achievementRepository.save(notAchievedAchievement)

            //when
            val result = achievementRepository.getNotAchievedAchievements(userId, Pageable.unpaged()).content

            //then
            assertThat(result).allMatch { it.id == notAchievedAchievement.id }
        }

        @DisplayName("활성 상태의 업적만 조회된다")
        @Test
        fun `활성 상태의 업적만 조회된다`() {
            //given
            val userId = 1L
            val achievementType = QUEST_REGISTRATION
            val activatedAchievement = Achievement("", "", achievementType, 1)
            achievementRepository.save(activatedAchievement)
            val inactivatedAchievement = Achievement("", "", QUEST_COMPLETION, 2)
            inactivatedAchievement.inactivateAchievement()
            achievementRepository.save(inactivatedAchievement)

            //when
            val result = achievementRepository.getNotAchievedAchievements(userId, Pageable.unpaged()).content

            //then
            assertThat(result).allMatch { it.id == activatedAchievement.id }
        }
    }
}