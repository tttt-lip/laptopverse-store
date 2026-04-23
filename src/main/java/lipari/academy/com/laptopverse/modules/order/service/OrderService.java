package lipari.academy.com.laptopverse.modules.order.service;

import lipari.academy.com.laptopverse.modules.order.dto.OrderResponseDTO;
import lipari.academy.com.laptopverse.modules.order.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface OrderService {
    OrderResponseDTO checkout(UUID userId, String shippingAddress);
    OrderResponseDTO getOrderById(UUID orderId, UUID userId);
    List<OrderResponseDTO> getOrderByUserId(UUID userId);
    OrderResponseDTO updateOrderStatus(UUID orderId, OrderStatus newStatus);
    OrderResponseDTO cancelOrder(UUID orderId, UUID userId);
    OrderResponseDTO paidOrder(UUID orderId, UUID userId);
    Page<OrderResponseDTO> getAllOrders(Pageable pageable);
}
