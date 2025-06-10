package com.hazelcast.fcannizzohz;

public interface OrderService {
    void placeOrder(Order order);

    Order getOrder(String id);

    void updateOrder(Order order);
}
