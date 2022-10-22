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

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Transactional
@Service
public class ItemService {

    private final ItemRepository itemRepository;
    private final ItemLogRepository itemLogRepository;

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



}
