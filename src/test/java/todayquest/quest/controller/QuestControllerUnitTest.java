package todayquest.quest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import todayquest.annotation.WithCustomMockUser;
import todayquest.config.SecurityConfig;
import todayquest.quest.dto.QuestRequestDto;
import todayquest.quest.dto.QuestResponseDto;
import todayquest.quest.entity.Quest;
import todayquest.quest.entity.QuestDifficulty;
import todayquest.quest.entity.QuestState;
import todayquest.quest.service.QuestService;
import todayquest.reward.dto.RewardResponseDto;
import todayquest.reward.service.RewardService;
import todayquest.user.entity.UserInfo;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
@WithCustomMockUser
@WebMvcTest(controllers = QuestController.class,
excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = SecurityConfig.class
    )
)
class QuestControllerUnitTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    QuestService questService;

    @MockBean
    RewardService rewardService;

    static final String URI_PREFIX = "/quests";
    List<QuestResponseDto> questList = new ArrayList<>();
    ObjectMapper mapper;

    @BeforeEach
    void init() {
        QuestResponseDto quest1 = QuestResponseDto.builder()
                .questId(1L).state(QuestState.PROCEED).title("title1")
                .description("desc1").isRepeat(true)
                .deadLineDate(LocalDate.now()).deadLineTime(LocalTime.now())
                .difficulty(QuestDifficulty.easy)
                .build();

        QuestResponseDto quest2 = QuestResponseDto.builder()
                .questId(2L).state(QuestState.PROCEED).title("title2")
                .description("desc2").isRepeat(true)
                .deadLineDate(LocalDate.now()).deadLineTime(LocalTime.now())
                .difficulty(QuestDifficulty.easy)
                .build();

        questList.add(quest1);
        questList.add(quest2);

        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
    }


    @DisplayName("퀘스트 목록 화면 요청")
    @Test
    public void testList() throws Exception {
        //given
        String uri = "";

        //when
        when(questService.getQuestList(any())).thenReturn(questList);

        //then
        mvc.perform(get(URI_PREFIX + uri))
                .andExpect(status().isOk())
                .andExpect(model().attribute("questList", questList))
                .andExpect(view().name("/quest/list"));
    }

    @DisplayName("퀘스트 등록 화면 요청")
    @Test
    public void testSaveForm() throws Exception {
        //given
        String uri = "/save";

        //then
        mvc.perform(get(URI_PREFIX + uri))
                .andExpect(status().isOk())
                .andExpect(model().attribute("rewardList", new ArrayList<>()))
                .andExpect(model().attributeExists("quest"))
                .andExpect(view().name("/quest/save"));

    }

    @DisplayName("퀘스트 조회 화면 요청")
    @Test
    public void testView() throws Exception {
        //given
        String uri = "/" + questList.get(0).getQuestId();

        //when
        when(questService.getQuestInfo(any())).thenReturn(questList.get(0));

        //then
        mvc.perform(get(URI_PREFIX + uri))
                .andExpect(status().isOk())
                .andExpect(model().attribute("rewardList", new ArrayList<>()))
                .andExpect(model().attribute("quest", questList.get(0)))
                .andExpect(view().name("/quest/view"));
    }

    @DisplayName("퀘스트 등록 요청")
    @Test
    public void testSave() throws Exception {
        //given
        String uri = "/save";
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("title", "save title");
        map.add("description", "save description");
        map.add("deadLineDate", "2022-11-11");
        map.add("deadLineTime", "11:11");
        map.add("repeat", "true");
        map.add("reward", "reward1");
        map.add("reward", "reward2");
        map.add("reward", "reward3");

        //when
        doNothing()
                .when(questService)
                .saveQuest(any(QuestRequestDto.class), any());

        //then
        mvc.perform(
                    post(URI_PREFIX + uri)
                        .with(csrf())
                        .params(map)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/quests"));

    }

    @DisplayName("퀘스트 삭제 성공")
    @Test
    public void testDeleteSuccess() throws Exception {
        //given
        String uri = "/" + questList.get(0).getQuestId();

        //when
        doReturn(true)
                .when(questService)
                .deleteQuest(any(), any());

        //then
        mvc.perform(delete(URI_PREFIX + uri).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/quests"))
                .andExpect(model().attributeDoesNotExist("message"));
    }

    @DisplayName("퀘스트 삭제 실패")
    @Test
    public void testDeleteFail() throws Exception {
        //given
        String uri = "/" + questList.get(0).getQuestId();

        //when
        doReturn(false)
                .when(questService)
                .deleteQuest(any(), any());
        //then
        mvc.perform(delete(URI_PREFIX + uri).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/quests*"))
                .andExpect(model().attributeExists("message"));
    }


    @DisplayName("퀘스트 수정 성공")
    @Test
    public void testUpdateSuccess() throws Exception {
        //given
        Long questId = questList.get(0).getQuestId();
        String uri = "/" + questId;

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("title", "save title");
        map.add("description", "save description");
        map.add("deadLineDate", "2022-11-11");
        map.add("deadLineTime", "11:11");
        map.add("repeat", "true");
        map.add("reward", "reward1");

        //when

        doReturn(true)
                .when(questService)
                .updateQuest(any(QuestRequestDto.class), any(), any());

        //then
        mvc.perform(
                put(URI_PREFIX + uri)
                        .with(csrf())
                        .params(map)
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/quests/" + questId))
                .andExpect(model().attributeDoesNotExist("message"));
    }

    @DisplayName("퀘스트 수정 실패")
    @Test
    public void testUpdateFail() throws Exception {
        //given
        Long questId = questList.get(0).getQuestId();
        String uri = "/" + questId;

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("title", "save title");
        map.add("description", "save description");
        map.add("deadLineDate", "2022-11-11");
        map.add("deadLineTime", "11:11");
        map.add("repeat", "true");
        map.add("reward", "reward1");

        //when

        doReturn(false)
                .when(questService)
                .updateQuest(any(QuestRequestDto.class), any(), any());

        //then
        mvc.perform(
                put(URI_PREFIX + uri)
                        .with(csrf())
                        .params(map)
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/quests*"))
                .andExpect(model().attributeExists("message"));
    }


}