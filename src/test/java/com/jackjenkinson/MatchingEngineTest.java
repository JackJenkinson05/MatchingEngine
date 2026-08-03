package com.jackjenkinson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MatchingEngineTest {

    private MatchingEngine engine;
    private long nextOrderId;

    @BeforeEach
    void setUp() {
        engine = new MatchingEngine();
        nextOrderId = 1L;
    }

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

        @Test
        @DisplayName("a lone order produces no trades")
        void loneOrderProducesNoTrades() {
            // given when
            List<Trade> trades = engine.submitOrder(buy(100, 10));

            //then
            assertTrue(trades.isEmpty());
        }

        @Test
        @DisplayName("non-crossing bid and ask do not match")
        void nonCrossingOrdersDoNotMatch() {
            //given
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

        @Test
        @DisplayName("exact match fully fills both orders at the resting price")
        void exactMatch() {
            //given
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

        @Test
        @DisplayName("crossing order trades at the resting order's price")
        void tradesAtRestingPrice() {
            //given
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

        @Test
        @DisplayName("incoming order partially fills a larger resting order")
        void incomingPartiallyFillsResting() {
            //given
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

        @Test
        @DisplayName("large incoming order is filled across multiple resting orders")
        void sweepsMultipleRestingOrders() {
            //given
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

        @Test
        @DisplayName("incoming order rests after consuming all available liquidity")
        void restsRemainderAfterPartialSweep() {
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

        @Test
        @DisplayName("best-priced resting order matches first (price priority)")
        void bestPriceMatchesFirst() {
            engine.submitOrder(sell(102, 10));
            Order best = sell(100, 10);
            engine.submitOrder(best);

            List<Trade> trades = engine.submitOrder(buy(102, 10));

            assertEquals(1, trades.size());
            assertEquals(100, trades.get(0).price());
            assertEquals(best.getId(), trades.get(0).sellOrderId());
        }

        @Test
        @DisplayName("at equal price, the earliest resting order matches first (time priority)")
        void earliestOrderMatchesFirstAtSamePrice() {
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
