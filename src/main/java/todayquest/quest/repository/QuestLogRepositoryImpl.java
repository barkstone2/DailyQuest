package todayquest.quest.repository;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.NullExpression;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import todayquest.common.TimeUtilKt;
import todayquest.quest.dto.QuestLogSearchCondition;
import todayquest.quest.entity.QuestState;
import todayquest.quest.entity.QuestType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static todayquest.quest.entity.QQuestLog.questLog;

@RequiredArgsConstructor
public class QuestLogRepositoryImpl implements QuestLogRepositoryCustom {

    private final EntityManager em;
    private JPAQueryFactory query;

    @PostConstruct
    private void init() {
        query = new JPAQueryFactory(em);
    }

    @Override
    public Map<LocalDate, Map<String, Long>> getQuestStatisticByState(Long userId, QuestLogSearchCondition condition) {

        Map<LocalDate, Map<String, Long>> result = condition.createResponseCollectionByType();

        final Function<LocalDateTime, LocalDate> dateKeyTransformFunction;

        switch (condition.getSearchType()) {
            case WEEKLY -> dateKeyTransformFunction = TimeUtilKt::firstDayOfWeek;
            case MONTHLY -> dateKeyTransformFunction = (loggedDate) -> LocalDate.from(loggedDate.with(TemporalAdjusters.firstDayOfMonth()));
            default -> dateKeyTransformFunction = LocalDate::from;
        }

        // MySql의 group by 시 자동 file sort 로 인해 null 정렬 추가
        query
                .select(questLog.state, Expressions.asDate(questLog.loggedDate), questLog.state.count())
                .from(questLog)
                .where(questLog.userId.eq(userId), questLog.loggedDate.between(condition.getStartDate(), condition.getEndDate()))
                .groupBy(questLog.state, Expressions.asDate(questLog.loggedDate))
                .orderBy(new OrderSpecifier<>(Order.ASC, (Expression) NullExpression.DEFAULT, OrderSpecifier.NullHandling.Default))
                .fetch()
                .forEach(tuple -> {
                    LocalDateTime loggedDate = tuple.get(questLog.loggedDate);
                    LocalDate dateKey = dateKeyTransformFunction.apply(loggedDate);

                    QuestState state = tuple.get(questLog.state);

                    Map<String, Long> typeMapOfDay = result.get(dateKey);
                    typeMapOfDay.compute(state.name(), (k, v) -> v + tuple.get(questLog.state.count()));
                });

        return result;
    }

    @Override
    public Map<String, Long> getQuestStatisticByType(Long userId, QuestLogSearchCondition condition) {

        Map<String, Long> resultMap = Arrays.stream(QuestType.values())
                .collect(Collectors.toMap(QuestType::name, type -> 0L));

        // MySql의 group by 시 자동 file sort 로 인해 null 정렬 추가
        query
                .select(questLog.type, questLog.type.count())
                .from(questLog)
                .where(questLog.userId.eq(userId), questLog.loggedDate.between(condition.getStartDate(), condition.getEndDate()))
                .groupBy(questLog.type)
                .orderBy(new OrderSpecifier<>(Order.ASC, (Expression) NullExpression.DEFAULT, OrderSpecifier.NullHandling.Default))
                .fetch()
                .forEach(tuple -> {
                    QuestType type = tuple.get(questLog.type);
                    if (type == null) {
                        return;
                    }
                    resultMap.put(type.name(), tuple.get(questLog.type.count()));
                });

        return resultMap;
    }



}
