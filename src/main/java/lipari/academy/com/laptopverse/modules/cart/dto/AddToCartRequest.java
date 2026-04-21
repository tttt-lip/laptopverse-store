package lipari.academy.com.laptopverse.modules.cart.dto;

import jakarta.validation.constraints.Min;

import java.util.UUID;

public record AddToCartRequest(
        UUID productId,
        @Min(value = 1,message = "La quantity deve essere > 0") Integer quantity
) {
}
