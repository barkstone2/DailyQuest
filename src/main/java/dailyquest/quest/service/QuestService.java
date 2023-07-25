package dailyquest.quest.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import dailyquest.common.MessageUtil;
import dailyquest.common.RestPage;
import dailyquest.quest.dto.DetailInteractRequest;
import dailyquest.quest.dto.DetailResponse;
import dailyquest.quest.dto.QuestRequest;
import dailyquest.quest.dto.QuestResponse;
import dailyquest.quest.entity.Quest;
import dailyquest.quest.entity.QuestState;
import dailyquest.quest.repository.QuestRepository;
import dailyquest.user.entity.UserInfo;
import dailyquest.user.repository.UserRepository;
import dailyquest.user.service.UserService;

import java.util.Optional;

@Transactional
@RequiredArgsConstructor
@Service
public class QuestService {

    private final QuestRepository questRepository;
    private final UserRepository userRepository;

    private final UserService userService;
    private final QuestLogService questLogService;

    public RestPage<QuestResponse> getQuestList(Long userId, QuestState state, Pageable pageable) {

        PageRequest pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());

        return new RestPage<>(
                questRepository
                        .getQuestsList(userId, state, pageRequest)
                        .map(QuestResponse::createDto)
        );
    }

    public QuestResponse getQuestInfo(Long questId, Long userId) {
        Quest quest = findByIdOrThrow(questId);
        quest.checkOwnershipOrThrow(userId);

        return QuestResponse.createDto(quest);
    }

    public QuestResponse saveQuest(QuestRequest dto, Long userId) {
        UserInfo findUser = userRepository.getReferenceById(userId);
        dto.checkRangeOfDeadLine(findUser.getResetTime());

        if (findUser.isNowCoreTime()) {
            dto.toMainQuest();
        }

        Long nextSeq = questRepository.getNextSeqByUserId(userId);

        Quest quest = dto.mapToEntity(nextSeq, findUser);
        questRepository.save(quest);

        quest.updateDetailQuests(dto.getDetails());

        questLogService.saveQuestLog(quest);

        return QuestResponse.createDto(quest);
    }

    public QuestResponse updateQuest(QuestRequest dto, Long questId, Long userId) {
        Quest quest = findByIdOrThrow(questId);
        dto.checkRangeOfDeadLine(quest.getUser().getResetTime());
        quest.checkOwnershipOrThrow(userId);
        quest.checkStateIsProceedOrThrow();

        if(quest.isMainQuest()) dto.toMainQuest();

        quest.updateQuestEntity(dto);

        return QuestResponse.createDto(quest);
    }

    public void deleteQuest(Long questId, Long userId) {
        Quest quest = findByIdOrThrow(questId);
        quest.checkOwnershipOrThrow(userId);
        quest.deleteQuest();
    }

    public void completeQuest(Long questId, Long userId) {
        Quest quest = findByIdOrThrow(questId);
        quest.checkOwnershipOrThrow(userId);
        quest.completeQuest();

        userService.earnExpAndGold(quest.getType(), quest.getUser());
        questLogService.saveQuestLog(quest);
    }

    public void discardQuest(Long questId, Long userId) {
        Quest quest = findByIdOrThrow(questId);
        quest.checkOwnershipOrThrow(userId);

        quest.discardQuest();

        questLogService.saveQuestLog(quest);
    }

    public DetailResponse interactWithDetailQuest(Long userId, Long questId, Long detailQuestId, DetailInteractRequest request) {
        Quest quest = findByIdOrThrow(questId);
        quest.checkOwnershipOrThrow(userId);

        return quest.interactWithDetailQuest(detailQuestId, request);
    }

    private Quest findByIdOrThrow(Long questId) {
        Optional<Quest> findQuest = questRepository.findById(questId);
        return findQuest.orElseThrow(() -> new EntityNotFoundException(
                MessageUtil.getMessage("exception.entity.notfound", MessageUtil.getMessage("quest"))));
    }
}
