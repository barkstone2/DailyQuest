package todayquest.user.entity;

import lombok.*;
import todayquest.common.BaseTimeEntity;

import javax.persistence.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor @Builder
@Entity
public class UserInfo extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false)
    private String oauth2Id;

    @Column(nullable = false, length = 20)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProviderType providerType;

    private int level;
    private Long exp;
    private Long gold;

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }
    public void earnExpAndGold(int exp, int gold) {
        this.gold += gold;
        this.exp += exp;
    }

    public void levelUpCheck(Long targetExp) {
        if(level == 100) return;
        if (exp >= targetExp) {
            level++;
            exp -= targetExp;
        }
    }

}
