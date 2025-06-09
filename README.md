# Testing samples

Testing applications that use Hazelcast (IMDG and Streaming) requires care to validate the behaviour at various levels - from 
unit to system tests - given Hazelcast’s distributed, eventually consistent and asynchronous behaviour.

## Unit testing

The purpose of unit tests is to test individual components (e.g., services, classes, listeners, processors) in isolation, 
focusing on their functional correctness.

Developers have two primary options:

 - **Mock Hazelcast interfaces** using libraries like [Mockito](mockito.org)
 - Use **embedded Hazelcast instances** created in-memory via the test factory

### Mocking Hazelcast Interfaces

Mocking Hazelcast APIs allows you to isolate the class under test and control its dependencies.  The advantages of this approach are:

 - **Isolation**: the test only focuses on testing the logic of the class under test
 - **Speed**: it may be faster to run as it doesn't need Hazelcast to run
 - **Control**: it's easier to setup edge cases (null, exceptions). For example `when(map.get("404")).thenReturn(null)`

While useful, mocking Hazelcast interfaces should be done with care. It is considered an antipattern to mock external interfaces 
(see paragraph 4.1 of [this paper](http://jmock.org/oopsla2004.pdf)), especially interfaces you don’t own, because:

 - Brittleness: Tests may break when Hazelcast changes its API or behaviour
 - Blind spots: It skips real Hazelcast behaviour such as:
   - Serialization/deserialization
   - Key/value validations
   - TTLs, eviction policies
   - Event listeners, interceptors

An example of mocking Hazelcast interfaces is:
```java
    @Test
    void testFindCustomerWithMock() {
        when(hzInstance.getMap("customers")).thenReturn((IMap) customerMap);
        when(customerMap.get("123")).thenReturn(new Customer("123", "Alice"));
        assertEquals("Alice", service.findCustomer("123").name());
    }
```

In here Mockito is used to mock the behaviour of the Hazelcast instance and of the `"customer"` `ÌMap` to validate the behaviour of the `findCustomer()` interface method.

### Testing with embedded Hazelcast mock network

While not mocking per se, **embedded Hazelcast instances** are real, in-memory cluster nodes ideal for local testing. 
They enable realistic behaviour without requiring external setup.

> [!NOTE] Developers should use, in unit test scenarios, `TestHazelcastInstanceFactory` over `Hazelcast.newHazelcastInstance()`.
> The latter creates full Hazelcast nodes and may interfere with local networking (e.g., TCP/IP stack, port conflicts), making tests slower and possibly more brittle.

To use Hazelcast’s mock network test support, you must include the test dependency with the `tests` classifier:

```xml
    <dependency>
      <groupId>com.hazelcast</groupId>
      <artifactId>hazelcast</artifactId>
      <version>{hz.version}</version>
      <classifier>tests</classifier>
    </dependency>
```

For example:

```java
    @Test
    void testFindCustomerHzLightweight() {
        TestHazelcastInstanceFactory factory = new TestHazelcastInstanceFactory(1);
        HazelcastInstance instance = factory.newHazelcastInstance();
        instance.getMap("customers").put("123", new Customer("123", "Alice"));
        HzCustomerService sut = new HzCustomerService(instance);
        assertEquals("Alice", sut.findCustomer("123").name());
    }
```
or, with a multi node setup:

```java
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

```

This approach allows testing realistic behaviour in a fast and controlled environment, for single or multi node clusters.

The [Hazelcast docs](https://docs.hazelcast.com/hazelcast/5.5/test/testing) contain further details on how to setup the unit tests
and for examples to test streaming applications. Here we will focus on the basic utilization.

Specifically it refers to the use of `JetTestSupport`. This is a valid approach but it forces the inclusion of the following 
dependencies and the tests are JUnit4

```xml
    <dependency>
      <groupId>junit</groupId>
      <artifactId>junit</artifactId>
      <version>4.13.2</version>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.apache.logging.log4j</groupId>
      <artifactId>log4j-core</artifactId>
      <version>2.20.0</version> <!-- or whatever latest you want -->
    </dependency>
    <dependency>
      <groupId>org.apache.logging.log4j</groupId>
      <artifactId>log4j-api</artifactId>
      <version>2.20.0</version>
    </dependency>
```

## Integration testing

Integration tests are be used to test integration between components using Hazelcast, including 

 - distributed data structures;
 - event listeners, enrty processors, and custom de/serializers;
 - specific behavioural configuration in Hazelcast like eviction policies, discovery of nodes,  time-to-live;

These behaviours are highly dependent on Hazelcast configuration and runtime state, so tests should closely replicate 
clustered environments.

### Testing with embedded Hazelcast

A common approach is to spin up embedded Hazelcast instances within the same JVM. This allows validation of distributed behaviour 
without requiring an external cluster. For example:

```java
    @Test
    void testFindCustomerNativeHz() {
        HazelcastInstance instance = Hazelcast.newHazelcastInstance();
        instance.getMap("customers").put("123", new Customer("123", "Alice"));
        HzCustomerService sut = new HzCustomerService(instance);
        assertEquals("Alice", sut.findCustomer("123").name());
    }
```

This approach enables realistic testing of:

 - Actual serialization and custom serializers
 - Discovery configuration (e.g., TCP/IP, Multicast)
 - TTLs, eviction, map stores
 - Network communication and runtime issues (e.g., port conflicts)

While embedded Hazelcast mirrors more closely production configuration and settings, it introduces **overhead and instability 
risks** as it is slower to startup, may introduce brittleness due to network instability and port collisions in CI/CD environments.

Finally, to reduce resources leaks and flaky tests, it's recommended to always **shut down** Hazelcast instances after each test run:
```java
    @AfterEach 
    public void after() {
        Hazelcast.shutdownAll();
    }
```
