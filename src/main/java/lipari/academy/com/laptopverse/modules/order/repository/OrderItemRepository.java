package lipari.academy.com.laptopverse.modules.order.repository;

import lipari.academy.com.laptopverse.modules.order.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
}
