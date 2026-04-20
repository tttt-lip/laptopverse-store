package lipari.academy.com.laptopverse.modules.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductRequestDTO(
        @NotBlank(message = "Il nome è obbligatorio")
        String name,

        @NotBlank(message = "Lo SKU è obbligatorio")
        String sku,

        @NotNull(message = "Il prezzo è obbligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "Il prezzo deve essere maggiore di zero")
        BigDecimal price,

        @NotNull(message = "La quantità in stock è obbligatoria")
        @Min(value = 0, message = "Lo stock non può essere negativo")
        Integer stockQuantity,

        String specs,

        @NotNull(message = "La categoria è obbligatoria")
        UUID categoryId
) {
}