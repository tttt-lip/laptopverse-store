package lipari.academy.com.laptopverse.modules.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddToCartRequest(
        @NotNull(message = "Il productId è obbligatorio")
        UUID productId,
        @Min(value = 1,message = "La quantity deve essere > 0") Integer quantity
) {
}
