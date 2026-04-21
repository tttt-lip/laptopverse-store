package lipari.academy.com.laptopverse.modules.cart.mapper;

import lipari.academy.com.laptopverse.modules.cart.dto.CartItemDTO;
import lipari.academy.com.laptopverse.modules.cart.dto.CartResponseDTO;
import lipari.academy.com.laptopverse.modules.cart.model.Cart;
import lipari.academy.com.laptopverse.modules.product.model.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CartMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    CartItemDTO toItemDTO(CartItem cartItem);

    @Mapping(target = "priceTotal", ignore = true)
    @Mapping(target = "user", ignore = true)
    CartResponseDTO toCartDTO(Cart cart);



}
