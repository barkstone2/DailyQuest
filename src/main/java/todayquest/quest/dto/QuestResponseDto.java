package todayquest.quest.dto;

import lombok.Builder;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import todayquest.quest.entity.Quest;
import todayquest.quest.entity.QuestDifficulty;
import todayquest.quest.entity.QuestState;
import todayquest.quest.entity.QuestType;
import todayquest.reward.dto.RewardResponseDto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Builder
@Data
public class QuestResponseDto {

    private Long questId;
    private String title;
    private String description;
    private boolean isRepeat;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate deadLineDate;
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime deadLineTime;
    private QuestState state;
    private QuestType type;
    private QuestDifficulty difficulty;
    private List<RewardResponseDto> rewards;

    public static QuestResponseDto createDto(Quest quest) {
        return QuestResponseDto.builder()
                .questId(quest.getId())
                .title(quest.getTitle())
                .description(quest.getDescription())
                .isRepeat(quest.isRepeat())
                .deadLineDate(quest.getDeadLineDate())
                .deadLineTime(quest.getDeadLineTime())
                .state(quest.getState())
                .type(quest.getType())
                .difficulty(quest.getDifficulty())
                .rewards(quest.getRewards().stream().map(r -> RewardResponseDto.createDto(r.getReward())).collect(Collectors.toList()))
                .build();
    }

}
