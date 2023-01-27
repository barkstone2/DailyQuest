package todayquest.quest.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

import static javax.persistence.EnumType.STRING;
import static javax.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Getter
@NoArgsConstructor(access = PROTECTED)
@Entity(name = "detail_quest")
public class DetailQuest {

    @Id @GeneratedValue(strategy = IDENTITY)
    @Column(name = "detail_quest_id")
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false)
    private Short targetCount;

    @Column(nullable = false)
    private Short count;

    @Enumerated(STRING)
    @Column(nullable = false)
    private DetailQuestType type;

    @Enumerated(STRING)
    @Column(nullable = false)
    private DetailQuestState state;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quest_id")
    private Quest quest;

    @Builder
    public DetailQuest(Long id, String title, Short count, DetailQuestType type, DetailQuestState state) {
        this.id = id;
        this.title = title;
        this.count = count;
        this.type = type;
        this.state = state;
    }

    public void resetCount() {
        this.count = 0;
    }

    public void addCount() {
        this.count++;
        if(count.equals(targetCount)) changeState(DetailQuestState.COMPLETE);
    }
}
