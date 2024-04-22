package dailyquest.quest.repository

import dailyquest.config.JpaAuditingConfiguration
import dailyquest.quest.dto.QuestSearchCondition
import dailyquest.quest.entity.Quest
import dailyquest.quest.entity.QuestState
import dailyquest.quest.entity.QuestType
import dailyquest.user.entity.ProviderType
import dailyquest.user.entity.User
import dailyquest.user.repository.UserRepository
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.Pageable
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@DisplayName("퀘스트 리포지토리 유닛 테스트")
@DataJpaTest
@Import(JpaAuditingConfiguration::class)
class QuestRepositoryUnitTest {

    @Autowired
    lateinit var questRepository: QuestRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @PersistenceContext
    lateinit var entityManager: EntityManager

    var user: User = User("", "", ProviderType.GOOGLE)
    var anotherUser: User = User("", "", ProviderType.GOOGLE)
    lateinit var quest: Quest

    @BeforeEach
    fun init() {
        user = if(user.id == 0L) userRepository.save(User("", "user1", ProviderType.GOOGLE)) else user
        anotherUser = if(anotherUser.id == 0L) userRepository.save(User("", "user2", ProviderType.GOOGLE)) else anotherUser
        anotherUser.updateCoreTime(0)
        userRepository.saveAndFlush(anotherUser)
    }

    @DisplayName("getSearchedQuests 호출 시")
    @Nested
    inner class TestForGetSearchedQuests {

        @DisplayName("인자로 주어진 ID의 퀘스트만 조회된다")
        @Test
        fun getQuestsByIds() {
            //given
            val savedQuest1 = questRepository.save(Quest("", "", user.id, 1L, QuestState.PROCEED, QuestType.MAIN))
            val savedQuest2 = questRepository.save(Quest("", "", user.id, 1L, QuestState.FAIL, QuestType.MAIN))
            val savedQuest3 = questRepository.save(Quest("", "", user.id, 1L, QuestState.DISCARD, QuestType.MAIN))
            val savedQuest4 = questRepository.save(Quest("", "", user.id, 1L, QuestState.DELETE, QuestType.MAIN))
            val savedQuest5 = questRepository.save(Quest("", "", user.id, 1L, QuestState.COMPLETE, QuestType.MAIN))

            val listOfQuestIds = listOf(savedQuest1.id, savedQuest2.id, savedQuest3.id)

            //when
            val questsList = questRepository.getSearchedQuests(listOfQuestIds, Pageable.ofSize(100))

            //then
            assertThat(questsList).containsExactlyInAnyOrder(savedQuest1, savedQuest2, savedQuest3)
            assertThat(questsList).doesNotContain(savedQuest4, savedQuest5)
            assertThat(questsList).hasSize(3)
        }

        @DisplayName("ID 역순으로 정렬되어 조회된다")
        @Test
        fun getQuestsOrderByIdDesc() {
            //given
            val savedQuest1 = questRepository.save(Quest("", "", user.id, 1L, QuestState.PROCEED, QuestType.MAIN))
            val savedQuest2 = questRepository.save(Quest("", "", user.id, 1L, QuestState.FAIL, QuestType.MAIN))
            val savedQuest3 = questRepository.save(Quest("", "", user.id, 1L, QuestState.DISCARD, QuestType.MAIN))
            val savedQuest4 = questRepository.save(Quest("", "", user.id, 1L, QuestState.DELETE, QuestType.MAIN))
            val savedQuest5 = questRepository.save(Quest("", "", user.id, 1L, QuestState.COMPLETE, QuestType.MAIN))

            val listOfQuestIds = listOf(savedQuest5.id, savedQuest1.id, savedQuest4.id, savedQuest2.id, savedQuest3.id)

            //when
            val questsList = questRepository.getSearchedQuests(listOfQuestIds, Pageable.ofSize(100))

            //then
            assertThat(questsList).containsExactly(savedQuest5, savedQuest4, savedQuest3, savedQuest2, savedQuest1)
        }

    }

    @DisplayName("getCurrentQuests 호출 시")
    @Nested
    inner class TestForGetCurrentQuests {
        private val baseDate = LocalDateTime.of(2022, 1, 1, 12, 0)
        private val prevDate = baseDate.minusDays(1)
        private val nextDate = baseDate.plusDays(1)

        @EnumSource(QuestState::class)
        @DisplayName("조회한 상태의 퀘스트만 조회된다")
        @ParameterizedTest(name = "{0} 값이 인자로 주어지면 {0} 상태의 퀘스트만 조회된다")
        fun `퀘스트 타입별 조회`(state: QuestState) {
            //given
            questRepository.save(Quest("", "", user.id, 1L, QuestState.PROCEED, QuestType.MAIN))
            questRepository.save(Quest("", "", user.id, 1L, QuestState.FAIL, QuestType.MAIN))
            questRepository.save(Quest("", "", user.id, 1L, QuestState.DISCARD, QuestType.MAIN))
            questRepository.save(Quest("", "", user.id, 1L, QuestState.DELETE, QuestType.MAIN))
            questRepository.save(Quest("", "", user.id, 1L, QuestState.COMPLETE, QuestType.MAIN))

            //when
            val questsList = questRepository.getCurrentQuests(user.id, state, prevDate, nextDate)

            //then
            assertThat(questsList).allMatch { quest -> quest.state == state }
        }


        @DisplayName("조회한 유저의 퀘스트만 조회된다")
        @Test
        fun `퀘스트 유저별 조회`() {
            //given
            questRepository.save(Quest("", "", user.id, 1L, QuestState.PROCEED, QuestType.MAIN))
            questRepository.save(Quest("", "", user.id, 1L, QuestState.PROCEED, QuestType.MAIN))
            questRepository.save(Quest("", "", anotherUser.id, 1L, QuestState.PROCEED, QuestType.MAIN))

            //when
            val questsList = questRepository.getCurrentQuests(user.id, QuestState.PROCEED, prevDate, nextDate)

            //then
            assertThat(questsList).allMatch { quest -> quest.userId == user.id }
        }

        @DisplayName("조회 날짜 범위 사이에 등록된 퀘스트만 조회된다")
        @Test
        fun `조회 기간 테스트`() {
            //given
            this.insertQuestWithCreatedTime(baseDate)
            this.insertQuestWithCreatedTime(prevDate)
            this.insertQuestWithCreatedTime(nextDate)

            this.insertQuestWithCreatedTime(prevDate.minusSeconds(1))
            this.insertQuestWithCreatedTime(nextDate.plusSeconds(1))

            //when
            val questsList = questRepository.getCurrentQuests(user.id, QuestState.PROCEED, prevDate, nextDate)

            //then
            assertThat(questsList)
                .allMatch { it.createdDate >= prevDate && it.createdDate <= nextDate }
                .anyMatch { it.createdDate == prevDate }
                .anyMatch { it.createdDate == nextDate }
        }

        private fun insertQuestWithCreatedTime(createdTime: LocalDateTime) {
            val insertQuery = entityManager
                .createNativeQuery("insert into quest (quest_id, created_date, last_modified_date, description, user_quest_seq, state, title, type, user_id) values (default, ?, ?, '', 1, 'PROCEED', '', 'MAIN', ?)")
                .setParameter(3, user.id)
            insertQuery.setParameter(1, createdTime).setParameter(2, createdTime).executeUpdate()
        }
    }

    @DisplayName("getNextSeqByUserId 호출 시")
    @Nested
    inner class GetNextSeqByUserIdTest {

        @Test
        @DisplayName("새로운 퀘스트가 등록되지 않으면 항상 같은 값을 반환한다")
        fun `새로운 퀘스트가 등록되지 않으면 항상 같은 값을 반환한다`() {

            //given
            val userId = user.id
            val initSeq = questRepository.getNextSeqByUserId(userId)

            //when
            val nextSeq = questRepository.getNextSeqByUserId(userId)

            //then
            assertThat(initSeq).isEqualTo(nextSeq)
        }

        @Test
        @DisplayName("현재 퀘스트의 MAX_SEQ+1 값을 반환한다")
        fun `현재 퀘스트의 MAX_SEQ+1 값을 반환한다`() {

            //given
            val userId = user.id
            val maxSeq = 5351L
            val saveQuest = Quest("", "", user.id, maxSeq, QuestState.PROCEED, QuestType.MAIN)
            questRepository.save(saveQuest)

            //when
            val nextSeq = questRepository.getNextSeqByUserId(userId)

            //then
            assertThat(nextSeq).isEqualTo(maxSeq+1)
        }

        @Test
        @DisplayName("현재 등록된 퀘스트가 없으면 1을 반환한다")
        fun `현재 등록된 퀘스트가 없으면 1을 반환한다`() {

            //given
            val userId = user.id
            questRepository.deleteAll()
            questRepository.flush()

            //when
            val nextSeq = questRepository.getNextSeqByUserId(userId)

            //then
            assertThat(nextSeq).isEqualTo(1)
        }


        @Test
        @DisplayName("유저 별로 SEQ 채번이 나눠진다")
        fun `유저 별로 SEQ 채번이 나눠진다`() {

            //given
            val userId1 = user.id
            val userId2 = anotherUser.id
            val user1Seq = 533L
            questRepository.deleteAll()
            questRepository.flush()

            val saveQuest = Quest("", "", user.id, user1Seq, QuestState.PROCEED, QuestType.MAIN)
            questRepository.save(saveQuest)

            //when
            val user1NextSeq = questRepository.getNextSeqByUserId(userId1)
            val user2NextSeq = questRepository.getNextSeqByUserId(userId2)

            //then
            assertThat(user1NextSeq).isEqualTo(user1Seq+1)
            assertThat(user2NextSeq).isEqualTo(1)
        }
    }

    @DisplayName("findQuestsByCondition 메서드 호출 시")
    @Nested
    inner class TestForFindQuestsByCondition {

        @DisplayName("상태 검색 조건이 null이면 모든 상태의 퀘스트가 조회된다")
        @Test
        fun `상태 검색 조건이 null이면 모든 상태의 퀘스트가 조회된다`() {
            //given
            val savedQuest = mutableListOf<Quest>()

            questRepository.save(Quest("", "", user.id, 1L, QuestState.PROCEED, QuestType.MAIN)).let { savedQuest.add(it) }
            questRepository.save(Quest("", "", user.id, 1L, QuestState.FAIL, QuestType.MAIN)).let { savedQuest.add(it) }
            questRepository.save(Quest("", "", user.id, 1L, QuestState.DISCARD, QuestType.MAIN)).let { savedQuest.add(it) }
            questRepository.save(Quest("", "", user.id, 1L, QuestState.DELETE, QuestType.MAIN)).let { savedQuest.add(it) }
            questRepository.save(Quest("", "", user.id, 1L, QuestState.COMPLETE, QuestType.MAIN)).let { savedQuest.add(it) }

            val searchCondition = QuestSearchCondition(null, null, null, null, null, null)

            //when
            val findQuests =
                questRepository.findQuestsByCondition(user.id, searchCondition, Pageable.ofSize(1000))

            //then
            assertThat(findQuests).containsExactlyInAnyOrderElementsOf(savedQuest)
            assertThat(findQuests).hasSize(savedQuest.size)
        }

        @EnumSource(QuestState::class)
        @DisplayName("상태 검색 조건이 null이 아니면 해당 상태의 퀘스트만 조회된다")
        @ParameterizedTest(name = "{0} 값이 인자로 주어지면 {0} 상태의 퀘스트만 조회된다")
        fun `상태 검색 조건이 null이 아니면 해당 상태의 퀘스트만 조회된다`(searchState: QuestState) {
            //given
            questRepository.save(Quest("", "", user.id, 1L, QuestState.PROCEED, QuestType.MAIN))
            questRepository.save(Quest("", "", user.id, 1L, QuestState.FAIL, QuestType.MAIN))
            questRepository.save(Quest("", "", user.id, 1L, QuestState.DISCARD, QuestType.MAIN))
            questRepository.save(Quest("", "", user.id, 1L, QuestState.DELETE, QuestType.MAIN))
            questRepository.save(Quest("", "", user.id, 1L, QuestState.COMPLETE, QuestType.MAIN))

            val searchCondition = QuestSearchCondition(null, searchState, null, null, null, null)

            //when
            val findQuests =
                questRepository.findQuestsByCondition(user.id, searchCondition, Pageable.ofSize(1000))

            //then
            assertThat(findQuests).allMatch { quest -> quest.state == searchState }
        }

        @DisplayName("시작일과 종료일 검색 조건이 모두 null이면 모든 등록일의 퀘스트가 조회된다")
        @Test
        fun `시작일과 종료일 검색 조건이 모두 null이면 모든 등록일의 퀘스트가 조회된다`() {
            //given
            val query = entityManager
                .createNativeQuery("insert into quest (quest_id, created_date, description, user_quest_seq, state, title, type, user_id) values (default, ?, '', 1, 'PROCEED', '', 'MAIN', ?)")
                .setParameter(2, user.id)

            val time = LocalTime.of(12, 0, 0)
            val date1 = LocalDateTime.of(LocalDate.of(2020, 12, 1), time)
            val date2 = LocalDateTime.of(LocalDate.of(2021, 12, 1), time)
            val date3 = LocalDateTime.of(LocalDate.of(2022, 11, 1), time)
            val date4 = LocalDateTime.of(LocalDate.of(2022, 11, 2), time)

            query.setParameter(1, date1).executeUpdate()
            query.setParameter(1, date2).executeUpdate()
            query.setParameter(1, date3).executeUpdate()
            query.setParameter(1, date4).executeUpdate()

            val searchCondition = QuestSearchCondition(null, null, null, null, null, null)

            //when
            val result =
                questRepository.findQuestsByCondition(user.id, searchCondition, Pageable.ofSize(100))

            //then
            assertThat(result.content).hasSize(4)
        }


        @DisplayName("시작일이 null이 아니고 종료일이 null이면 퀘스트 등록일이 시작일 오전 6시와 같거나 이후인 퀘스트만 조회된다")
        @Test
        fun `시작일이 null이 아니고 종료일이 null이면 퀘스트 등록일이 시작일 오전 6시와 같거나 이후인 퀘스트만 조회된다`() {
            //given
            val query = entityManager
                .createNativeQuery("insert into quest (quest_id, created_date, description, user_quest_seq, state, title, type, user_id) values (default, ?, '', 1, 'PROCEED', '', 'MAIN', ?)")
                .setParameter(2, user.id)

            val startDate = LocalDate.of(2022, 12, 1)
            val startDateTime = LocalDateTime.of(startDate, LocalTime.of(6, 0))

            val datetime1 = LocalDateTime.of(startDate, LocalTime.of(5, 59))
            val datetime2 = LocalDateTime.of(startDate, LocalTime.of(6, 0))
            val datetime3 = LocalDateTime.of(startDate, LocalTime.of(6, 1))

            query.setParameter(1, datetime1).executeUpdate()
            query.setParameter(1, datetime2).executeUpdate()
            query.setParameter(1, datetime3).executeUpdate()

            val searchCondition = QuestSearchCondition(null, null, null, null, startDate, null)

            //when
            val result =
                questRepository.findQuestsByCondition(user.id, searchCondition, Pageable.ofSize(100))

            //then
            assertThat(result.content).noneMatch { it.createdDate.isBefore(startDateTime) }
        }

        @DisplayName("시작일이 null이고 종료일이 null이 아니면 등록일이 종료일 다음날 오전 6시와 같거나 이전인 퀘스트만 조회된다")
        @Test
        fun `시작일이 null이고 종료일이 null이 아니면 등록일이 종료일 다음날 오전 6시와 같거나 이전인 퀘스트만 조회된다`() {
            //given
            val query = entityManager
                .createNativeQuery("insert into quest (quest_id, created_date, description, user_quest_seq, state, title, type, user_id) values (default, ?, '', 1, 'PROCEED', '', 'MAIN', ?)")
                .setParameter(2, user.id)

            val endDate = LocalDate.of(2022, 12, 1)
            val endDateTime = LocalDateTime.of(endDate.plusDays(1), LocalTime.of(6, 0))

            val datetime1 = LocalDateTime.of(endDate.plusDays(1), LocalTime.of(5, 59))
            val datetime2 = LocalDateTime.of(endDate.plusDays(1), LocalTime.of(6, 0))
            val datetime3 = LocalDateTime.of(endDate.plusDays(1), LocalTime.of(6, 1))

            query.setParameter(1, datetime1).executeUpdate()
            query.setParameter(1, datetime2).executeUpdate()
            query.setParameter(1, datetime3).executeUpdate()

            val searchCondition = QuestSearchCondition(null, null, null, null, null, endDate)

            //when
            val result =
                questRepository.findQuestsByCondition(user.id, searchCondition, Pageable.ofSize(100))

            //then
            assertThat(result.content).noneMatch { it.createdDate.isAfter(endDateTime) }
        }

        @DisplayName("시작일과 종료일이 모두 null이 아니면 시작일 오전 6시 <= 퀘스트 등록일 <= 종료일 다음날 오전 6시인 퀘스트만 조회된다")
        @Test
        fun testWhenBothNotNull() {
            //given
            val query = entityManager
                .createNativeQuery("insert into quest (quest_id, created_date, description, user_quest_seq, state, title, type, user_id) values (default, ?, '', 1, 'PROCEED', '', 'MAIN', ?)")
                .setParameter(2, user.id)

            val startDate = LocalDate.of(2022, 12, 1)
            val startDateTime = LocalDateTime.of(startDate, LocalTime.of(6, 0))

            val endDate = LocalDate.of(2022, 12, 10)
            val endDateTime = LocalDateTime.of(endDate.plusDays(1), LocalTime.of(6, 0))

            val datetime1 = LocalDateTime.of(startDate, LocalTime.of(5, 59))
            val datetime2 = LocalDateTime.of(startDate, LocalTime.of(6, 0))
            val datetime3 = LocalDateTime.of(endDate.plusDays(1), LocalTime.of(6, 0))
            val datetime4 = LocalDateTime.of(endDate.plusDays(1), LocalTime.of(6, 1))

            query.setParameter(1, datetime1).executeUpdate()
            query.setParameter(1, datetime2).executeUpdate()
            query.setParameter(1, datetime3).executeUpdate()
            query.setParameter(1, datetime4).executeUpdate()

            val searchCondition = QuestSearchCondition(null, null, null, null, startDate, endDate)

            //when
            val result =
                questRepository.findQuestsByCondition(user.id, searchCondition, Pageable.ofSize(100))

            //then
            assertThat(result.content).noneMatch {
                it.createdDate.isBefore(startDateTime) || it.createdDate.isAfter(endDateTime)
            }
        }

        @DisplayName("ID 역순으로 정렬된 상태의 퀘스트가 조회된다")
        @Test
        fun `ID 역순으로 정렬된 상태의 퀘스트가 조회된다`() {
            //given
            val savedQuest = mutableListOf<Quest>()

            questRepository.save(Quest("", "", user.id, 1L, QuestState.PROCEED, QuestType.MAIN)).let { savedQuest.add(it) }
            questRepository.save(Quest("", "", user.id, 1L, QuestState.FAIL, QuestType.MAIN)).let { savedQuest.add(it) }
            questRepository.save(Quest("", "", user.id, 1L, QuestState.DISCARD, QuestType.MAIN)).let { savedQuest.add(it) }
            questRepository.save(Quest("", "", user.id, 1L, QuestState.DELETE, QuestType.MAIN)).let { savedQuest.add(it) }
            questRepository.save(Quest("", "", user.id, 1L, QuestState.COMPLETE, QuestType.MAIN)).let { savedQuest.add(it) }

            val searchCondition = QuestSearchCondition(null, null, null, null, null, null)

            //when
            val findQuests =
                questRepository.findQuestsByCondition(user.id, searchCondition, Pageable.ofSize(1000))

            //then
            assertThat(findQuests).containsExactlyElementsOf(savedQuest.reversed())
            assertThat(findQuests).hasSize(savedQuest.size)
        }
    }
}