package lipari.academy.com.laptopverse.modules.order.dto;

import lipari.academy.com.laptopverse.modules.order.model.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class OrderResponseDTO {
    private UUID id;
    private String orderNumber;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private String shippingAddress;
    private List<OrderItemDTO> items;
    private LocalDateTime createdAt;
}
