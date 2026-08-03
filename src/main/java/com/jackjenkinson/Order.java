package com.jackjenkinson;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Order {
    private long id;
    private Side side;
    private int price;
    private int quantity;
    private long timestamp;

    public Order(long id, Side side, int price, int quantity) {
        this.id = id;
        this.side = side;
        this.price = price;
        this.quantity = quantity;
    }
}
