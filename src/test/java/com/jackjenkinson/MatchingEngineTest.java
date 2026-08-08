package com.jackjenkinson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jackjenkinson.MatchingEngineSolutions.EngineSolution;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("MatchingEngine contract")
class MatchingEngineTest {

    private long nextOrderId = 1L;

    /** Builds an order with the next sequential id. */
    private Order order(Side side, int price, int quantity) {
        return new Order(nextOrderId++, side, price, quantity);
    }

    private Order buy(int price, int quantity) {
        return order(Side.BUY, price, quantity);
    }

    private Order sell(int price, int quantity) {
        return order(Side.SELL, price, quantity);
    }

    @Nested
    @DisplayName("resting orders (no match)")
    class Resting {

        @ParameterizedTest(name = "[{0}]")
        @EnumSource(EngineSolution.class)
        @DisplayName("a lone order produces no trades")
        void loneOrderProducesNoTrades(EngineSolution solution) {
            // given when
            MatchingEngine engine = solution.create();
            List<Trade> trades = engine.submitOrder(buy(100, 10));

            //then
            assertTrue(trades.isEmpty());
        }

        @ParameterizedTest(name = "[{0}]")
        @EnumSource(EngineSolution.class)
        @DisplayName("non-crossing bid and ask do not match")
        void nonCrossingOrdersDoNotMatch(EngineSolution solution) {
            //given
            MatchingEngine engine = solution.create();
            engine.submitOrder(buy(99, 10));

            //when
            List<Trade> trades = engine.submitOrder(sell(101, 10));

            //then
            assertTrue(trades.isEmpty());
        }
    }

    @Nested
    @DisplayName("matching")
    class Matching {

        @ParameterizedTest(name = "[{0}]")
        @EnumSource(EngineSolution.class)
        @DisplayName("exact match fully fills both orders at the resting price")
        void exactMatch(EngineSolution solution) {
            //given
            MatchingEngine engine = solution.create();
            Order resting = buy(100, 10);
            engine.submitOrder(resting);

            //when
            Order incoming = sell(100, 10);
            List<Trade> trades = engine.submitOrder(incoming);

            //then
            assertEquals(1, trades.size());
            Trade trade = trades.get(0);
            assertEquals(resting.getId(), trade.buyOrderId());
            assertEquals(incoming.getId(), trade.sellOrderId());
            assertEquals(100, trade.price());
            assertEquals(10, trade.quantity());
        }

        @ParameterizedTest(name = "[{0}]")
        @EnumSource(EngineSolution.class)
        @DisplayName("crossing order trades at the resting order's price")
        void tradesAtRestingPrice(EngineSolution solution) {
            //given
            MatchingEngine engine = solution.create();
            Order resting = sell(100, 10);
            engine.submitOrder(resting);

            //when
            // Aggressive buy willing to pay 105 should still trade at 100.
            List<Trade> trades = engine.submitOrder(buy(105, 10));

            //then
            assertEquals(1, trades.size());
            assertEquals(100, trades.get(0).price());
            assertEquals(10, trades.get(0).quantity());
        }

        @ParameterizedTest(name = "[{0}]")
        @EnumSource(EngineSolution.class)
        @DisplayName("incoming order partially fills a larger resting order")
        void incomingPartiallyFillsResting(EngineSolution solution) {
            //given
            MatchingEngine engine = solution.create();
            engine.submitOrder(buy(100, 10));

            //when
            List<Trade> trades = engine.submitOrder(sell(100, 4));

            //then
            assertEquals(1, trades.size());
            assertEquals(4, trades.get(0).quantity());

            // Remaining 6 should still rest and be matchable.
            List<Trade> more = engine.submitOrder(sell(100, 6));
            assertEquals(1, more.size());
            assertEquals(6, more.get(0).quantity());
        }

        @ParameterizedTest(name = "[{0}]")
        @EnumSource(EngineSolution.class)
        @DisplayName("large incoming order is filled across multiple resting orders")
        void sweepsMultipleRestingOrders(EngineSolution solution) {
            //given
            MatchingEngine engine = solution.create();
            Order first = buy(100, 5);
            Order second = buy(100, 5);
            engine.submitOrder(first);
            engine.submitOrder(second);

            //when
            List<Trade> trades = engine.submitOrder(sell(100, 10));

            //then
            assertEquals(2, trades.size());
            assertEquals(10, totalQuantity(trades));
        }

        @ParameterizedTest(name = "[{0}]")
        @EnumSource(EngineSolution.class)
        @DisplayName("incoming order rests after consuming all available liquidity")
        void restsRemainderAfterPartialSweep(EngineSolution solution) {
            MatchingEngine engine = solution.create();
            engine.submitOrder(sell(100, 4));

            List<Trade> trades = engine.submitOrder(buy(100, 10));
            assertEquals(4, totalQuantity(trades));

            // The 6-lot remainder should now be resting on the bid side.
            List<Trade> against = engine.submitOrder(sell(100, 6));
            assertEquals(6, totalQuantity(against));
        }
    }

    @Nested
    @DisplayName("priority")
    class Priority {

        @ParameterizedTest(name = "[{0}]")
        @EnumSource(EngineSolution.class)
        @DisplayName("best-priced resting order matches first (price priority)")
        void bestPriceMatchesFirst(EngineSolution solution) {
            MatchingEngine engine = solution.create();
            engine.submitOrder(sell(102, 10));
            Order best = sell(100, 10);
            engine.submitOrder(best);

            List<Trade> trades = engine.submitOrder(buy(102, 10));

            assertEquals(1, trades.size());
            assertEquals(100, trades.get(0).price());
            assertEquals(best.getId(), trades.get(0).sellOrderId());
        }

        @ParameterizedTest(name = "[{0}]")
        @EnumSource(EngineSolution.class)
        @DisplayName("at equal price, the earliest resting order matches first (time priority)")
        void earliestOrderMatchesFirstAtSamePrice(EngineSolution solution) {
            MatchingEngine engine = solution.create();
            Order early = buy(100, 10);
            Order late = buy(100, 10);
            engine.submitOrder(early);
            engine.submitOrder(late);

            Order incoming = sell(100, 10);
            List<Trade> trades = engine.submitOrder(incoming);

            assertEquals(1, trades.size());
            assertEquals(early.getId(), trades.get(0).buyOrderId());
            assertEquals(incoming.getId(), trades.get(0).sellOrderId());
        }
    }

    private static int totalQuantity(List<Trade> trades) {
        return trades.stream().mapToInt(Trade::quantity).sum();
    }
}
