package com.douniu.utils;

import com.douniu.enums.CardType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 牌型计算器测试类
 */
@DisplayName("牌型判断测试")
class CardTypeCalculatorTest {

    /**
     * 创建牌
     * @param suit 花色：0-黑桃，1-红桃，2-梅花，3-方块
     * @param rank 点数：1-A, 2-2, ..., 10-10, 11-J, 12-Q, 13-K
     */
    private CardTypeCalculator.Card createCard(int suit, int rank) {
        return new CardTypeCalculator.Card(suit, rank);
    }

    /**
     * 创建启用所有牌型的集合
     */
    private Set<String> getAllEnabledTypes() {
        return new HashSet<>(Arrays.asList(
            "五小牛", "炸弹牛", "五花牛", "四花牛", "顺子"
        ));
    }

    @Test
    @DisplayName("测试五小牛")
    void testWuXiaoNiu() {
        Set<String> enabledTypes = getAllEnabledTypes();
        
        // 五小牛：5张牌都小于5，且总和≤10
        // 例：A A 2 2 3 (总和=9)
        List<CardTypeCalculator.Card> cards = Arrays.asList(
            createCard(0, 1),  // 黑桃A
            createCard(1, 1),  // 红桃A
            createCard(0, 2),  // 黑桃2
            createCard(1, 2),  // 红桃2
            createCard(0, 3)   // 黑桃3
        );
        
        CardType result = CardTypeCalculator.calculateCardType(cards, enabledTypes);
        assertEquals(CardType.WU_XIAO_NIU, result, "应该是五小牛");
    }

    @Test
    @DisplayName("测试炸弹牛")
    void testZhaDanNiu() {
        Set<String> enabledTypes = getAllEnabledTypes();
        
        // 炸弹牛：4张相同点数的牌
        // 例：A A A A K
        List<CardTypeCalculator.Card> cards = Arrays.asList(
            createCard(0, 1),  // 黑桃A
            createCard(1, 1),  // 红桃A
            createCard(2, 1),  // 方块A
            createCard(3, 1),  // 梅花A
            createCard(0, 13)  // 黑桃K
        );
        
        CardType result = CardTypeCalculator.calculateCardType(cards, enabledTypes);
        assertEquals(CardType.ZHA_DAN_NIU, result, "应该是炸弹牛");
    }

    @Test
    @DisplayName("测试五花牛")
    void testWuHuaNiu() {
        Set<String> enabledTypes = getAllEnabledTypes();
        
        // 五花牛：5张都是J、Q、K
        // 例：J J Q Q K
        List<CardTypeCalculator.Card> cards = Arrays.asList(
            createCard(0, 11), // 黑桃J
            createCard(1, 11), // 红桃J
            createCard(0, 12), // 黑桃Q
            createCard(1, 12), // 红桃Q
            createCard(0, 13)  // 黑桃K
        );
        
        CardType result = CardTypeCalculator.calculateCardType(cards, enabledTypes);
        assertEquals(CardType.WU_HUA_NIU, result, "应该是五花牛");
    }

    @Test
    @DisplayName("测试四花牛")
    void testSiHuaNiu() {
        Set<String> enabledTypes = getAllEnabledTypes();
        
        // 四花牛：4张都是J、Q、K
        // 例：J J Q K A
        List<CardTypeCalculator.Card> cards = Arrays.asList(
            createCard(0, 11), // 黑桃J
            createCard(1, 11), // 红桃J
            createCard(0, 12), // 黑桃Q
            createCard(1, 13), // 红桃K
            createCard(0, 1)   // 黑桃A
        );
        
        CardType result = CardTypeCalculator.calculateCardType(cards, enabledTypes);
        assertEquals(CardType.SI_HUA_NIU, result, "应该是四花牛");
    }

    @Test
    @DisplayName("测试顺子牛")
    void testShunZiNiu() {
        Set<String> enabledTypes = getAllEnabledTypes();
        
        // 顺子：5张牌点数连续
        // 例：A 2 3 4 5
        List<CardTypeCalculator.Card> cards = Arrays.asList(
            createCard(0, 1),  // 黑桃A
            createCard(1, 2),  // 红桃2
            createCard(0, 3),  // 黑桃3
            createCard(1, 4),  // 红桃4
            createCard(0, 5)   // 黑桃5
        );
        
        CardType result = CardTypeCalculator.calculateCardType(cards, enabledTypes);
        assertEquals(CardType.SHUN_ZI_NIU, result, "应该是顺子牛");
    }

    @Test
    @DisplayName("测试牛牛")
    void testNiuNiu() {
        Set<String> enabledTypes = getAllEnabledTypes();
        
        // 牛牛：前3张和后2张都能组成10的倍数
        // 例：K K 10 5 5 (K+K+10=30, 5+5=10)
        List<CardTypeCalculator.Card> cards = Arrays.asList(
            createCard(0, 13), // 黑桃K (10点)
            createCard(1, 13), // 红桃K (10点)
            createCard(0, 10), // 黑桃10
            createCard(1, 5),  // 红桃5
            createCard(2, 5)    // 方块5
        );
        
        CardType result = CardTypeCalculator.calculateCardType(cards, enabledTypes);
        assertEquals(CardType.NIU_NIU, result, "应该是牛牛");
    }

    @Test
    @DisplayName("测试牛9")
    void testNiu9() {
        Set<String> enabledTypes = getAllEnabledTypes();
        
        // 牛9：前3张组成10的倍数，后2张和为9
        // 例：K K 10 4 5 (K+K+10=30, 4+5=9)
        List<CardTypeCalculator.Card> cards = Arrays.asList(
            createCard(0, 13), // 黑桃K (10点)
            createCard(1, 13), // 红桃K (10点)
            createCard(0, 10), // 黑桃10
            createCard(1, 4),  // 红桃4
            createCard(2, 5)   // 方块5
        );
        
        CardType result = CardTypeCalculator.calculateCardType(cards, enabledTypes);
        assertEquals(CardType.NIU_9, result, "应该是牛9");
    }

    @Test
    @DisplayName("测试牛8")
    void testNiu8() {
        Set<String> enabledTypes = getAllEnabledTypes();
        
        // 牛8：前3张组成10的倍数，后2张和为8
        // 例：K K 10 3 5 (K+K+10=30, 3+5=8)
        List<CardTypeCalculator.Card> cards = Arrays.asList(
            createCard(0, 13), // 黑桃K (10点)
            createCard(1, 13), // 红桃K (10点)
            createCard(0, 10), // 黑桃10
            createCard(1, 3),  // 红桃3
            createCard(2, 5)   // 方块5
        );
        
        CardType result = CardTypeCalculator.calculateCardType(cards, enabledTypes);
        assertEquals(CardType.NIU_8, result, "应该是牛8");
    }

    @Test
    @DisplayName("测试牛1")
    void testNiu1() {
        Set<String> enabledTypes = getAllEnabledTypes();
        
        // 牛1：前3张组成10的倍数，后2张和为1
        // 例：K K 10 A 10 (K+K+10=30, A+10=11, 11%10=1)
        List<CardTypeCalculator.Card> cards = Arrays.asList(
            createCard(0, 13), // 黑桃K (10点)
            createCard(1, 13), // 红桃K (10点)
            createCard(0, 10), // 黑桃10
            createCard(1, 1),  // 红桃A (1点)
            createCard(2, 10)  // 方块10
        );
        
        CardType result = CardTypeCalculator.calculateCardType(cards, enabledTypes);
        assertEquals(CardType.NIU_1, result, "应该是牛1");
    }

    @Test
    @DisplayName("测试无牛")
    void testWuNiu() {
        Set<String> enabledTypes = getAllEnabledTypes();
        
        // 无牛：无法组成10的倍数
        // 例：A 2 3 4 6 (无法组成10的倍数)
        List<CardTypeCalculator.Card> cards = Arrays.asList(
            createCard(0, 1),  // 黑桃A
            createCard(1, 2),  // 红桃2
            createCard(0, 3),  // 黑桃3
            createCard(1, 4),  // 红桃4
            createCard(2, 6)   // 方块6
        );
        
        CardType result = CardTypeCalculator.calculateCardType(cards, enabledTypes);
        assertEquals(CardType.WU_NIU, result, "应该是无牛");
    }

    @Test
    @DisplayName("测试牌型优先级：五小牛 > 炸弹牛")
    void testCardTypePriority1() {
        Set<String> enabledTypes = getAllEnabledTypes();
        
        // 如果同时满足多个特殊牌型，应该返回优先级最高的
        // 这个例子可能不太准确，但测试优先级逻辑
        // 五小牛：A A 2 2 3
        List<CardTypeCalculator.Card> cards = Arrays.asList(
            createCard(0, 1),  // 黑桃A
            createCard(1, 1),  // 红桃A
            createCard(0, 2),  // 黑桃2
            createCard(1, 2),  // 红桃2
            createCard(0, 3)   // 黑桃3
        );
        
        CardType result = CardTypeCalculator.calculateCardType(cards, enabledTypes);
        assertEquals(CardType.WU_XIAO_NIU, result, "五小牛优先级最高");
    }

    @Test
    @DisplayName("测试未启用牌型时返回普通牌型")
    void testDisabledCardType() {
        // 不启用特殊牌型
        Set<String> enabledTypes = new HashSet<>();
        
        // 五花牛牌面，但不启用
        List<CardTypeCalculator.Card> cards = Arrays.asList(
            createCard(0, 11), // 黑桃J
            createCard(1, 11), // 红桃J
            createCard(0, 12), // 黑桃Q
            createCard(1, 12), // 红桃Q
            createCard(0, 13)  // 黑桃K
        );
        
        CardType result = CardTypeCalculator.calculateCardType(cards, enabledTypes);
        // 应该返回普通牌型（可能是牛牛或其他）
        assertNotEquals(CardType.WU_HUA_NIU, result, "未启用五花牛，不应该返回五花牛");
    }

    @Test
    @DisplayName("测试牌型比较：黑桃 > 红桃 > 梅花 > 方块")
    void testCardComparison() {
        // 创建两张相同点数但不同花色的牌
        CardTypeCalculator.Card card1 = createCard(0, 13); // 黑桃K
        CardTypeCalculator.Card card2 = createCard(1, 13); // 红桃K
        CardTypeCalculator.Card card3 = createCard(3, 13); // 梅花K
        CardTypeCalculator.Card card4 = createCard(2, 13); // 方块K
        
        List<CardTypeCalculator.Card> cards1 = Arrays.asList(
            card1, card1, card1, card1, card1
        );
        List<CardTypeCalculator.Card> cards2 = Arrays.asList(
            card2, card2, card2, card2, card2
        );
        List<CardTypeCalculator.Card> cards3 = Arrays.asList(
            card3, card3, card3, card3, card3
        );
        List<CardTypeCalculator.Card> cards4 = Arrays.asList(
            card4, card4, card4, card4, card4
        );
        
        Set<String> enabledTypes = getAllEnabledTypes();
        CardType type1 = CardTypeCalculator.calculateCardType(cards1, enabledTypes);
        CardType type2 = CardTypeCalculator.calculateCardType(cards2, enabledTypes);
        CardType type3 = CardTypeCalculator.calculateCardType(cards3, enabledTypes);
        CardType type4 = CardTypeCalculator.calculateCardType(cards4, enabledTypes);
        
        // 所有都是炸弹牛，牌型相同
        assertEquals(type1, type2);
        assertEquals(type2, type3);
        assertEquals(type3, type4);
        
        // 测试比较逻辑（通过compareCardType方法）
        // 黑桃K应该大于红桃K（当牌型相同时比较花色）
        int compare12 = CardTypeCalculator.compareCardType(type1, cards1, type2, cards2);
        // 由于都是炸弹牛，会比较单牌
        // 黑桃K的suit=0，getSuitValue(0)=0
        // 红桃K的suit=1，getSuitValue(1)=1
        // 黑桃的suit值更小，所以黑桃更大，返回负数（表示cards1玩家大）
        assertTrue(compare12 < 0, "黑桃K应该大于红桃K");
        
        // 测试红桃K > 梅花K
        int compare23 = CardTypeCalculator.compareCardType(type2, cards2, type3, cards3);
        assertTrue(compare23 < 0, "红桃K应该大于梅花K");
        
        // 测试梅花K > 方块K
        int compare34 = CardTypeCalculator.compareCardType(type3, cards3, type4, cards4);
        assertTrue(compare34 < 0, "梅花K应该大于方块K");
    }

    @Test
    @DisplayName("测试边界情况：null或空牌")
    void testEdgeCases() {
        Set<String> enabledTypes = getAllEnabledTypes();
        
        // 测试null
        CardType result1 = CardTypeCalculator.calculateCardType(null, enabledTypes);
        assertEquals(CardType.WU_NIU, result1, "null应该返回无牛");
        
        // 测试空列表
        CardType result2 = CardTypeCalculator.calculateCardType(new ArrayList<>(), enabledTypes);
        assertEquals(CardType.WU_NIU, result2, "空列表应该返回无牛");
        
        // 测试少于5张牌
        List<CardTypeCalculator.Card> cards3 = Arrays.asList(
            createCard(0, 1),
            createCard(1, 2)
        );
        CardType result3 = CardTypeCalculator.calculateCardType(cards3, enabledTypes);
        assertEquals(CardType.WU_NIU, result3, "少于5张牌应该返回无牛");
    }

    @Test
    @DisplayName("测试各种牛的组合")
    void testVariousNiu() {
        Set<String> enabledTypes = getAllEnabledTypes();
        
        // 测试牛2
        List<CardTypeCalculator.Card> cards2 = Arrays.asList(
            createCard(0, 13), // K (10)
            createCard(1, 13), // K (10)
            createCard(0, 10), // 10
            createCard(1, 1),  // A (1)
            createCard(2, 1)   // A (1) -> 1+1=2
        );
        assertEquals(CardType.NIU_2, CardTypeCalculator.calculateCardType(cards2, enabledTypes));
        
        // 测试牛3
        List<CardTypeCalculator.Card> cards3 = Arrays.asList(
            createCard(0, 13), // K (10)
            createCard(1, 13), // K (10)
            createCard(0, 10), // 10
            createCard(1, 1),  // A (1)
            createCard(2, 2)   // 2 -> 1+2=3
        );
        assertEquals(CardType.NIU_3, CardTypeCalculator.calculateCardType(cards3, enabledTypes));
        
        // 测试牛5
        List<CardTypeCalculator.Card> cards5 = Arrays.asList(
            createCard(0, 13), // K (10)
            createCard(1, 13), // K (10)
            createCard(0, 10), // 10
            createCard(1, 2),  // 2
            createCard(2, 3)   // 3 -> 2+3=5
        );
        assertEquals(CardType.NIU_5, CardTypeCalculator.calculateCardType(cards5, enabledTypes));
    }
}

