package lipari.academy.com.laptopverse.modules.product.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponseDTO(
        UUID id,
        String name,
        String sku,
        String slug,
        BigDecimal price,
        Integer stockQuantity,
        String specs,
        String categoryName,
        UUID categoryId
) {
}