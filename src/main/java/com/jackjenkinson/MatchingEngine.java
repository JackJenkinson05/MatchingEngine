package com.jackjenkinson;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MatchingEngine {

    private TreeMap<Integer, LinkedList<Order>> bids = new TreeMap<>(Collections.reverseOrder());
    private TreeMap<Integer, LinkedList<Order>> asks = new TreeMap<>();
    private Map<Long, Order> ordersById = new HashMap<>();
    private long nextTimestamp = 0L;

    public List<Trade> submitOrder(Order order) {
        order.setTimestamp(nextTimestamp++);








        return List.of();
    }
}
