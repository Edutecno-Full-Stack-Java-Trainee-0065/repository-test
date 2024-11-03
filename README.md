# Guía Completa de Testing de Repositorios Spring Boot

## Índice
1. [Configuración del Proyecto](#1-configuración-del-proyecto)
2. [Modelo de Datos](#2-modelo-de-datos)
3. [Pruebas de Integración](#3-pruebas-de-integración)
4. [Ejecución y Verificación](#4-ejecución-y-verificación)
5. [Mejores Prácticas](#5-mejores-prácticas)

## 1. Configuración del Proyecto

### 1.1. Estructura de Archivos
```plaintext
src/
├── main/
│   ├── java/
│   │   └── cl/playground/repositorytest/
│   │       ├── model/
│   │       │   ├── Customer.java
│   │       │   ├── Order.java
│   │       │   └── OrderItem.java
│   │       └── repository/
│   │           └── OrderRepository.java
│   └── resources/
│       └── application.properties
└── test/
    └── java/
        └── cl/playground/repositorytest/repository/
            └── OrderRepositoryTest.java
```

### 1.2. Dependencias (pom.xml)
```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 1.3. Docker Compose (docker-compose.yml)
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

### 1.4. Configuración de Base de Datos (application.properties)
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/schooldb
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

## 2. Modelo de Datos

### 2.1. Entidades JPA

#### Customer.java
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

#### Order.java
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

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);
    }
}
```

#### OrderItem.java
```java
@Entity
@Table(name = "order_items")
@Data
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

    public void setOrder(Order order) {
        if (this.order != null) {
            this.order.getItems().remove(this);
        }
        this.order = order;
        if (order != null && !order.getItems().contains(this)) {
            order.getItems().add(this);
        }
    }
}
```

### 2.2. Configuraciones Importantes
- Relaciones bidireccionales
- Lazy loading
- Cascade operations
- Orphan removal

## 3. Pruebas de Integración

### 3.1. Tests de Consultas Personalizadas
- `shouldFindOrdersWithDetailsAfterDate`
- `shouldFindCustomerOrdersWithExpensiveItems`

### 3.2. Tests de Casos Límite
- `shouldReturnEmptyListWhenNoOrdersMatchCriteria`
- `shouldHandleCustomerWithNoExpensiveItems`

### 3.3. Tests de Performance
- `shouldNotGenerateNPlusOneQueries`

### 3.4. Tests CRUD
- `shouldCreateOrderSuccessfully`
- `shouldReadOrderSuccessfully`
- `shouldUpdateOrderSuccessfully`
- `shouldDeleteOrderSuccessfully`

### 3.5. Tests de Gestión de Items
- `shouldUpdateOrderItemSuccessfully`
- `shouldAddItemToExistingOrder`
- `shouldRemoveItemFromExistingOrder`

### 3.6. Tests de Conteo
- `shouldCountOrdersCorrectly`

### 3.7. Test de Documentación
- `documentTestStructure`

## 4. Ejecución y Verificación

### 4.1. Comandos Básicos
```bash
# Iniciar PostgreSQL
docker-compose up -d

# Verificar contenedor
docker-compose ps

# Ejecutar tests
./mvnw test
```

### 4.2. Verificación de Resultados
1. Consola de Tests
2. Logs SQL
3. Base de Datos

## 5. Mejores Prácticas

### 5.1. Gestión de Datos
- Limpieza entre tests
- Estado inicial conocido
- Transaccionalidad

### 5.2. Performance
- Uso de EntityManager
- Prevención N+1 queries
- Lazy loading

### 5.3. Estructura de Tests
- Patrón Given-When-Then
- Nombres descriptivos
- Verificaciones completas

### 5.4. Mantenibilidad
- Código documentado
- Tests independientes
- Reutilización de código

[Documentación detallada de cada test disponible en el documento original]