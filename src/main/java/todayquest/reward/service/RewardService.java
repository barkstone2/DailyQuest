package todayquest.reward.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import todayquest.common.MessageUtil;
import todayquest.reward.dto.RewardRequestDto;
import todayquest.reward.dto.RewardResponseDto;
import todayquest.reward.entity.Reward;
import todayquest.reward.repository.RewardRepository;
import todayquest.user.entity.UserInfo;
import todayquest.user.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Transactional
@RequiredArgsConstructor
@Service
public class RewardService {
    private final RewardRepository rewardRepository;
    private final UserRepository userRepository;

    public List<RewardResponseDto> getRewardList(Long userId) {
        return rewardRepository.findAllByUserId(userId).stream()
                .map(RewardResponseDto::createDto)
                .collect(Collectors.toList());
    }

    public RewardResponseDto getReward(Long id, Long userId) {
        Reward reward = getRewardWithOwnerCheck(id, userId);
        return RewardResponseDto.createDto(reward);
    }

    public Long saveReward(RewardRequestDto dto, Long userId) {
        UserInfo findUser = userRepository.getById(userId);
        Reward saveReward = rewardRepository.save(dto.mapToEntity(findUser));
        return saveReward.getId();
    }

    public void updateReward(RewardRequestDto dto, Long rewardId, Long userId) {
        Reward reward = getRewardWithOwnerCheck(rewardId, userId);
        reward.updateReward(dto);
    }

    public void deleteReward(Long rewardId, Long userId) {
        getRewardWithOwnerCheck(rewardId, userId);
        rewardRepository.deleteById(rewardId);
    }

    public List<RewardResponseDto> getRewardListByIds(List<Long> ids, Long userId) {
        return rewardRepository.findAllByIdAndUserId(ids, userId).stream().map(RewardResponseDto::createDto).collect(Collectors.toList());
    }

    /**
     * 요청한 RewardId가 올바른지, 유저 정보가 올바른지 확인
     */
    private Reward getRewardWithOwnerCheck(Long id, Long userId) {
        Optional<Reward> findReward = rewardRepository.findById(id);
        Reward reward = findReward
                .orElseThrow(() -> new IllegalArgumentException(MessageUtil.getMessage("exception.entity.notfound", MessageUtil.getMessage("reward"))));

        if(!reward.getUser().getId().equals(userId)) {
            throw new AccessDeniedException(MessageUtil.getMessage("exception.access.denied", MessageUtil.getMessage("reward")));
        }
        return reward;
    }

}
