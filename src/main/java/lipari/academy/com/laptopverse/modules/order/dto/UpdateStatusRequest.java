package lipari.academy.com.laptopverse.modules.order.dto;

import jakarta.validation.constraints.NotNull;
import lipari.academy.com.laptopverse.modules.order.model.OrderStatus;

public record UpdateStatusRequest(
        @NotNull(message = "Lo stato è obbligatorio")
        OrderStatus status
) {
}
