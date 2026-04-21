package lipari.academy.com.laptopverse.modules.cart.dto;

import lipari.academy.com.laptopverse.modules.user.dto.UserResponseDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartResponseDTO(
        UUID id,
        List<CartItemDTO> items,
        BigDecimal totalPrice,
        Integer totalQuantity, UserResponseDTO user
) {
}
