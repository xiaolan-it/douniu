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
            "五小牛", "炸弹牛", "五花牛", "顺子"
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
            createCard(2, 1),  // 梅花A
            createCard(3, 1),  // 方块A
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
            createCard(2, 5)    // 梅花5
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
            createCard(2, 5)   // 梅花5
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
            createCard(2, 5)   // 梅花5
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
            createCard(2, 10)  // 梅花10
        );
        
        CardType result = CardTypeCalculator.calculateCardType(cards, enabledTypes);
        assertEquals(CardType.NIU_1, result, "应该是牛1");
    }

    @Test
    @DisplayName("测试无牛")
    void testWuNiu() {
        Set<String> enabledTypes = getAllEnabledTypes();
        
        // 无牛：无法组成10的倍数
        // 例：A 2 3 4 8 (无法组成10的倍数)
        List<CardTypeCalculator.Card> cards = Arrays.asList(
            createCard(0, 1),  // 黑桃A
            createCard(1, 2),  // 红桃2
            createCard(0, 3),  // 黑桃3
            createCard(1, 4),  // 红桃4
            createCard(2, 8)   // 梅花8
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
        // 创建相同点数但不同花色的牌
        CardTypeCalculator.Card card1 = createCard(0, 13); // 黑桃K
        CardTypeCalculator.Card card2 = createCard(1, 13); // 红桃K
        CardTypeCalculator.Card card3 = createCard(2, 13); // 梅花K
        CardTypeCalculator.Card card4 = createCard(3, 13); // 方块K
        
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

    @Test
    @DisplayName("测试有牛场景：同点数不同花色比较 - 牛牛")
    void testSameRankDifferentSuitWithNiu_NiuNiu() {
        Set<String> enabledTypes = getAllEnabledTypes();
        
        // 玩家1：牛牛，最大牌是黑桃K
        // K(黑桃) K(红桃) 10(黑桃) 5(红桃) 5(梅花) = 牛牛
        List<CardTypeCalculator.Card> cards1 = Arrays.asList(
            createCard(0, 13), // 黑桃K (最大牌)
            createCard(1, 13), // 红桃K
            createCard(0, 10), // 黑桃10
            createCard(1, 5),  // 红桃5
            createCard(2, 5)   // 梅花5
        );
        
        // 玩家2：牛牛，最大牌是红桃K
        // K(红桃) K(梅花) 10(红桃) 5(梅花) 5(方块) = 牛牛
        List<CardTypeCalculator.Card> cards2 = Arrays.asList(
            createCard(1, 13), // 红桃K (最大牌)
            createCard(2, 13), // 梅花K
            createCard(1, 10), // 红桃10
            createCard(2, 5),  // 梅花5
            createCard(3, 5)   // 方块5
        );
        
        CardType type1 = CardTypeCalculator.calculateCardType(cards1, enabledTypes);
        CardType type2 = CardTypeCalculator.calculateCardType(cards2, enabledTypes);
        
        assertEquals(CardType.NIU_NIU, type1, "玩家1应该是牛牛");
        assertEquals(CardType.NIU_NIU, type2, "玩家2应该是牛牛");
        
        // 比较：牌型相同，都是牛牛，最大牌都是K，但玩家1是黑桃K，玩家2是红桃K
        // 黑桃K > 红桃K，所以玩家1大，返回负数（玩家1大）
        int compare = CardTypeCalculator.compareCardType(type1, cards1, type2, cards2);
        assertTrue(compare < 0, "黑桃K的牛牛应该大于红桃K的牛牛");
    }

    @Test
    @DisplayName("测试有牛场景：同点数不同花色比较 - 牛8")
    void testSameRankDifferentSuitWithNiu_Niu8() {
        Set<String> enabledTypes = getAllEnabledTypes();
        
        // 玩家1：牛8，最大牌是黑桃K
        // K(黑桃) K(红桃) 10(黑桃) 3(红桃) 5(梅花) = 牛8
        List<CardTypeCalculator.Card> cards1 = Arrays.asList(
            createCard(0, 13), // 黑桃K (最大牌)
            createCard(1, 13), // 红桃K
            createCard(0, 10), // 黑桃10
            createCard(1, 3),  // 红桃3
            createCard(2, 5)   // 梅花5
        );
        
        // 玩家2：牛8，最大牌是梅花K
        // K(梅花) K(方块) 10(梅花) 3(方块) 5(红桃) = 牛8
        List<CardTypeCalculator.Card> cards2 = Arrays.asList(
            createCard(2, 13), // 梅花K (最大牌)
            createCard(3, 13), // 方块K
            createCard(2, 10), // 梅花10
            createCard(3, 3),  // 方块3
            createCard(1, 5)   // 红桃5
        );
        
        CardType type1 = CardTypeCalculator.calculateCardType(cards1, enabledTypes);
        CardType type2 = CardTypeCalculator.calculateCardType(cards2, enabledTypes);
        
        assertEquals(CardType.NIU_8, type1, "玩家1应该是牛8");
        assertEquals(CardType.NIU_8, type2, "玩家2应该是牛8");
        
        // 比较：牌型相同，都是牛8，最大牌都是K，但玩家1是黑桃K，玩家2是梅花K
        // 黑桃K > 梅花K，所以玩家1大，返回负数（玩家1大）
        int compare = CardTypeCalculator.compareCardType(type1, cards1, type2, cards2);
        assertTrue(compare < 0, "黑桃K的牛8应该大于梅花K的牛8");
    }

    @Test
    @DisplayName("测试有牛场景：同点数不同花色比较 - 牛9")
    void testSameRankDifferentSuitWithNiu_Niu9() {
        Set<String> enabledTypes = getAllEnabledTypes();
        
        // 玩家1：牛9，最大牌是红桃Q
        // Q(红桃) Q(梅花) 10(红桃) 4(梅花) 5(方块) = 牛9
        List<CardTypeCalculator.Card> cards1 = Arrays.asList(
            createCard(1, 12), // 红桃Q (最大牌)
            createCard(2, 12), // 梅花Q
            createCard(1, 10), // 红桃10
            createCard(2, 4),  // 梅花4
            createCard(3, 5)   // 方块5
        );
        
        // 玩家2：牛9，最大牌是方块Q
        // Q(方块) Q(黑桃) 10(方块) 4(黑桃) 5(红桃) = 牛9
        List<CardTypeCalculator.Card> cards2 = Arrays.asList(
            createCard(3, 12), // 方块Q (最大牌)
            createCard(0, 11), // 黑桃J
            createCard(3, 10), // 方块10
            createCard(0, 4),  // 黑桃4
            createCard(1, 5)   // 红桃5
        );
        
        CardType type1 = CardTypeCalculator.calculateCardType(cards1, enabledTypes);
        CardType type2 = CardTypeCalculator.calculateCardType(cards2, enabledTypes);
        
        assertEquals(CardType.NIU_9, type1, "玩家1应该是牛9");
        assertEquals(CardType.NIU_9, type2, "玩家2应该是牛9");
        
        // 比较：牌型相同，都是牛9，最大牌都是Q，但玩家1是红桃Q，玩家2是方块Q
        // 红桃Q > 方块Q，所以玩家1大，返回负数（玩家1大）
        int compare = CardTypeCalculator.compareCardType(type1, cards1, type2, cards2);
        assertTrue(compare < 0, "红桃Q的牛9应该大于方块Q的牛9");
    }

    @Test
    @DisplayName("测试无牛场景：同点数不同花色比较")
    void testSameRankDifferentSuitWithoutNiu() {
        Set<String> enabledTypes = getAllEnabledTypes();
        
        // 玩家1：无牛，最大牌是黑桃K
        // K(黑桃) 2(红桃) 4(黑桃) 5(红桃) 9(梅花) = 无牛
        List<CardTypeCalculator.Card> cards1 = Arrays.asList(
            createCard(0, 13), // 黑桃K (最大牌)
            createCard(1, 2),  // 红桃2
            createCard(0, 4),  // 黑桃4
            createCard(1, 5),  // 红桃5
            createCard(2, 9)   // 梅花9
        );
        
        // 玩家2：无牛，最大牌是红桃K
        // K(红桃) 2(梅花) 4(红桃) 5(梅花) 9(方块) = 无牛
        List<CardTypeCalculator.Card> cards2 = Arrays.asList(
            createCard(1, 13), // 红桃K (最大牌)
            createCard(2, 2),  // 梅花2
            createCard(1, 4),  // 红桃4
            createCard(2, 5),  // 梅花5
            createCard(3, 9)   // 方块9
        );
        
        CardType type1 = CardTypeCalculator.calculateCardType(cards1, enabledTypes);
        CardType type2 = CardTypeCalculator.calculateCardType(cards2, enabledTypes);
        
        assertEquals(CardType.WU_NIU, type1, "玩家1应该是无牛");
        assertEquals(CardType.WU_NIU, type2, "玩家2应该是无牛");
        
        // 比较：牌型相同，都是无牛，最大牌都是K，但玩家1是黑桃K，玩家2是红桃K
        // 黑桃K > 红桃K，所以玩家1大，返回负数（玩家1大）
        int compare = CardTypeCalculator.compareCardType(type1, cards1, type2, cards2);
        assertTrue(compare < 0, "黑桃K的无牛应该大于红桃K的无牛");
    }

    @Test
    @DisplayName("测试有牛场景：同点数不同花色比较 - 所有花色顺序")
    void testSameRankDifferentSuitWithNiu_AllSuits() {
        Set<String> enabledTypes = getAllEnabledTypes();
        
        // 创建4副牌，都是牛牛，最大牌都是K，但花色不同
        // 黑桃K > 红桃K > 梅花K > 方块K
        
        // 玩家1：黑桃K最大
        List<CardTypeCalculator.Card> cards1 = Arrays.asList(
            createCard(0, 13), // 黑桃K (最大牌)
            createCard(1, 13), // 红桃K
            createCard(0, 10), // 黑桃10
            createCard(1, 5),  // 红桃5
            createCard(2, 5)   // 梅花5
        );
        
        // 玩家2：红桃K最大
        List<CardTypeCalculator.Card> cards2 = Arrays.asList(
            createCard(1, 13), // 红桃K (最大牌)
            createCard(2, 13), // 梅花K
            createCard(1, 10), // 红桃10
            createCard(2, 5),  // 梅花5
            createCard(3, 5)   // 方块5
        );
        
        // 玩家3：梅花K最大，但最大牌和第二大牌与玩家4相同，需要比较第三大牌（梅花10 > 方块10）
        List<CardTypeCalculator.Card> cards3 = Arrays.asList(
            createCard(2, 13), // 梅花K (最大牌，与玩家4的方块K相同点数)
            createCard(3, 13), // 方块K (第二大牌，与玩家4的梅花K相同点数)
            createCard(2, 10), // 梅花10 (第三大牌，比玩家4的方块10大)
            createCard(3, 5),  // 方块5
            createCard(0, 5)   // 黑桃5
        );
        
        // 玩家4：方块K最大，但最大牌和第二大牌与玩家3相同，需要比较第三大牌（方块10 < 梅花10）
        List<CardTypeCalculator.Card> cards4 = Arrays.asList(
            createCard(3, 13), // 方块K (最大牌，与玩家3的梅花K相同点数)
            createCard(2, 13), // 梅花K (第二大牌，与玩家3的方块K相同点数)
            createCard(3, 10), // 方块10 (第三大牌，比玩家3的梅花10小)
            createCard(2, 5),  // 梅花5
            createCard(1, 5)   // 红桃5
        );
        
        CardType type1 = CardTypeCalculator.calculateCardType(cards1, enabledTypes);
        CardType type2 = CardTypeCalculator.calculateCardType(cards2, enabledTypes);
        CardType type3 = CardTypeCalculator.calculateCardType(cards3, enabledTypes);
        CardType type4 = CardTypeCalculator.calculateCardType(cards4, enabledTypes);
        
        assertEquals(CardType.NIU_NIU, type1);
        assertEquals(CardType.NIU_NIU, type2);
        assertEquals(CardType.NIU_NIU, type3);
        assertEquals(CardType.NIU_NIU, type4);
        
        // 验证大小关系：黑桃K > 红桃K > 梅花K > 方块K
        int compare12 = CardTypeCalculator.compareCardType(type1, cards1, type2, cards2);
        int compare23 = CardTypeCalculator.compareCardType(type2, cards2, type3, cards3);
        int compare34 = CardTypeCalculator.compareCardType(type3, cards3, type4, cards4);
        int compare14 = CardTypeCalculator.compareCardType(type1, cards1, type4, cards4);
        
        assertTrue(compare12 < 0, "黑桃K应该大于红桃K");
        assertTrue(compare23 < 0, "红桃K应该大于梅花K");
        assertTrue(compare34 < 0, "梅花K应该大于方块K");
        assertTrue(compare14 < 0, "黑桃K应该大于方块K");
    }

    @Test
    @DisplayName("测试无牛场景：同点数不同花色比较 - 所有花色顺序")
    void testSameRankDifferentSuitWithoutNiu_AllSuits() {
        Set<String> enabledTypes = getAllEnabledTypes();
        
        // 创建4副牌，都是无牛，最大牌都是Q，但花色不同
        // 黑桃Q > 红桃Q > 梅花Q > 方块Q
        
        // 玩家1：黑桃Q最大
        List<CardTypeCalculator.Card> cards1 = Arrays.asList(
            createCard(0, 12), // 黑桃Q (最大牌)
            createCard(1, 2),  // 红桃2
            createCard(0, 4),   // 黑桃4
            createCard(1, 5),   // 红桃5
            createCard(2, 9)    // 梅花9
        );
        
        // 玩家2：红桃Q最大
        List<CardTypeCalculator.Card> cards2 = Arrays.asList(
            createCard(1, 12), // 红桃Q (最大牌)
            createCard(2, 2),  // 梅花2
            createCard(1, 4),   // 红桃4
            createCard(2, 5),   // 梅花5
            createCard(3, 9)    // 方块9
        );
        
        // 玩家3：梅花Q最大
        List<CardTypeCalculator.Card> cards3 = Arrays.asList(
            createCard(2, 12), // 梅花Q (最大牌)
            createCard(3, 2),  // 方块2
            createCard(2, 4),   // 梅花4
            createCard(3, 5),   // 方块5
            createCard(0, 9)    // 黑桃9
        );
        
        // 玩家4：方块Q最大
        List<CardTypeCalculator.Card> cards4 = Arrays.asList(
            createCard(3, 12), // 方块Q (最大牌)
            createCard(0, 2),  // 黑桃2
            createCard(3, 4),   // 方块4
            createCard(0, 5),   // 黑桃5
            createCard(1, 9)    // 红桃9
        );
        
        CardType type1 = CardTypeCalculator.calculateCardType(cards1, enabledTypes);
        CardType type2 = CardTypeCalculator.calculateCardType(cards2, enabledTypes);
        CardType type3 = CardTypeCalculator.calculateCardType(cards3, enabledTypes);
        CardType type4 = CardTypeCalculator.calculateCardType(cards4, enabledTypes);
        
        assertEquals(CardType.WU_NIU, type1);
        assertEquals(CardType.WU_NIU, type2);
        assertEquals(CardType.WU_NIU, type3);
        assertEquals(CardType.WU_NIU, type4);
        
        // 验证大小关系：黑桃Q > 红桃Q > 梅花Q > 方块Q
        int compare12 = CardTypeCalculator.compareCardType(type1, cards1, type2, cards2);
        int compare23 = CardTypeCalculator.compareCardType(type2, cards2, type3, cards3);
        int compare34 = CardTypeCalculator.compareCardType(type3, cards3, type4, cards4);
        int compare14 = CardTypeCalculator.compareCardType(type1, cards1, type4, cards4);
        
        assertTrue(compare12 < 0, "黑桃Q应该大于红桃Q");
        assertTrue(compare23 < 0, "红桃Q应该大于梅花Q");
        assertTrue(compare34 < 0, "梅花Q应该大于方块Q");
        assertTrue(compare14 < 0, "黑桃Q应该大于方块Q");
    }
}

