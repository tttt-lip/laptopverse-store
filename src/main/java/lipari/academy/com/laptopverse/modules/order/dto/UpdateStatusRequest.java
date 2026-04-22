package lipari.academy.com.laptopverse.modules.order.dto;

import lipari.academy.com.laptopverse.modules.order.model.OrderStatus;
import lombok.Data;

@Data
public class UpdateStatusRequest {
    private OrderStatus status;
}
