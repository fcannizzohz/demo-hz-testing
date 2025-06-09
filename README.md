# Testing samples

Testing applications that use Hazelcast (IMDG and Streaming) requires care to validate the behaviour at various levels - from 
unit to system tests - given Hazelcast’s distributed, eventually consistent and asynchronous behaviour.

## Unit testing

The purpose of unit tests is to test individual components (eg. services, classes, listeners, processors) in isolation for functionality.

Developers can choose to:

 - **Mock Hazelcast interfaces** using mocking libraries like [Mockito](mockito.org) 
 - Use **embedded Hazelcast** for lightweight testing when mocks are insufficient (e.g., verifying query predicates or listeners).

### Mocking Hazelcast Interfaces

The advantages of this approach are 

 - **Isolation**: the test only focuses on testing the logic of the class under test
 - **Speed**: it may be faster to run as it doesn't need Hazelcast to run
 - **Control**: it's easier to setup edge cases (null, exceptions). For example `when(map.get("404")).thenReturn(null)`

However this approach should be adopted with care; it is a common antipattern to mock external interfaces (see paragraph 4.1 [here](http://jmock.org/oopsla2004.pdf) for an explanation), and in general
interfaces that one doesn't own, because:

- it makes tests and mock depend on an external interface that may change in the future making the test brittle
- hides integration problems by skipping validation and key/value serialization, or bypassing behaviour implicitly executed by Hazelcast, like eviction.

An example of mocking Hazelcast interfaces is [here](https://github.com/fcannizzohz/testsamples/blob/27136bd40d7d95d1c5493a72b54e265f8dcb290e/src/test/java/com/hazelcast/fcannizzohz/CustomerServiceTest.java#L29):
```java
    @Test
    void testFindCustomerWithMock() {
        when(hzInstance.getMap("customers")).thenReturn((IMap) customerMap);
        when(customerMap.get("123")).thenReturn(new Customer("123", "Alice"));
        assertEquals("Alice", service.findCustomer("123").name());
    }
```
