package cl.playground.repositorytest.repository;

import cl.playground.repositorytest.model.Customer;
import cl.playground.repositorytest.model.Order;
import cl.playground.repositorytest.model.OrderItem;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional()
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setup() {
        // Limpiar datos anteriores
        orderRepository.deleteAll();

        // Preparar datos de prueba
        Customer customer = new Customer();
        customer.setName("John Doe");
        customer.setEmail("john@example.com");
        entityManager.persist(customer);

        Order order = new Order();
        order.setOrderDate(LocalDateTime.now());
        order.setCustomer(customer);

        OrderItem item1 = new OrderItem();
        item1.setProductName("Laptop");
        item1.setPrice(new BigDecimal("1200.00"));
        item1.setQuantity(1);
        item1.setOrder(order);

        OrderItem item2 = new OrderItem();
        item2.setProductName("Mouse");
        item2.setPrice(new BigDecimal("20.00"));
        item2.setQuantity(1);
        item2.setOrder(order);

        order.getItems().add(item1);
        order.getItems().add(item2);

        orderRepository.save(order);

        // Flush para asegurar que los datos están en la BD
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void shouldFindOrdersWithDetailsAfterDate() {
        // Given
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);

        // When
        List<Order> orders = orderRepository.findOrdersWithDetailsAfterDate(yesterday);

        // Then
        assertFalse(orders.isEmpty());
        Order order = orders.get(0);

        // Verificar que los datos relacionados están cargados
        assertNotNull(order.getCustomer());
        assertFalse(order.getItems().isEmpty());
        assertEquals(2, order.getItems().size());

        // Verificar los detalles
        assertEquals("John Doe", order.getCustomer().getName());
        assertTrue(order.getItems().stream()
                .anyMatch(item -> item.getProductName().equals("Laptop")));
    }

    @Test
    void shouldFindCustomerOrdersWithExpensiveItems() {
        // Given
        String customerEmail = "john@example.com";
        BigDecimal minPrice = new BigDecimal("1000.00");

        // When
        List<Order> orders = orderRepository.findCustomerOrdersWithExpensiveItems(
                customerEmail, minPrice);

        // Then
        assertFalse(orders.isEmpty());
        Order order = orders.get(0);

        // Verificar que solo se encuentran items caros
        assertTrue(order.getItems().stream()
                .anyMatch(item -> item.getPrice().compareTo(minPrice) > 0));

        // Verificar que el cliente es correcto
        assertEquals(customerEmail, order.getCustomer().getEmail());
    }

    @Test
    void shouldReturnEmptyListWhenNoOrdersMatchCriteria() {
        // Given
        LocalDateTime futureDate = LocalDateTime.now().plusDays(1);

        // When
        List<Order> orders = orderRepository.findOrdersWithDetailsAfterDate(futureDate);

        // Then
        assertTrue(orders.isEmpty());
    }

    @Test
    void shouldHandleCustomerWithNoExpensiveItems() {
        // Given
        BigDecimal veryHighPrice = new BigDecimal("9999999.00");

        // When
        List<Order> orders = orderRepository.findCustomerOrdersWithExpensiveItems(
                "john@example.com", veryHighPrice);

        // Then
        assertTrue(orders.isEmpty());
    }

    @Test
    void shouldNotGenerateNPlusOneQueries() {
        // Given
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);

        // When
        List<Order> orders = orderRepository.findOrdersWithDetailsAfterDate(yesterday);

        // Then
        // Verificar acceso a datos relacionados sin queries adicionales
        orders.forEach(order -> {
            order.getCustomer().getName();  // No debería generar query adicional
            order.getItems().size();        // No debería generar query adicional
        });
    }

    @Test
    void shouldCreateOrderSuccessfully() {
        // Given
        Customer customer = new Customer();
        customer.setName("Jane Doe");
        customer.setEmail("jane@example.com");
        entityManager.persist(customer);

        Order newOrder = new Order();
        newOrder.setOrderDate(LocalDateTime.now());
        newOrder.setCustomer(customer);

        OrderItem newItem = new OrderItem();
        newItem.setProductName("Tablet");
        newItem.setPrice(new BigDecimal("500.00"));
        newItem.setQuantity(1);
        newItem.setOrder(newOrder);

        newOrder.getItems().add(newItem);

        // When
        Order savedOrder = orderRepository.save(newOrder);
        entityManager.flush();
        entityManager.clear();

        // Then
        Order foundOrder = orderRepository.findById(savedOrder.getId()).orElse(null);
        assertNotNull(foundOrder);
        assertEquals("Jane Doe", foundOrder.getCustomer().getName());
        assertEquals(1, foundOrder.getItems().size());
        assertEquals("Tablet", foundOrder.getItems().get(0).getProductName());
    }

    @Test
    void shouldReadOrderSuccessfully() {
        // Given - Ya tenemos datos del setup()
        Order existingOrder = orderRepository.findAll().get(0);

        // When
        Order foundOrder = orderRepository.findById(existingOrder.getId()).orElse(null);

        // Then
        assertNotNull(foundOrder);
        assertEquals("John Doe", foundOrder.getCustomer().getName());
        assertEquals(2, foundOrder.getItems().size());
    }

    @Test
    void shouldUpdateOrderSuccessfully() {
        // Given
        Order existingOrder = orderRepository.findAll().get(0);
        LocalDateTime newDate = LocalDateTime.now().plusDays(1);

        // When
        existingOrder.setOrderDate(newDate);
        orderRepository.save(existingOrder);
        entityManager.flush();
        entityManager.clear();

        // Then
        Order updatedOrder = orderRepository.findById(existingOrder.getId()).orElse(null);
        assertNotNull(updatedOrder);
        assertEquals(newDate.truncatedTo(ChronoUnit.SECONDS),
                updatedOrder.getOrderDate().truncatedTo(ChronoUnit.SECONDS));
    }

    @Test
    void shouldDeleteOrderSuccessfully() {
        // Given
        Order existingOrder = orderRepository.findAll().get(0);
        Long orderId = existingOrder.getId();

        // When
        orderRepository.deleteById(orderId);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<Order> deletedOrder = orderRepository.findById(orderId);
        assertTrue(deletedOrder.isEmpty());
    }

    @Test
    void shouldUpdateOrderItemSuccessfully() {
        // Given
        Order existingOrder = orderRepository.findAll().get(0);
        OrderItem itemToUpdate = existingOrder.getItems().stream()
                .filter(item -> item.getProductName().equals("Laptop"))
                .findFirst()
                .orElseThrow();
        BigDecimal newPrice = new BigDecimal("1500.00");

        // When
        itemToUpdate.setPrice(newPrice);
        orderRepository.save(existingOrder);
        entityManager.flush();
        entityManager.clear();

        // Then
        Order updatedOrder = orderRepository.findById(existingOrder.getId()).orElse(null);
        assertNotNull(updatedOrder);

        OrderItem updatedItem = updatedOrder.getItems().stream()
                .filter(item -> item.getProductName().equals("Laptop"))
                .findFirst()
                .orElseThrow();
        assertEquals(newPrice, updatedItem.getPrice());
    }

    @Test
    void shouldAddItemToExistingOrder() {
        // Given
        Order existingOrder = orderRepository.findAll().get(0);
        int initialItemCount = existingOrder.getItems().size();

        OrderItem newItem = new OrderItem();
        newItem.setProductName("Keyboard");
        newItem.setPrice(new BigDecimal("50.00"));
        newItem.setQuantity(1);
        newItem.setOrder(existingOrder);

        // When
        existingOrder.getItems().add(newItem);
        orderRepository.save(existingOrder);
        entityManager.flush();
        entityManager.clear();

        // Then
        Order updatedOrder = orderRepository.findById(existingOrder.getId()).orElse(null);
        assertNotNull(updatedOrder);
        assertEquals(initialItemCount + 1, updatedOrder.getItems().size());
        assertTrue(updatedOrder.getItems().stream()
                .anyMatch(item -> item.getProductName().equals("Keyboard")));
    }

    @Test
    void shouldRemoveItemFromExistingOrder() {
        // Given
        Order existingOrder = orderRepository.findAll().get(0);
        int initialItemCount = existingOrder.getItems().size();
        OrderItem itemToRemove = existingOrder.getItems().stream()
                .filter(item -> item.getProductName().equals("Laptop"))
                .findFirst()
                .orElseThrow();

        // When
        existingOrder.getItems().remove(itemToRemove);
        orderRepository.save(existingOrder);
        entityManager.flush();
        entityManager.clear();

        // Then
        Order updatedOrder = orderRepository.findById(existingOrder.getId()).orElse(null);
        assertNotNull(updatedOrder);
        assertEquals(initialItemCount - 1, updatedOrder.getItems().size());
        assertTrue(updatedOrder.getItems().stream()
                .noneMatch(item -> item.getProductName().equals("Laptop")));
    }

    @Test
    void shouldCountOrdersCorrectly() {
        // Given - Ya tenemos un orden del setup()

        // When
        long count = orderRepository.count();

        // Then
        assertEquals(1, count);

        // Agregar otra orden
        Customer customer = new Customer();
        customer.setName("New Customer");
        customer.setEmail("new@example.com");
        entityManager.persist(customer);

        Order newOrder = new Order();
        newOrder.setOrderDate(LocalDateTime.now());
        newOrder.setCustomer(customer);
        orderRepository.save(newOrder);

        entityManager.flush();

        // Verificar nuevo conteo
        assertEquals(2, orderRepository.count());
    }

    @Test
    void documentTestStructure() throws InterruptedException {
        // Given - Preparación de datos
        System.out.println("\n=== Estado Inicial ===");
        System.out.println("Clientes: " + entityManager
                .createQuery("SELECT COUNT(c) FROM Customer c").getSingleResult());
        System.out.println("Órdenes: " + entityManager
                .createQuery("SELECT COUNT(o) FROM Order o").getSingleResult());
        System.out.println("Items: " + entityManager
                .createQuery("SELECT COUNT(i) FROM OrderItem i").getSingleResult());

        // Pausa para verificar BD
        System.out.println("\nPausa de 15 segundos para verificar estado inicial...");
        Thread.sleep(15000);

        // When - Ejecutar algunas operaciones
        Order newOrder = createTestOrder();
        orderRepository.save(newOrder);
        entityManager.flush();

        System.out.println("\n=== Después de Crear Nueva Orden ===");
        System.out.println("Órdenes: " + entityManager
                .createQuery("SELECT COUNT(o) FROM Order o").getSingleResult());

        // Pausa para verificar cambios
        System.out.println("\nPausa de 15 segundos para verificar cambios...");
        Thread.sleep(15000);

        // Then - Verificar y limpiar
        orderRepository.deleteById(newOrder.getId());
        entityManager.flush();

        System.out.println("\n=== Estado Final ===");
        System.out.println("Órdenes: " + entityManager
                .createQuery("SELECT COUNT(o) FROM Order o").getSingleResult());

        // Pausa final
        System.out.println("\nPausa final de 15 segundos...");
        Thread.sleep(15000);
    }

    private Order createTestOrder() {
        Customer customer = new Customer();
        customer.setName("Test Customer");
        customer.setEmail("test@example.com");
        entityManager.persist(customer);

        Order order = new Order();
        order.setOrderDate(LocalDateTime.now());
        order.setCustomer(customer);

        OrderItem item = new OrderItem();
        item.setProductName("Test Product");
        item.setPrice(new BigDecimal("100.00"));
        item.setQuantity(1);
        item.setOrder(order);

        order.getItems().add(item);
        return order;
    }
}