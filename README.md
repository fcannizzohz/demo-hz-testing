# Testing samples

Testing applications that use Hazelcast (IMDG and Streaming) requires care to validate the behaviour at various levels - from 
unit to system tests - given Hazelcast’s distributed, eventually consistent and asynchronous behaviour.

## Mocking Hazelcast Interfaces

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

## Testing with embedded Hazelcast mock network

While not mocking per se, **embedded Hazelcast instances** are real, in-memory cluster nodes ideal for local testing. 
They enable realistic behaviour without requiring external setup.

Unless for specific reasons regarding testing of network deployments developers should use, `HazelcastTestSupport` over
running fully fledged instances started with `Hazelcast.newHazelcastInstance()`.
The latter creates full Hazelcast nodes and may interfere with local networking (e.g., TCP/IP stack, port conflicts), making 
tests slower and possibly more brittle.

### Setting up the Hazelcast Test support

The `HazelcastTestSupport` requires following dependency with the `tests` classifier:

```xml
    <dependency>
      <groupId>com.hazelcast</groupId>
      <artifactId>hazelcast</artifactId>
      <version>{hz.version}</version>
      <classifier>tests</classifier>
    </dependency>
```
as well as the use of JUnit 4 and OpenTest4J:

```xml
    <dependency>
      <groupId>junit</groupId>
      <artifactId>junit</artifactId>
      <version>{junit4.version}</version>
      <scope>test</scope>
    </dependency>
   <dependency>
      <groupId>org.opentest4j</groupId>
      <artifactId>opentest4j</artifactId>
      <version>{opentest4j.version}</version>
      <scope>test</scope>
   </dependency>
```

### Using `HazelcastTestSupport`

A test class wanting to use `HazelcastTestSupport` must extend it, for example:

```java
import com.hazelcast.test.HazelcastTestSupport;

class CustomerServiceWithSupportTest extends HazelcastTestSupport {
}
```

By doing so, it has access to a variety of utility methods for reuse, including `createHazelcastInstance()`.
For example:

```java
    @Test
    public void findCustomerSingleNode() {
        HazelcastInstance instance = createHazelcastInstance();
        instance.getMap("customers").put("123", new Customer("123", "Alice"));
        HzCustomerService sut = new HzCustomerService(instance);
        assertEquals("Alice", sut.findCustomer("123").name());
    }
```
or, with a multi node setup:

```java
    @Test
    public void findCustomerTwoNodes() {
        HazelcastInstance[] instances = createHazelcastInstances(2);
        HazelcastInstance node1 = instances[0];
        HazelcastInstance node2 = instances[1];
   
        // data injected in node1
        node1.getMap("customers").put("123", new Customer("123", "Alice"));
   
        // data retrieved from node2
        HzCustomerService sut2 = new HzCustomerService(node2);
        assertEquals("Alice", sut2.findCustomer("123").name());
    }
```

> **NOTE**: When creating instances with `createHazelcastInstances()` it's best practice to shutdown them at the end of each test to
> free up resources and prevent test brittleness:
> ```java
> @After
> public void tearDown() {
>    instance.shutdown();
> }
> ```

### Testing complex test scenarios

This approach allows testing realistic behaviour in a fast and controlled environment. 

#### Testing the integration of two services

In the following example, two services (`Customer`and `Order` share state via Hazelcast.) Functionality can be tested as following:

```java
    @Test
    public void testCustomerAndOrderServicesIntegration() {
        // Create a shared Hazelcast instance
        instance = createHazelcastInstance();

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
```

### Testing a component integration with its dependencies

Another typical scenario consist of testing the integration of a component, in isolation, but integrated with its dependencies:

```java
    @Test
    public void customerServiceWithMapStoreInteractions()
            throws Exception {
        // Set up H2 in-memory DB
        Connection conn = DriverManager.getConnection("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        conn.createStatement().execute("CREATE TABLE customers (id VARCHAR PRIMARY KEY, name VARCHAR)");

        // Set up Hazelcast config with MapStore
        Config config = new Config();
        config.getMapConfig("customers").getMapStoreConfig().setEnabled(true).setImplementation(new CustomerMapStore(conn));

        hz = createHazelcastInstance(config);
        CustomerService service = new HzCustomerService(hz);

        // Act
        service.save(new Customer("c1", "Alice")); // should go into both IMap and DB
        Customer fromMap = service.findCustomer("c1");      // should be from IMap

        // Clear IMap to test reloading from DB
        hz.getMap("customers").evictAll();
        Customer fromStore = service.findCustomer("c1");    // should be reloaded from H2

        // Assert
        assertEquals("Alice", fromMap.name());
        assertEquals("Alice", fromStore.name());
    }

```

### Testing integrated behaviour

`HazelcastTestSupport` supports testing of the application using the Hazelcast capabilities, for example, in this case, the 
execution of a listener:

```java
    @Test
    public void testOrderServiceListener() throws Exception {
        instance = createHazelcastInstance();
        // set a customer
        instance.getMap("customers").put("c1", new Customer("c1", "Alice"));

        OrderService sut = new HzOrderService(instance, mockConsumer);

        Order order = new Order("o1", "c1", "Laptop");
        sut.placeOrder(order);
        // Update the order so hazelcast triggers the event
        sut.updateOrder(order.confirm());
        
        verify(mockConsumer, timeout(100).only()).accept(any(Order.class));
    }
```

### Testing streaming applications

Hazelcast releases also support for testing streaming applications. This is done extending `JetTestSupport` (itself an extension 
of `HazelcastTestSupport`). The [Hazelcast docs](https://docs.hazelcast.com/hazelcast/5.5/test/testing) provide further details.

To use `JetTestSupport` the following dependencies must be included:

```xml
    <dependency>
      <groupId>org.apache.logging.log4j</groupId>
      <artifactId>log4j-core</artifactId>
      <version>{log4j.version}</version> <!-- or whatever latest you want -->
    </dependency>
    <dependency>
      <groupId>org.apache.logging.log4j</groupId>
      <artifactId>log4j-api</artifactId>
      <version>{log4j.version}</version>
    </dependency>
```

