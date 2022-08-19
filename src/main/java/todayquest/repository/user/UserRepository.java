package todayquest.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import todayquest.user.entity.UserInfo;

@Repository
public interface UserRepository extends JpaRepository<UserInfo, Long> {

    UserInfo findByOauth2Id(String oauth2Id);
    boolean existsByNickname(String nickname);
}
