package lipari.academy.com.laptopverse.modules.cart.controller;

import jakarta.validation.Valid;
import lipari.academy.com.laptopverse.common.ApiResponse;
import lipari.academy.com.laptopverse.exception.BadRequestException;
import lipari.academy.com.laptopverse.modules.cart.dto.AddToCartRequest;
import lipari.academy.com.laptopverse.modules.cart.dto.CartResponseDTO;
import lipari.academy.com.laptopverse.modules.cart.service.CartService;
import lipari.academy.com.laptopverse.modules.user.model.User;
import lipari.academy.com.laptopverse.modules.user.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    /*
        Recupero Carrello con JWT o Guest Header
     */
    @GetMapping
    public ResponseEntity<ApiResponse<CartResponseDTO>> getCartJWT(
            @AuthenticationPrincipal User currentUser,
            @RequestHeader(value = "X-Guest-Cart-Id", required = false) UUID guestCartId) {
        CartResponseDTO data;
        if (currentUser != null) {
            data = cartService.getOrCreateCart(currentUser.getId());
        } else if (guestCartId != null) {
            data = cartService.getByCardIdGuest(guestCartId);
        } else {
            throw new BadRequestException("Login required or provide X-Guest-Card-Id Header");
        }

        return ResponseEntity.ok(ApiResponse.success(data, "Cart found"));
    }

    /*
     Permette di creare un carrello per gli utente Guest
     */
    @GetMapping("/guest")
    public ResponseEntity<ApiResponse<CartResponseDTO>> createCartGuest() {
        CartResponseDTO data = cartService.createGuestCart();
        return ResponseEntity.ok(ApiResponse.success(data, "Cart Guest create"));
    }

    /*
        Aggiunge un item nel carrello con JWT o Guest Header
     */
    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponseDTO>> addItemCartJWT(@AuthenticationPrincipal User currentUser,
                                                                       @Valid @RequestBody AddToCartRequest addToCartRequest,
                                                                       @RequestHeader(value = "X-Guest-Cart-Id", required = false) UUID guestCartId
    ) {
        CartResponseDTO data = currentUser != null ?
                cartService.addToCart(currentUser.getId(), addToCartRequest) :
                cartService.addToCartGuest(guestCartId, addToCartRequest);

        return new ResponseEntity<>(ApiResponse.success(data, "Product added to cart "), HttpStatus.CREATED);
    }




    /*
        Rimuove un elemento dal carrello
     */

    @DeleteMapping("/item/{itemId}")
    public ResponseEntity<ApiResponse<CartResponseDTO>> removeItem(@AuthenticationPrincipal User currentUser,
                                                                   @RequestHeader(value = "X-Guest-Cart-Id", required = false) UUID guestCartId,
                                                                   @PathVariable UUID itemId) {

        CartResponseDTO data = currentUser != null ? cartService.removeItem(currentUser.getId(), itemId) :
                cartService.removeItemGuest(guestCartId, itemId);
        return ResponseEntity.ok(ApiResponse.success(data, "Item remove from cart"));
    }

    /*
        Pulisce gli elementi dal carrello
     */

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearCart(@AuthenticationPrincipal User currentUser,
                                                       @RequestHeader(value = "X-Guest-Cart-Id", required = false) UUID guestCartId) {
        if (currentUser != null) {
            cartService.clearCart(currentUser.getId());
        } else if (guestCartId != null) {
            cartService.clearCartGuest(guestCartId);
        } else {
            throw new BadRequestException("Login required or provide X-Guest-Card-Id ");
        }
        return ResponseEntity.ok(ApiResponse.success(null, "Cart is clear"));
    }


    @PostMapping("/user/{userId}/items")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CartResponseDTO>> addItemCart(@PathVariable UUID userId,
                                                                    @Valid @RequestBody AddToCartRequest addToCartRequest
    ) {
        CartResponseDTO data = cartService.addToCart(userId, addToCartRequest);
        return new ResponseEntity<>(ApiResponse.success(data, "Product added to cart "), HttpStatus.CREATED);
    }


    @GetMapping("/userId/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CartResponseDTO>> getCart(@PathVariable("userId") UUID userId) {
        CartResponseDTO data = cartService.getOrCreateCart(userId);
        return ResponseEntity.ok(ApiResponse.success(data, "Cart found"));
    }

}
