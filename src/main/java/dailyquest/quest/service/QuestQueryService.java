package dailyquest.quest.service;

import dailyquest.common.MessageUtil;
import dailyquest.common.RestPage;
import dailyquest.quest.dto.QuestResponse;
import dailyquest.quest.dto.QuestSearchCondition;
import dailyquest.quest.entity.Quest;
import dailyquest.quest.entity.QuestState;
import dailyquest.quest.repository.QuestRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class QuestQueryService {

    private final QuestRepository questRepository;

    public List<QuestResponse> getCurrentQuests(Long userId, QuestState state) {

        return questRepository
                .getCurrentQuests(userId, state)
                .stream()
                .map(QuestResponse::createDto).toList();
    }

    public RestPage<QuestResponse> getQuestsByCondition(Long userId, QuestSearchCondition condition, Pageable pageable) {

        PageRequest pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());

        return new RestPage<>(
                questRepository
                        .findQuestsByCondition(userId, condition.state(), condition.startDate(), condition.endDate(), pageRequest)
                        .map(QuestResponse::createDto)
        );
    }

    public QuestResponse getQuestInfo(Long questId, Long userId) {
        Quest quest = findByIdOrThrow(questId);
        quest.checkOwnershipOrThrow(userId);

        return QuestResponse.createDto(quest);
    }

    Quest findByIdOrThrow(Long questId) {
        Optional<Quest> findQuest = questRepository.findById(questId);
        return findQuest.orElseThrow(() -> new EntityNotFoundException(
                MessageUtil.getMessage("exception.entity.notfound", MessageUtil.getMessage("quest"))));
    }

    public RestPage<QuestResponse> getSearchedQuests(List<Long> searchedIds, Long userId, Pageable pageable) {
        return new RestPage<>(
                questRepository.getSearchedQuests(userId, searchedIds, pageable)
                        .map(QuestResponse::createDto)
        );
    }
}
