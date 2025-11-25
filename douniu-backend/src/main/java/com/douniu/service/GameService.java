package com.douniu.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.douniu.entity.GameDetail;
import com.douniu.entity.GameRecord;
import com.douniu.entity.Room;
import com.douniu.entity.RoomPlayer;
import com.douniu.enums.CardType;
import com.douniu.enums.GameStatus;
import com.douniu.enums.RoundStatus;
import com.douniu.mapper.GameDetailMapper;
import com.douniu.mapper.GameRecordMapper;
import com.douniu.mapper.RoomMapper;
import com.douniu.mapper.RoomPlayerMapper;
import com.douniu.utils.CardTypeCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameService {

    private final RoomMapper roomMapper;
    private final RoomPlayerMapper roomPlayerMapper;
    private final GameRecordMapper gameRecordMapper;
    private final GameDetailMapper gameDetailMapper;
    private final UserService userService;

    // 存储当前对局的牌面信息（key: gameRecordId, value: Map<userId, List<Card>>）
    private final Map<Long, Map<Long, List<CardTypeCalculator.Card>>> currentGameCards = new HashMap<>();
    // 存储当前对局的投注信息（key: gameRecordId, value: Map<userId, betAmount>）
    private final Map<Long, Map<Long, Integer>> currentGameBets = new HashMap<>();
    // 存储当前对局的开牌状态（key: gameRecordId, value: Set<userId>）
    private final Map<Long, java.util.Set<Long>> currentGameRevealed = new HashMap<>();
    // 存储当前对局的已准备玩家（key: roomId, value: Set<userId>）
    private final Map<Long, Set<Long>> currentRoundReadyPlayers = new HashMap<>();

    /**
     * 开始新一局游戏
     * @param readyPlayerIds 已准备的玩家ID集合（如果为null，则使用所有玩家）
     */
    @Transactional
    public GameRecord startNewRound(Long roomId, Long adminId, Set<Long> readyPlayerIds) {
        // 使用悲观锁锁定房间记录，避免并发问题
        Room room = roomMapper.selectById(roomId);
        if (room == null) {
            throw new RuntimeException("房间不存在");
        }

        if (!room.getAdminId().equals(adminId)) {
            throw new RuntimeException("无权限");
        }

        // 重新查询房间，确保获取最新的 currentRound
        room = roomMapper.selectById(roomId);
        
        if (room.getCurrentRound() >= room.getMaxRounds()) {
            throw new RuntimeException("已达到最大局数");
        }

        // 获取房间内所有玩家
        List<RoomPlayer> allPlayers = getRoomPlayers(roomId);
        
        // 如果指定了已准备的玩家，只使用已准备的玩家
        List<RoomPlayer> players;
        if (readyPlayerIds != null && !readyPlayerIds.isEmpty()) {
            players = allPlayers.stream()
                    .filter(p -> readyPlayerIds.contains(p.getUserId()))
                    .collect(java.util.stream.Collectors.toList());
        } else {
            players = allPlayers;
        }
        
        if (players.size() < 2) {
            throw new RuntimeException("至少需要2名玩家");
        }

        // 确定庄家（必须是已准备的玩家，如果设置了自动轮换，则轮换；否则使用指定的庄家）
        RoomPlayer dealer = players.stream()
                .filter(p -> p.getIsDealer() == 1)
                .findFirst()
                .orElse(players.get(0));
        
        // 保存当前对局的已准备玩家列表
        if (readyPlayerIds != null) {
            currentRoundReadyPlayers.put(roomId, readyPlayerIds);
        }

        // 只在第一局开始时重置所有玩家的房间积分为0
        // 之后每局累加/减，直到房间结束
        int nextRound = room.getCurrentRound() + 1;
        if (nextRound == 1) {
            // 第一局，重置所有玩家房间积分为0
            for (RoomPlayer player : players) {
                player.setTotalScore(0);
                roomPlayerMapper.updateById(player);
            }
        }

        // 创建对局记录（先创建记录，再更新房间状态）
        GameRecord record = new GameRecord();
        record.setRoomId(roomId);
        record.setRoundNumber(nextRound); // 使用计算出的 nextRound，而不是 room.getCurrentRound()
        record.setDealerId(dealer.getUserId());
        record.setStatus(RoundStatus.IN_PROGRESS.getCode());
        record.setStartTime(LocalDateTime.now());
        gameRecordMapper.insert(record);

        // 更新房间状态
        room.setCurrentRound(nextRound);
        room.setStatus(GameStatus.GAMING.getCode());
        roomMapper.updateById(room);

        return record;
    }

    /**
     * 玩家投注（庄家不能投注）
     */
    public void placeBet(Long gameRecordId, Long userId, Integer betAmount) {
        GameRecord record = gameRecordMapper.selectById(gameRecordId);
        if (record == null || record.getStatus() != RoundStatus.IN_PROGRESS.getCode()) {
            throw new RuntimeException("对局不存在或已结束");
        }

        // 检查玩家是否在房间中
        RoomPlayer player = getRoomPlayer(record.getRoomId(), userId);
        if (player == null) {
            throw new RuntimeException("玩家不在房间中");
        }

        // 检查是否是庄家，庄家不能投注
        if (record.getDealerId().equals(userId)) {
            throw new RuntimeException("庄家不需要投注");
        }

        // 保存投注信息
        currentGameBets.computeIfAbsent(gameRecordId, k -> new HashMap<>()).put(userId, betAmount);
    }

    /**
     * 获取当前对局的投注信息
     */
    public Map<Long, Integer> getCurrentGameBets(Long gameRecordId) {
        return currentGameBets.getOrDefault(gameRecordId, new HashMap<>());
    }

    /**
     * 发牌（只给已准备的玩家发牌）
     */
    @Transactional
    public Map<Long, List<CardTypeCalculator.Card>> dealCards(Long gameRecordId) {
        GameRecord record = gameRecordMapper.selectById(gameRecordId);
        if (record == null) {
            throw new RuntimeException("对局不存在");
        }

        // 获取已准备的玩家列表
        Set<Long> readyPlayerIds = currentRoundReadyPlayers.get(record.getRoomId());
        List<RoomPlayer> allPlayers = getRoomPlayers(record.getRoomId());
        
        // 只给已准备的玩家发牌
        List<RoomPlayer> players;
        if (readyPlayerIds != null && !readyPlayerIds.isEmpty()) {
            players = allPlayers.stream()
                    .filter(p -> readyPlayerIds.contains(p.getUserId()))
                    .collect(java.util.stream.Collectors.toList());
        } else {
            players = allPlayers;
        }
        
        // 生成并洗牌
        List<CardTypeCalculator.Card> deck = CardTypeCalculator.generateDeck();
        CardTypeCalculator.shuffle(deck);

        // 发牌（每人5张）
        List<List<CardTypeCalculator.Card>> hands = CardTypeCalculator.dealCards(deck, players.size());

        // 保存牌面信息
        Map<Long, List<CardTypeCalculator.Card>> cardsMap = new HashMap<>();
        for (int i = 0; i < players.size(); i++) {
            cardsMap.put(players.get(i).getUserId(), hands.get(i));
        }
        currentGameCards.put(gameRecordId, cardsMap);
        
        // 重置开牌状态
        currentGameRevealed.remove(gameRecordId);

        return cardsMap;
    }

    /**
     * 结算
     */
    @Transactional
    public Map<Long, GameDetail> settleRound(Long gameRecordId) {
        GameRecord record = gameRecordMapper.selectById(gameRecordId);
        if (record == null) {
            throw new RuntimeException("对局不存在");
        }

        Room room = roomMapper.selectById(record.getRoomId());
        List<String> enabledCardTypes = JSON.parseArray(room.getEnabledCardTypes(), String.class);
        Set<String> enabledTypesSet = new HashSet<>(enabledCardTypes);

        // 获取已准备的玩家列表（只结算已准备的玩家）
        Set<Long> readyPlayerIds = currentRoundReadyPlayers.get(record.getRoomId());
        List<RoomPlayer> allPlayers = getRoomPlayers(record.getRoomId());
        
        // 只结算已准备的玩家
        List<RoomPlayer> players;
        if (readyPlayerIds != null && !readyPlayerIds.isEmpty()) {
            players = allPlayers.stream()
                    .filter(p -> readyPlayerIds.contains(p.getUserId()))
                    .collect(java.util.stream.Collectors.toList());
        } else {
            players = allPlayers;
        }
        
        RoomPlayer dealer = players.stream()
                .filter(p -> p.getUserId().equals(record.getDealerId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("庄家不存在"));

        Map<Long, List<CardTypeCalculator.Card>> cardsMap = currentGameCards.get(gameRecordId);
        Map<Long, Integer> betsMap = currentGameBets.getOrDefault(gameRecordId, new HashMap<>());

        // 计算所有玩家的牌型
        Map<Long, CardType> playerCardTypes = new HashMap<>();
        for (RoomPlayer player : players) {
            List<CardTypeCalculator.Card> cards = cardsMap.get(player.getUserId());
            if (cards != null) {
                CardType cardType = CardTypeCalculator.calculateCardType(cards, enabledTypesSet);
                playerCardTypes.put(player.getUserId(), cardType);
            }
        }

        // 计算庄家牌型
        List<CardTypeCalculator.Card> dealerCards = cardsMap.get(dealer.getUserId());
        CardType dealerCardType = CardTypeCalculator.calculateCardType(dealerCards, enabledTypesSet);

        // 结算每个玩家
        Map<Long, GameDetail> details = new HashMap<>();
        for (RoomPlayer player : players) {
            if (player.getUserId().equals(dealer.getUserId())) {
                continue; // 庄家单独结算
            }

            Integer betAmount = betsMap.getOrDefault(player.getUserId(), 10); // 默认10元
            CardType playerCardType = playerCardTypes.get(player.getUserId());
            List<CardTypeCalculator.Card> playerCards = cardsMap.get(player.getUserId());

            // 比较牌型（包含牌面比较，当牌型相同时会比较单牌大小和花色）
            // compareCardType(type1, cards1, type2, cards2) 返回 level2 - level1
            // 如果玩家牌型更大，返回负数；如果庄家牌型更大，返回正数
            int compare = CardTypeCalculator.compareCardType(playerCardType, playerCards, dealerCardType, dealerCards);
            boolean isWinner = compare < 0; // 玩家牌型更大时返回负数，所以 < 0 表示玩家赢

            // 计算积分变化
            // 斗牛规则：
            // - 赢了：得到 投注额 × 玩家牌型倍数（净收益）
            // - 输了：扣除 投注额 × 庄家牌型倍数
            // 注意：投注时没有扣除积分，所以结算时直接计算净收益或净损失
            int scoreChange;
            if (isWinner) {
                // 赢了：投注额 × 玩家牌型倍数 = 净收益
                // 例如：投注50，牛8（倍数2），得到 50 × 2 = 100
                // 例如：投注50，牛牛（倍数3），得到 50 × 3 = 150
                int multiplier = playerCardType.getMultiplier();
                scoreChange = betAmount * multiplier;
                log.info("玩家 {} 赢了，投注额: {}, 玩家牌型: {} (倍数: {}), 庄家牌型: {} (倍数: {}), 比较结果: {}, 积分变化: {}", 
                    player.getUserId(), betAmount, playerCardType.getName(), multiplier, 
                    dealerCardType.getName(), dealerCardType.getMultiplier(), compare, scoreChange);
            } else if (compare > 0) {
                // 输了：扣除 投注额 × 庄家牌型倍数
                // 例如：投注50，庄家牛牛（倍数3），扣除 50 × 3 = 150
                // 例如：投注50，庄家牛8（倍数2），扣除 50 × 2 = 100
                int dealerMultiplier = dealerCardType.getMultiplier();
                scoreChange = -betAmount * dealerMultiplier;
                log.info("玩家 {} 输了，投注额: {}, 玩家牌型: {} (倍数: {}), 庄家牌型: {} (倍数: {}), 比较结果: {}, 积分变化: {}", 
                    player.getUserId(), betAmount, playerCardType.getName(), playerCardType.getMultiplier(),
                    dealerCardType.getName(), dealerMultiplier, compare, scoreChange);
            } else {
                // 平局（compare == 0）：不输不赢
                scoreChange = 0;
                log.info("玩家 {} 平局，投注额: {}, 玩家牌型: {} (倍数: {}), 庄家牌型: {} (倍数: {}), 比较结果: {}, 积分变化: {}", 
                    player.getUserId(), betAmount, playerCardType.getName(), playerCardType.getMultiplier(),
                    dealerCardType.getName(), dealerCardType.getMultiplier(), compare, scoreChange);
            }

            // 创建对局详情
            GameDetail detail = new GameDetail();
            detail.setGameRecordId(gameRecordId);
            detail.setUserId(player.getUserId());
            detail.setSeatNumber(player.getSeatNumber());
            detail.setBetAmount(betAmount);
            detail.setCards(JSON.toJSONString(cardsMap.get(player.getUserId())));
            detail.setCardType(playerCardType.getName());
            detail.setMultiplier(playerCardType.getMultiplier());
            detail.setScoreChange(scoreChange);
            detail.setIsWinner(isWinner ? 1 : 0);
            gameDetailMapper.insert(detail);

            // 更新玩家房间积分
            player.setTotalScore(player.getTotalScore() + scoreChange);
            roomPlayerMapper.updateById(player);

            // 更新用户全局积分
            userService.updateBalance(player.getUserId(), scoreChange);

            details.put(player.getUserId(), detail);
        }

        // 结算庄家（与所有玩家结算）
        int dealerScoreChange = 0;
        for (GameDetail detail : details.values()) {
            dealerScoreChange -= detail.getScoreChange();
        }

        // 庄家详情
        GameDetail dealerDetail = new GameDetail();
        dealerDetail.setGameRecordId(gameRecordId);
        dealerDetail.setUserId(dealer.getUserId());
        dealerDetail.setSeatNumber(dealer.getSeatNumber());
        dealerDetail.setBetAmount(0);
        dealerDetail.setCards(JSON.toJSONString(dealerCards));
        dealerDetail.setCardType(dealerCardType.getName());
        dealerDetail.setMultiplier(dealerCardType.getMultiplier());
        dealerDetail.setScoreChange(dealerScoreChange);
        dealerDetail.setIsWinner(dealerScoreChange > 0 ? 1 : 0);
        gameDetailMapper.insert(dealerDetail);

        // 更新庄家房间积分
        dealer.setTotalScore(dealer.getTotalScore() + dealerScoreChange);
        roomPlayerMapper.updateById(dealer);

        // 更新用户全局积分
        userService.updateBalance(dealer.getUserId(), dealerScoreChange);

        details.put(dealer.getUserId(), dealerDetail);

        // 更新对局记录状态
        record.setStatus(RoundStatus.SETTLED.getCode());
        record.setEndTime(LocalDateTime.now());
        gameRecordMapper.updateById(record);

        // 检查是否所有对局已完成（使用之前获取的room对象）
        if (room != null && room.getCurrentRound() >= room.getMaxRounds()) {
            // 所有对局已完成，更新房间状态为"已结束"
            room.setStatus(GameStatus.FINISHED.getCode());
            roomMapper.updateById(room);
        }

        // 清理缓存
        currentGameCards.remove(gameRecordId);
        currentGameBets.remove(gameRecordId);
        currentGameRevealed.remove(gameRecordId);
        currentRoundReadyPlayers.remove(record.getRoomId());

        return details;
    }
    
    /**
     * 记录玩家开牌
     */
    public void revealCard(Long gameRecordId, Long userId) {
        GameRecord record = gameRecordMapper.selectById(gameRecordId);
        if (record == null) {
            throw new RuntimeException("对局不存在");
        }
        
        // 记录开牌状态
        currentGameRevealed.computeIfAbsent(gameRecordId, k -> new java.util.HashSet<>()).add(userId);
    }
    
    /**
     * 检查是否所有玩家都已开牌
     */
    public boolean areAllPlayersRevealed(Long gameRecordId) {
        GameRecord record = gameRecordMapper.selectById(gameRecordId);
        if (record == null) {
            return false;
        }
        
        List<RoomPlayer> players = getRoomPlayers(record.getRoomId());
        java.util.Set<Long> revealed = currentGameRevealed.getOrDefault(gameRecordId, new java.util.HashSet<>());
        
        // 所有玩家都需要开牌（包括庄家）
        return revealed.size() == players.size();
    }

    /**
     * 提前结算（管理员操作）
     */
    @Transactional
    public void finishGame(Long roomId, Long adminId) {
        Room room = roomMapper.selectById(roomId);
        if (room == null) {
            throw new RuntimeException("房间不存在");
        }

        if (!room.getAdminId().equals(adminId)) {
            throw new RuntimeException("无权限");
        }

        room.setStatus(GameStatus.FINISHED.getCode());
        roomMapper.updateById(room);
    }

    /**
     * 设置庄家
     */
    @Transactional
    public void setDealer(Long roomId, Long userId) {
        // 清除当前庄家
        LambdaQueryWrapper<RoomPlayer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoomPlayer::getRoomId, roomId)
                .eq(RoomPlayer::getIsDealer, 1);
        List<RoomPlayer> dealers = roomPlayerMapper.selectList(wrapper);
        for (RoomPlayer dealer : dealers) {
            dealer.setIsDealer(0);
            roomPlayerMapper.updateById(dealer);
        }

        // 设置新庄家
        RoomPlayer player = getRoomPlayer(roomId, userId);
        if (player == null) {
            throw new RuntimeException("玩家不在房间中");
        }
        player.setIsDealer(1);
        roomPlayerMapper.updateById(player);
    }

    private List<RoomPlayer> getRoomPlayers(Long roomId) {
        LambdaQueryWrapper<RoomPlayer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoomPlayer::getRoomId, roomId)
                .orderByAsc(RoomPlayer::getSeatNumber);
        return roomPlayerMapper.selectList(wrapper);
    }

    private RoomPlayer getRoomPlayer(Long roomId, Long userId) {
        LambdaQueryWrapper<RoomPlayer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoomPlayer::getRoomId, roomId)
                .eq(RoomPlayer::getUserId, userId);
        return roomPlayerMapper.selectOne(wrapper);
    }

    /**
     * 获取对局记录
     */
    public GameRecord getGameRecord(Long gameRecordId) {
        return gameRecordMapper.selectById(gameRecordId);
    }
    
    /**
     * 获取当前对局的牌面信息
     */
    public Map<Long, List<CardTypeCalculator.Card>> getCurrentGameCards(Long gameRecordId) {
        return currentGameCards.getOrDefault(gameRecordId, new HashMap<>());
    }
}

