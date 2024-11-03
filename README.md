# Tutorial: Pruebas de Integración para Repositorios Spring Boot

Este tutorial explica paso a paso cómo realizar pruebas de integración para la capa de repositorios en Spring Boot, usando PostgreSQL en Docker.

## 1. Configuración Inicial

### 1.1 Dependencias (pom.xml)
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <!-- ... otras configuraciones ... -->
    <dependencies>
        <!-- Spring Data JPA: Proporciona la integración con JPA -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- PostgreSQL: Driver para conectar con la base de datos -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Lombok: Reduce el código repetitivo -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

### 1.2 Base de Datos (docker-compose.yml)
```yaml
version: '3.8'
services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: schooldb
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

### 1.3 Configuración de Spring (application.properties)
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/schooldb
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

## 2. Modelo de Datos

### 2.1 Customer.java
```java
@Data
@Entity
@Table(name = "customers")
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private String email;
    
    @OneToMany(mappedBy = "customer")
    private List<Order> orders = new ArrayList<>();
}
```
- `@Entity`: Marca la clase como una entidad JPA
- `@Table`: Especifica el nombre de la tabla en la BD
- `@OneToMany`: Define la relación uno a muchos con Order

### 2.2 Order.java
```java
@Data
@Entity
@Table(name = "orders")
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "order_date")
    private LocalDateTime orderDate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items = new ArrayList<>();
}
```
- `@ManyToOne`: Define la relación muchos a uno con Customer
- `fetch = FetchType.LAZY`: Optimización de rendimiento, carga bajo demanda
- `cascade = CascadeType.ALL`: Propaga operaciones a los items

### 2.3 OrderItem.java
```java
@Data
@Entity
@Table(name = "order_items")
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String productName;
    private BigDecimal price;
    private Integer quantity;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;
}
```

## 3. Repositorio

```java
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    @Query("SELECT o FROM Order o " +
           "LEFT JOIN FETCH o.customer " +
           "LEFT JOIN FETCH o.items " +
           "WHERE o.orderDate >= :startDate")
    List<Order> findOrdersWithDetailsAfterDate(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT DISTINCT o FROM Order o " +
           "JOIN FETCH o.customer c " +
           "JOIN FETCH o.items i " +
           "WHERE c.email = :email " +
           "AND i.price > :minPrice")
    List<Order> findCustomerOrdersWithExpensiveItems(
            @Param("email") String email,
            @Param("minPrice") BigDecimal minPrice);
}
```

**Explicación de las queries:**
1. `findOrdersWithDetailsAfterDate`:
    - Usa LEFT JOIN FETCH para traer datos relacionados
    - Evita el problema N+1 queries
    - Filtra por fecha

2. `findCustomerOrdersWithExpensiveItems`:
    - Usa JOIN FETCH para datos relacionados
    - DISTINCT evita duplicados
    - Filtra por email y precio

## 4. Pruebas de Integración

```java
@SpringBootTest              // Carga el contexto completo de Spring
@Transactional              // Hace rollback después de cada test
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EntityManager entityManager;  // Para control fino de persistencia

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
        // Clear para limpiar el contexto de persistencia
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
    void shouldHandleCustomerWithNoExpensiveItems() {
        // Given
        BigDecimal veryHighPrice = new BigDecimal("9999999.00");

        // When
        List<Order> orders = orderRepository.findCustomerOrdersWithExpensiveItems(
                "john@example.com", veryHighPrice);

        // Then
        assertTrue(orders.isEmpty());
    }
}
```

## 5. Ejecución y Verificación

### 5.1 Iniciar la Base de Datos
```bash
# Iniciar PostgreSQL
docker-compose up -d

# Verificar que está corriendo
docker-compose ps
```

### 5.2 Ejecutar Pruebas
```bash
# Ejecutar todas las pruebas
./mvnw test

# Ejecutar una clase específica
./mvnw test -Dtest=OrderRepositoryTest
```

### 5.3 Verificar Resultados

1. **Consola de Tests**: Muestra resultados de las pruebas

2. **Logs SQL**: Con `spring.jpa.show-sql=true` podemos ver:
    - Queries generadas
    - Orden de ejecución
    - Problemas de rendimiento

3. **Verificaciones Importantes**:
    - Los tests pasan (verde)
    - Las queries SQL son eficientes
    - No hay problemas N+1
    - Las relaciones se cargan correctamente

### 5.4 Herramientas Útiles

1. **PgAdmin o DBeaver**:
    - Conectar a localhost:5432
    - Examinar las tablas creadas
    - Verificar datos de prueba

2. **Logs de Spring**:
    - Ver queries SQL
    - Detectar problemas de rendimiento

## Resumen

1. **¿Por qué EntityManager?**
    - Control preciso de la persistencia
    - Forzar sincronización con la BD
    - Limpiar caché de primer nivel

2. **¿Por qué @Transactional?**
    - Rollback automático después de cada test
    - Consistencia entre pruebas
    - Aislamiento de pruebas

3. **¿Por qué JOIN FETCH?**
    - Evitar problema N+1 queries
    - Mejorar rendimiento
    - Cargar relaciones eficientemente

4. **¿Por qué estos tests?**
    - Prueban funcionalidad real
    - Verifican mapeo JPA
    - Validan queries SQL
    - Comprueban relaciones