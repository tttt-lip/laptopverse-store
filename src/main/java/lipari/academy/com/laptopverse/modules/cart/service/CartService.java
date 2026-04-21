package lipari.academy.com.laptopverse.modules.cart.service;

import lipari.academy.com.laptopverse.modules.cart.dto.AddToCartRequest;
import lipari.academy.com.laptopverse.modules.cart.dto.CartResponseDTO;

import java.util.UUID;

public interface CartService {
    CartResponseDTO getOrCreateCart(UUID userId);

    CartResponseDTO addToCart(UUID userId, AddToCartRequest addToCartRequest);

    CartResponseDTO removeItem(UUID userId, UUID itemId);

    void clearCart(UUID userId);

    CartResponseDTO getByCardIdGuest(UUID cartId);

    CartResponseDTO addToCartGuest(UUID cartId, AddToCartRequest addToCartRequest);

    CartResponseDTO removeItemGuest(UUID cartId, UUID itemId);
    void clearCartGuest(UUID cartId);

    CartResponseDTO createGuestCart();
}
