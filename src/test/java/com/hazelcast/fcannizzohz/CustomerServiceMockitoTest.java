package com.hazelcast.fcannizzohz;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CustomerServiceMockitoTest {

    @Mock
    IMap<String, Customer> customerMap;

    @Mock
    HazelcastInstance hzInstance;

    @InjectMocks
    HzCustomerService service;

    @Test
    public void findCustomerWithMock() {
        //noinspection unchecked,rawtypes
        when(hzInstance.getMap("customers")).thenReturn((IMap) customerMap);
        when(customerMap.get("123")).thenReturn(new Customer("123", "Alice"));

        assertEquals("Alice", service.findCustomer("123").name());
    }
}
