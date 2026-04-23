package lipari.academy.com.laptopverse.modules.order.dto;

import jakarta.validation.constraints.NotBlank;

public record CheckoutRequest(
        @NotBlank(message = "L'indirizzo di spedizione è obbligatorio")
        String shippingAddress
) {
}
