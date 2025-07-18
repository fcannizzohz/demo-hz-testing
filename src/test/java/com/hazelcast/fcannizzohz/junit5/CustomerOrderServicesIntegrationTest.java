package com.hazelcast.fcannizzohz.junit5;

import com.hazelcast.client.test.TestHazelcastFactory;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.fcannizzohz.Customer;
import com.hazelcast.fcannizzohz.CustomerService;
import com.hazelcast.fcannizzohz.HzCustomerService;
import com.hazelcast.fcannizzohz.HzOrderService;
import com.hazelcast.fcannizzohz.Order;
import com.hazelcast.fcannizzohz.OrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CustomerOrderServicesIntegrationTest {

    private TestHazelcastFactory factory;

    @BeforeEach
    public void setUp() {
        factory = new TestHazelcastFactory();
    }

    @AfterEach
    public void tearDown() {
        if (factory != null) {
            factory.shutdownAll();
        }
    }

    @Test
    public void customerAndOrderServicesIntegration() {
        // Create a shared Hazelcast instance
        HazelcastInstance instance = factory.newHazelcastInstance();

        // Instantiate both services using same cluster
        CustomerService customerService = new HzCustomerService(instance);
        OrderService orderService = new HzOrderService(instance);

        // Add customer
        Customer alice = new Customer("c1", "Alice");
        customerService.save(alice);

        // Place an order for Alice
        Order order = new Order("o1", "c1", "Laptop");
        orderService.placeOrder(order);

        // Verify state across services
        assertEquals("Alice", customerService.findCustomer("c1").name());
        assertEquals("Laptop", orderService.getOrder("o1").product());
    }
}