package tech.iraelie.practice.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.iraelie.practice.order.model.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
}
