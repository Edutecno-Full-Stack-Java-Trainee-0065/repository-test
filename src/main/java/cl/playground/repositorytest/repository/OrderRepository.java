package cl.playground.repositorytest.repository;

import cl.playground.repositorytest.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
