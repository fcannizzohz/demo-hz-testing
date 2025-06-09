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

### Testing with embedded Hazelcast

While not mocking per se, **embedded Hazelcast instances** are created **in-memory** and are the recommended way to simulate a full
cluster without external setup.

The most immediate way to test using embedded hazelcast is to create an instance of the server in the test itself. For example:

```java
    @Test
    void testFindCustomerNativeHz() {
        HazelcastInstance instance = Hazelcast.newHazelcastInstance();
        instance.getMap("customers").put("123", new Customer("123", "Alice"));
        HzCustomerService sut = new HzCustomerService(instance);
        assertEquals("Alice", sut.findCustomer("123").name());
    }
```

This method allows testing of the application logic in a realistic environment that realistically mirrors the production behaviour. 
It allows testing of actual serialization, configuration and discovery mechanisms, network comms and potential runtime issues like 
port conflicts and bootstrap failures. However it comes with some trade-offs: native instances are slower to start and may conflict 
with system ports and therefore  in CI/CD environments may introduce flakiness and brittleness. While valuable integration tools, 
they should be limited to testing only the network  configuration and bootstrap process and not leaked to test business logic. 

A more reliable mechanism to test business logic in using tests it to adopt the Hazelcast Mock network. 

> [!NOTE]
> To reduce brittleness introduced by spinning up real Hazelcast instances, it's recommended 
> to shutdown the instances after the end of each test, or as soon as desirable, using for example:
> 
> ```java
>     @AfterEach
>     public void after() {
>         Hazelcast.shutdownAll();
>     }
> ```
