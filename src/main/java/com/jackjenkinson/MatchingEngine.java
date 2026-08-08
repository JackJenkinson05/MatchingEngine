package com.jackjenkinson;

import java.util.List;

public interface MatchingEngine {
    List<Trade> submitOrder(Order order);
}
