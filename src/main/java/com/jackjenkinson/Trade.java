package com.jackjenkinson;

public record Trade(long buyOrderId, long sellOrderId, int price, int quantity) {
}
