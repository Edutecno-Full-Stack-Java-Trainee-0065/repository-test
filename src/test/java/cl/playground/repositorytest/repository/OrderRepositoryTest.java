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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
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
}