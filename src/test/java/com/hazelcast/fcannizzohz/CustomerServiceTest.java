package com.hazelcast.fcannizzohz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {
    @Mock
    IMap<String, Customer> customerMap;
    @Mock
    HazelcastInstance hzInstance;

    @InjectMocks
    CustomerServiceImpl service;

    @Test
    void testFindCustomerWithMock() {
        //noinspection unchecked,rawtypes
        when(hzInstance.getMap("customers")).thenReturn((IMap) customerMap);
        when(customerMap.get("123")).thenReturn(new Customer("123", "Alice"));
        assertEquals("Alice", service.findCustomer("123").name());
    }

    @Test
    void testFindCustomer() {


    }
}