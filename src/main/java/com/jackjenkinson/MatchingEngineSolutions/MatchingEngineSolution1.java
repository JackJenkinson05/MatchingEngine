package com.jackjenkinson.MatchingEngineSolutions;

import com.jackjenkinson.MatchingEngine;
import com.jackjenkinson.Order;
import com.jackjenkinson.Side;
import com.jackjenkinson.Trade;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MatchingEngineSolution1 implements MatchingEngine {

    private TreeMap<Integer, LinkedList<Order>> bids = new TreeMap<>(Collections.reverseOrder());
    private TreeMap<Integer, LinkedList<Order>> asks = new TreeMap<>();
    private Map<Long, Order> ordersById = new HashMap<>();
    private long nextTimestamp = 0L;

    public List<Trade> submitOrder(Order order) {
        order.setTimestamp(nextTimestamp++);

        addOrderToOrderMaps(order);

        return executeTrades();
    }

    private void addOrderToOrderMaps(Order order) {
        ordersById.put(order.getId(), order);
        TreeMap<Integer, LinkedList<Order>> book = order.getSide() == Side.BUY ? bids : asks;
        book.computeIfAbsent(order.getPrice(), p -> new LinkedList<>()).add(order);
    }

    private List<Trade> executeTrades() {

        if (bids.firstEntry() == null || asks.firstEntry() == null) {
            return List.of();
        }

        Integer largestBidPrice = bids.firstEntry().getKey();
        Integer smallestAskPrice = asks.firstEntry().getKey();


        if (smallestAskPrice > largestBidPrice) {
            return List.of();
        }

        List<Trade> trades = new LinkedList<>();

        while (smallestAskPrice != null && largestBidPrice != null && smallestAskPrice <= largestBidPrice) {

            Order largestBid = bids.firstEntry().getValue().getFirst();
            Order smallestAsk = asks.firstEntry().getValue().getFirst();

            trades.add(tradeBidAndAsk(largestBid, smallestAsk));

            largestBidPrice = bids.firstEntry() != null ? bids.firstEntry().getKey() : null;
            smallestAskPrice = asks.firstEntry() != null ? asks.firstEntry().getKey() : null;
        }

        return trades;
    }

    private Trade tradeBidAndAsk(Order bidOrder, Order askOrder) {

        if (bidOrder.getQuantity() == askOrder.getQuantity()) {

            bids.get(bidOrder.getPrice()).removeFirst();
            asks.get(askOrder.getPrice()).removeFirst();

            if (bids.get(bidOrder.getPrice()).isEmpty()) {
                bids.remove(bidOrder.getPrice());
            }

            if (asks.get(askOrder.getPrice()).isEmpty()) {
                asks.remove(askOrder.getPrice());
            }

            return new Trade(bidOrder.getId(), askOrder.getId(), askOrder.getPrice(), askOrder.getQuantity());
        } else if (bidOrder.getQuantity() > askOrder.getQuantity()) {

            asks.get(askOrder.getPrice()).removeFirst();
            bids.get(bidOrder.getPrice()).getFirst().setQuantity(bidOrder.getQuantity() - askOrder.getQuantity());

            if (asks.get(askOrder.getPrice()).isEmpty()) {
                asks.remove(askOrder.getPrice());
            }

            return new Trade(bidOrder.getId(), askOrder.getId(), askOrder.getPrice(), askOrder.getQuantity());
        } else {

            bids.get(bidOrder.getPrice()).removeFirst();
            asks.get(askOrder.getPrice()).getFirst().setQuantity(askOrder.getQuantity() - bidOrder.getQuantity());

            if (bids.get(bidOrder.getPrice()).isEmpty()) {
                bids.remove(bidOrder.getPrice());
            }

            return new Trade(bidOrder.getId(), askOrder.getId(), askOrder.getPrice(), bidOrder.getQuantity());
        }
    }
}
