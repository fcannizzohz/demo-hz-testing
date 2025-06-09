package com.hazelcast.fcannizzohz;

import com.hazelcast.core.HazelcastInstance;
//import com.hazelcast.test.TestHazelcastInstanceFactory;
import com.hazelcast.jet.core.JetTestSupport;
import com.hazelcast.test.TestHazelcastInstanceFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class CustomerServiceMockNetworkTest
{

    @Test
    void findCustomerSingleNode() {
        TestHazelcastInstanceFactory factory = new TestHazelcastInstanceFactory(1);
        HazelcastInstance instance = factory.newHazelcastInstance();
        instance.getMap("customers").put("123", new Customer("123", "Alice"));
        HzCustomerService sut = new HzCustomerService(instance);
        assertEquals("Alice", sut.findCustomer("123").name());
    }

    @Test
    void findCustomerTwoNodes() {
        TestHazelcastInstanceFactory factory = new TestHazelcastInstanceFactory(2);
        HazelcastInstance node1 = factory.newHazelcastInstance();
        HazelcastInstance node2 = factory.newHazelcastInstance();

        // data injected in node1
        node1.getMap("customers").put("123", new Customer("123", "Alice"));

        // data retrieved from node2
        HzCustomerService sut2 = new HzCustomerService(node2);
        assertEquals("Alice", sut2.findCustomer("123").name());
    }

}