package com.hazelcast.fcannizzohz;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.map.listener.EntryExpiredListener;

import java.io.Serializable;
import java.util.function.Consumer;

class OrderExpiredListener
        implements EntryExpiredListener<String, Order>, Serializable {

    private final Consumer<Order> listener;

    OrderExpiredListener(Consumer<Order> listener) {
        this.listener = listener;
    }

    @Override
    public void entryExpired(EntryEvent<String, Order> event) {
        listener.accept(event.getValue());
    }
}

public class HzOrderService
        implements OrderService {
    private final HazelcastInstance instance;

    public HzOrderService(HazelcastInstance hz) {
        this(hz, null);
    }
    public HzOrderService(HazelcastInstance hz, Consumer<Order> onExpiryCallback) {
        this.instance = hz;
        if (onExpiryCallback != null) {
            orderMap().addEntryListener(new OrderExpiredListener(onExpiryCallback), true);
        }
    }

    private IMap<String, Order> orderMap() {
        return instance.getMap("orders");
    }

    private IMap<String, Customer> customerMap() {
        return instance.getMap("customers");
    }

    @Override
    public void placeOrder(Order order) {
        // Enrich or validate order using shared customer state
        Customer customer = customerMap().get(order.customerId());
        if (customer == null) {
            throw new IllegalStateException("Customer does not exist: " + order.customerId());
        }
        orderMap().put(order.id(), order); // store order if valid
    }

    @Override
    public Order getOrder(String id) {
        return orderMap().get(id);
    }

}

