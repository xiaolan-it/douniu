package com.douniu.utils;

import com.douniu.enums.CardType;
import lombok.Data;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 斗牛牌型计算工具类
 */
public class CardTypeCalculator {

    @Data
    public static class Card {
        private int suit; // 花色：0-黑桃，1-红桃，2-梅花，3-方块（大小：黑桃>红桃>梅花>方块）
        private int rank; // 点数：1-A, 2-2, ..., 10-10, 11-J, 12-Q, 13-K

        public Card(int suit, int rank) {
            this.suit = suit;
            this.rank = rank;
        }

        public int getValue() {
            // 斗牛中J、Q、K都算10点
            if (rank >= 11) {
                return 10;
            }
            return rank;
        }
    }

    /**
     * 计算牌型
     * @param cards 5张牌
     * @param enabledTypes 启用的牌型列表
     * @return 牌型
     */
    public static CardType calculateCardType(List<Card> cards, Set<String> enabledTypes) {
        if (cards == null || cards.size() != 5) {
            return CardType.WU_NIU;
        }

        // 检查五小牛（5张牌都小于5，且总和小于等于10）
        if (enabledTypes.contains("五小牛") && isWuXiaoNiu(cards)) {
            return CardType.WU_XIAO_NIU;
        }

        // 检查炸弹牛（4张相同点数的牌）
        if (enabledTypes.contains("炸弹牛") && isZhaDanNiu(cards)) {
            return CardType.ZHA_DAN_NIU;
        }

        // 检查五花牛（5张都是J、Q、K）
        if (enabledTypes.contains("五花牛") && isWuHuaNiu(cards)) {
            return CardType.WU_HUA_NIU;
        }

        // 检查顺子牛（5张连续，且前3张能组成10的倍数）
        if (enabledTypes.contains("顺子") && isShunZiNiu(cards)) {
            return CardType.SHUN_ZI_NIU;
        }

        // 计算普通牛牛
        int[] cardValues = cards.stream().mapToInt(Card::getValue).toArray();
        int niuValue = calculateNiu(cardValues);
        
        if (niuValue == 0) {
            return CardType.NIU_NIU;
        } else if (niuValue >= 1 && niuValue <= 9) {
            return CardType.valueOf("NIU_" + niuValue);
        }

        return CardType.WU_NIU;
    }

    /**
     * 判断是否五小牛
     */
    private static boolean isWuXiaoNiu(List<Card> cards) {
        int sum = cards.stream().mapToInt(Card::getValue).sum();
        boolean allLessThan5 = cards.stream().allMatch(c -> c.getRank() < 5);
        return allLessThan5 && sum <= 10;
    }

    /**
     * 判断是否炸弹牛
     */
    private static boolean isZhaDanNiu(List<Card> cards) {
        Map<Integer, Long> rankCount = cards.stream()
                .collect(Collectors.groupingBy(Card::getRank, Collectors.counting()));
        return rankCount.values().stream().anyMatch(count -> count >= 4);
    }

    /**
     * 判断是否五花牛（5张都是J、Q、K）
     */
    private static boolean isWuHuaNiu(List<Card> cards) {
        return cards.stream().allMatch(c -> c.getRank() >= 11);
    }

    /**
     * 判断是否顺子牛（只需要是顺子即可，不需要校验是否为牛）
     */
    private static boolean isShunZiNiu(List<Card> cards) {
        List<Integer> ranks = cards.stream()
                .map(Card::getRank)
                .sorted()
                .collect(Collectors.toList());
        
        // 检查是否连续（5张牌点数连续）
        for (int i = 0; i < ranks.size() - 1; i++) {
            if (ranks.get(i + 1) - ranks.get(i) != 1) {
                return false;
            }
        }

        return true;
    }

    /**
     * 计算牛几（0表示牛牛，1-9表示牛1-牛9，-1表示无牛）
     */
    private static int calculateNiu(int[] cardValues) {
        if (hasNiu(cardValues)) {
            // 找到3张牌组成10的倍数后，计算剩余2张的和
            for (int i = 0; i < 5; i++) {
                for (int j = i + 1; j < 5; j++) {
                    for (int k = j + 1; k < 5; k++) {
                        int sum = cardValues[i] + cardValues[j] + cardValues[k];
                        if (sum % 10 == 0) {
                            int total = 0;
                            for (int m = 0; m < 5; m++) {
                                if (m != i && m != j && m != k) {
                                    total += cardValues[m];
                                }
                            }
                            return total % 10;
                        }
                    }
                }
            }
        }
        return -1;
    }
    
    /**
     * 获取牌型分组（用于前端显示）
     * @param cards 5张牌
     * @return 分组信息：{group1: [3张牌], group2: [2张牌]}
     */
    public static Map<String, List<Card>> getCardGroups(List<Card> cards) {
        Map<String, List<Card>> groups = new HashMap<>();
        if (cards == null || cards.size() != 5) {
            return groups;
        }
        
        int[] cardValues = cards.stream().mapToInt(Card::getValue).toArray();
        List<Card> cardList = new ArrayList<>(cards);
        
        // 查找3张牌组成10的倍数
        for (int i = 0; i < 5; i++) {
            for (int j = i + 1; j < 5; j++) {
                for (int k = j + 1; k < 5; k++) {
                    int sum = cardValues[i] + cardValues[j] + cardValues[k];
                    if (sum % 10 == 0) {
                        // 找到3张牌组成10的倍数
                        List<Card> group1 = new ArrayList<>();
                        group1.add(cardList.get(i));
                        group1.add(cardList.get(j));
                        group1.add(cardList.get(k));
                        
                        List<Card> group2 = new ArrayList<>();
                        for (int m = 0; m < 5; m++) {
                            if (m != i && m != j && m != k) {
                                group2.add(cardList.get(m));
                            }
                        }
                        
                        groups.put("group1", group1);
                        groups.put("group2", group2);
                        return groups;
                    }
                }
            }
        }
        
        // 如果没有牛，返回所有5张牌作为一组
        groups.put("group1", cardList);
        return groups;
    }

    /**
     * 判断是否有牛（是否有3张牌能组成10的倍数）
     */
    private static boolean hasNiu(int[] cardValues) {
        for (int i = 0; i < 5; i++) {
            for (int j = i + 1; j < 5; j++) {
                for (int k = j + 1; k < 5; k++) {
                    int sum = cardValues[i] + cardValues[j] + cardValues[k];
                    if (sum % 10 == 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 比较两个牌型大小（不比较牌面）
     * @return 正数表示type1大，负数表示type2大，0表示相等
     */
    public static int compareCardType(CardType type1, CardType type2) {
        // 先比较牌型等级
        int level1 = getCardTypeLevel(type1);
        int level2 = getCardTypeLevel(type2);
        
        if (level1 != level2) {
            return level2 - level1; // 等级高的更大
        }

        // 同等级时，比较具体牌型
        return type2.ordinal() - type1.ordinal();
    }

    /**
     * 比较两个牌型大小（包含牌面比较）
     * @param type1 牌型1（玩家）
     * @param cards1 牌面1（玩家）
     * @param type2 牌型2（庄家）
     * @param cards2 牌面2（庄家）
     * @return 正数表示type2（庄家）大，负数表示type1（玩家）大，0表示相等
     */
    public static int compareCardType(CardType type1, List<Card> cards1, CardType type2, List<Card> cards2) {
        // 先比较牌型等级
        int level1 = getCardTypeLevel(type1);
        int level2 = getCardTypeLevel(type2);
        
        if (level1 != level2) {
            return level2 - level1; // 等级高的更大，返回正数表示庄家大，负数表示玩家大
        }

        // 同等级时，比较具体牌型（ordinal越小，牌型越大）
        // 例如：WU_XIAO_NIU(0) > ZHA_DAN_NIU(1) > ... > WU_NIU(14)
        // 如果玩家ordinal更小（牌型更大），应该返回负数（玩家大）
        // 如果庄家ordinal更小（牌型更大），应该返回正数（庄家大）
        int typeCompare = type1.ordinal() - type2.ordinal();
        // typeCompare < 0 表示玩家ordinal更小（牌型更大），应该返回负数（玩家大）✓
        // typeCompare > 0 表示庄家ordinal更小（牌型更大），应该返回正数（庄家大）✓
        // 所以直接返回 typeCompare 即可
        if (typeCompare != 0) {
            return typeCompare;
        }

        // 牌型完全相同（比如都是无牛，或者都是牛牛），比较单牌大小和花色
        if (cards1 != null && cards2 != null && cards1.size() == 5 && cards2.size() == 5) {
            // compareCards返回：正数表示庄家（cards2）大，负数表示玩家（cards1）大
            return compareCards(cards1, cards2);
        }

        return 0;
    }

    /**
     * 比较两副牌的大小（当牌型相同时使用）
     * 规则：比较最大的一张牌，先比点数，点数相同再比花色（黑桃>红桃>梅花>方块）
     * 如果最大牌的点数相同，则按最大牌的花色比较大小
     * @param cards1 牌1（玩家）
     * @param cards2 牌2（庄家）
     * @return 正数表示cards2（庄家）大，负数表示cards1（玩家）大，0表示相等
     */
    private static int compareCards(List<Card> cards1, List<Card> cards2) {
        // 找到每副牌中最大的一张牌（先比点数，再比花色）
        Card maxCard1 = findMaxCard(cards1);
        Card maxCard2 = findMaxCard(cards2);

        // 先比较点数（rank）
        if (maxCard1.getRank() != maxCard2.getRank()) {
            // 点数大的更大，返回：正数表示庄家大，负数表示玩家大
            int rankCompare = maxCard2.getRank() - maxCard1.getRank();
            System.out.println(String.format("比较牌 - 点数不同: 玩家最大牌(rank=%d, suit=%d) vs 庄家最大牌(rank=%d, suit=%d), 结果=%d", 
                maxCard1.getRank(), maxCard1.getSuit(), maxCard2.getRank(), maxCard2.getSuit(), rankCompare));
            return rankCompare;
        }

        // 点数相同，比较花色（大小：黑桃(0) > 红桃(1) > 梅花(2) > 方块(3)）
        // 花色大小映射：0-黑桃(最大), 1-红桃, 2-梅花, 3-方块(最小)
        int suit1Value = getSuitValue(maxCard1.getSuit());
        int suit2Value = getSuitValue(maxCard2.getSuit());
        // suit值小的更大，返回：正数表示庄家大，负数表示玩家大
        int suitCompare = suit1Value - suit2Value;
        System.out.println(String.format("比较牌 - 点数相同，比较花色: 玩家最大牌(rank=%d, suit=%d, suitValue=%d) vs 庄家最大牌(rank=%d, suit=%d, suitValue=%d), 结果=%d", 
            maxCard1.getRank(), maxCard1.getSuit(), suit1Value, maxCard2.getRank(), maxCard2.getSuit(), suit2Value, suitCompare));
        return suitCompare;
    }

    /**
     * 获取花色的大小值（用于比较）
     * 大小：黑桃(0) > 红桃(1) > 梅花(2) > 方块(3)
     * @param suit 花色：0-黑桃，1-红桃，2-梅花，3-方块
     * @return 花色大小值（越小越大）
     */
    private static int getSuitValue(int suit) {
        // 映射：0-黑桃(0), 1-红桃(1), 2-梅花(2), 3-方块(3)
        // 这样：0 < 1 < 2 < 3，对应：黑桃 > 红桃 > 梅花 > 方块
        switch (suit) {
            case 0: return 0; // 黑桃 - 最大
            case 1: return 1; // 红桃
            case 2: return 2; // 梅花
            case 3: return 3; // 方块 - 最小
            default: return 4; // 未知花色
        }
    }

    /**
     * 找到一副牌中最大的一张牌
     * 规则：先比点数，点数相同再比花色（黑桃>红桃>梅花>方块）
     */
    private static Card findMaxCard(List<Card> cards) {
        Card maxCard = cards.get(0);
        for (int i = 1; i < cards.size(); i++) {
            Card card = cards.get(i);
            // 先比较点数
            if (card.getRank() > maxCard.getRank()) {
                maxCard = card;
            } else if (card.getRank() == maxCard.getRank()) {
                // 点数相同，比较花色（使用花色大小值，值越小越大）
                if (getSuitValue(card.getSuit()) < getSuitValue(maxCard.getSuit())) {
                    maxCard = card;
                }
            }
        }
        return maxCard;
    }

    /**
     * 获取牌型等级
     */
    private static int getCardTypeLevel(CardType type) {
        switch (type) {
            case WU_XIAO_NIU:
            case ZHA_DAN_NIU:
            case WU_HUA_NIU:
            case SHUN_ZI_NIU:
            case NIU_NIU:
                return 3; // 高级牌型
            case NIU_9:
            case NIU_8:
                return 2; // 中级牌型
            case NIU_7:
            case NIU_6:
            case NIU_5:
            case NIU_4:
            case NIU_3:
            case NIU_2:
            case NIU_1:
            case WU_NIU:
                return 1; // 低级牌型
            default:
                return 0;
        }
    }

    /**
     * 生成一副牌（52张）
     */
    public static List<Card> generateDeck() {
        List<Card> deck = new ArrayList<>();
        for (int suit = 0; suit < 4; suit++) {
            for (int rank = 1; rank <= 13; rank++) {
                deck.add(new Card(suit, rank));
            }
        }
        return deck;
    }

    /**
     * 洗牌
     */
    public static void shuffle(List<Card> deck) {
        Collections.shuffle(deck);
    }

    /**
     * 发牌（每人5张）
     */
    public static List<List<Card>> dealCards(List<Card> deck, int playerCount) {
        List<List<Card>> hands = new ArrayList<>();
        for (int i = 0; i < playerCount; i++) {
            hands.add(new ArrayList<>());
        }

        // 每人发5张
        for (int round = 0; round < 5; round++) {
            for (int player = 0; player < playerCount; player++) {
                if (!deck.isEmpty()) {
                    hands.get(player).add(deck.remove(0));
                }
            }
        }

        return hands;
    }
}

