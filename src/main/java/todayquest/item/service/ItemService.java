package todayquest.item.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import todayquest.common.MessageUtil;
import todayquest.item.dto.ItemResponseDto;
import todayquest.item.entity.Item;
import todayquest.item.entity.ItemLog;
import todayquest.item.entity.ItemLogType;
import todayquest.item.repository.ItemLogRepository;
import todayquest.item.repository.ItemRepository;
import todayquest.reward.entity.Reward;
import todayquest.user.entity.UserInfo;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Transactional
@Service
public class ItemService {

    private final ItemRepository itemRepository;
    private final ItemLogRepository itemLogRepository;
    private final ItemLogService itemLogService;

    public List<ItemResponseDto> getInventoryItems(Long userId) {
        return itemRepository.findByUserIdAndCountIsNot(userId, 0).stream().map(ItemResponseDto::createDto).collect(Collectors.toList());
    }

    public ItemResponseDto getItemInfo(Long itemId, Long userId) {
        return ItemResponseDto
                .createDto(itemRepository
                        .findByIdAndUserId(itemId, userId)
                        .orElseThrow(() -> new IllegalArgumentException(MessageUtil.getMessage("exception.entity.notfound", MessageUtil.getMessage("reward")))));
    }

    public ItemResponseDto useItem(Long itemId, Long userId, int count) {
        Item findItem = itemRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new IllegalArgumentException(MessageUtil.getMessage("exception.entity.notfound", MessageUtil.getMessage("reward"))));
        findItem.subtractCount(count);
        itemLogRepository.save(ItemLog.builder().rewardId(findItem.getReward().getId()).userId(userId).type(ItemLogType.USE).build());
        return ItemResponseDto.createDto(findItem);
    }

    public ItemResponseDto abandonItem(Long itemId, Long userId, int count) {
        Item findItem = itemRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new IllegalArgumentException(MessageUtil.getMessage("exception.entity.notfound", MessageUtil.getMessage("reward"))));
        findItem.subtractCount(count);
        itemLogRepository.save(ItemLog.builder().rewardId(findItem.getReward().getId()).userId(userId).type(ItemLogType.ABANDON).build());
        return ItemResponseDto.createDto(findItem);
    }

    public void saveAllWithDirtyChecking(List<Reward> rewards, UserInfo user) {

        // Reward의 ID 값만 가져온다.
        List<Long> rewardIds = rewards.stream()
                .map(r -> r.getId())
                .collect(Collectors.toList());

        // 보상 아이템 중 이미 인벤토리에 있는 아이템을 찾아 dirty checking으로 개수를 증가시킨다.
        List<Item> dirtyCheckItemList = itemRepository.findAllByRewardIdsAndUserId(rewardIds, user.getId());
        dirtyCheckItemList.stream().forEach(r -> r.addCount());

        // 더티 체킹으로 업데이트한 아이템의 Reward ID 값만 가져온다.
        List<Long> dirtyCheckIds = dirtyCheckItemList.stream()
                .map(i -> i.getReward().getId())
                .collect(Collectors.toList());

        // 보상 아이템 중 더티체킹으로 업데이트 하지 않은, 즉 새로 등록할 아이템을 걸러내 새로 등록한다.
        List<Item> saveList = rewards.stream()
                .filter(r -> !dirtyCheckIds.contains(r.getId()))
                .map(r -> Item.builder().reward(r).user(user).build())
                .collect(Collectors.toList());

        itemRepository.saveAll(saveList);

        // 아이템 획득 로그 저장
        itemLogService.saveItemEarnLogs(rewardIds, user.getId());
    }




}
