package com.hazelcast.fcannizzohz.junit5;

import com.hazelcast.config.Config;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.fcannizzohz.Customer;
import com.hazelcast.fcannizzohz.HzCustomerService;
import com.hazelcast.test.TestHazelcastInstanceFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.hazelcast.test.HazelcastTestSupport.randomName;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CustomerServiceWithSupportTest {
    private TestHazelcastInstanceFactory factory;

    @BeforeEach
    void setup() {
        factory = new TestHazelcastInstanceFactory();
    }

    @AfterEach
    void tearDown() {
        if (factory != null) {
            factory.shutdownAll();
        }
    }

    @Test
    public void findCustomerSingleNode() {
        HazelcastInstance instance = factory.newHazelcastInstance();
        instance.getMap("customers").put("123", new Customer("123", "Alice"));
        HzCustomerService sut = new HzCustomerService(instance);
        assertEquals("Alice", sut.findCustomer("123").name());
    }

    @Test
    public void findCustomerTwoNodes() {
        HazelcastInstance[] cluster = factory.newInstances(new Config().setClusterName(randomName()), 2);
        HazelcastInstance node1 = cluster[0];
        HazelcastInstance node2 = cluster[1];

        // data injected in node1
        node1.getMap("customers").put("123", new Customer("123", "Alice"));

        // data retrieved from node2
        HzCustomerService sut2 = new HzCustomerService(node2);
        assertEquals("Alice", sut2.findCustomer("123").name());
    }

}