package com.hazelcast.fcannizzohz;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomerServiceIntegrationTest {

    @Test
    void findCustomerNativeHz() {
        HazelcastInstance instance = Hazelcast.newHazelcastInstance();
        instance.getMap("customers").put("123", new Customer("123", "Alice"));
        HzCustomerService sut = new HzCustomerService(instance);
        assertEquals("Alice", sut.findCustomer("123").name());
    }

    @AfterEach
    public void after() {
        Hazelcast.shutdownAll();
    }
}