package com.jackjenkinson;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class Order {
    private long id;
    private Side side;
    private int price;
    private int quantity;
    private long timestamp;
}
