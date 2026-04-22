package lipari.academy.com.laptopverse.modules.cart.service.imp;

import lipari.academy.com.laptopverse.exception.BadRequestException;
import lipari.academy.com.laptopverse.exception.ResourceNotFoundException;
import lipari.academy.com.laptopverse.modules.cart.dto.AddToCartRequest;
import lipari.academy.com.laptopverse.modules.cart.dto.CartResponseDTO;
import lipari.academy.com.laptopverse.modules.cart.mapper.CartMapper;
import lipari.academy.com.laptopverse.modules.cart.model.Cart;
import lipari.academy.com.laptopverse.modules.cart.repository.CartRepository;
import lipari.academy.com.laptopverse.modules.cart.service.CartService;
import lipari.academy.com.laptopverse.modules.cart.model.CartItem;
import lipari.academy.com.laptopverse.modules.product.model.Product;
import lipari.academy.com.laptopverse.modules.product.repository.ProductRepository;
import lipari.academy.com.laptopverse.modules.user.mapper.UserMapper;
import lipari.academy.com.laptopverse.modules.user.model.User;
import lipari.academy.com.laptopverse.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartServiceImp implements CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartMapper cartMapper;
    private final UserMapper userMapper;


    @Override
    @Transactional
    public CartResponseDTO getOrCreateCart(UUID userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found with userId: " + userId));
                    Cart newCart = Cart.builder().user(user).items(new ArrayList<>()).build();
                    return cartRepository.save(newCart);
                });
        return mapToResponse(cart);
    }

    @Override
    @Transactional
    public CartResponseDTO addToCart(UUID userId, AddToCartRequest addToCartRequest) {

        UUID addProductId = addToCartRequest.productId();
        Product product = productRepository.findById(addProductId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + addProductId));


        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));

        Cart cart = cartRepository.findByUserId(userId)
                .orElse(Cart.builder().
                        user(user).
                        build());
        Cart saved = proccessAddToCart(cart, addToCartRequest, product, addProductId);

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public CartResponseDTO removeItem(UUID userId, UUID itemId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found with userId: " + userId));

        Optional<CartItem> cartItem = cart.getItems()
                .stream().filter(cartItemExists -> cartItemExists.getId().equals(itemId))
                .findFirst();

        if (cartItem.isEmpty()) {
            throw new ResourceNotFoundException("CartItem not found with id: " + itemId);
        }

        cart.removeItem(cartItem.get());

        Cart saved = cartRepository.save(cart);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void clearCart(UUID userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found with userId: " + userId));

        cart.getItems().clear();

        cartRepository.save(cart);
    }

    @Override
    public CartResponseDTO getByCardIdGuest(UUID cartId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found with id: " + cartId));
        return mapToResponse(cart);
    }

    @Override
    @Transactional
    public CartResponseDTO addToCartGuest(UUID cartId, AddToCartRequest addToCartRequest) {

        UUID productId = addToCartRequest.productId();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + productId));
        Cart cart = cartRepository.findById(cartId)
                .orElse(Cart.builder().
                        user(null).
                        build());
        Cart saved = proccessAddToCart(cart, addToCartRequest, product, productId);

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public CartResponseDTO removeItemGuest(UUID cartId, UUID itemId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found with id: " + cartId));

        cart.getItems()
                .stream().filter(cartItemExists -> cartItemExists.getId().equals(itemId))
                .findFirst()
                .ifPresentOrElse(cart::removeItem,
                        // se non è presente
                        () -> {
                            throw new ResourceNotFoundException("CartItem not found with id: " + itemId);
                        }
                );
        Cart saved = cartRepository.save(cart);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void clearCartGuest(UUID cartId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found with id: " + cartId));
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    @Override
    public CartResponseDTO createGuestCart() {
        Cart newCart = Cart.builder()
                .user(null)
                .items(new ArrayList<>())
                .build();
        Cart saved = cartRepository.save(newCart);
        return mapToResponse(saved);
    }

    private @NonNull Cart proccessAddToCart(Cart cart, AddToCartRequest addToCartRequest, Product product, UUID addProductId) {
        int quantity = addToCartRequest.quantity();
        if (quantity > product.getStockQuantity()) {
            throw new BadRequestException("Insufficient Stock, quantity: " + quantity);
        }
        Optional<CartItem> exitsCart = cart.getItems().stream()
                .filter(cartItem -> cartItem.getProduct().getId().equals(addProductId))
                .findFirst();

        exitsCart.ifPresentOrElse(cartItem -> {
                    // Se esiste allora controlla la quantità e poi aggiungi
                    if (cartItem.getQuantity() + quantity > product.getStockQuantity()) {
                        throw new BadRequestException("Insufficient Stock, quantity: " + quantity);
                    }
                    cartItem.setQuantity(cartItem.getQuantity() + quantity);
                },
                // Se esiste allora crea e aggiungi
                () -> cart.addItem(CartItem.builder()
                        .product(product)
                        .cart(cart)
                        .quantity(quantity)
                        .priceSnapshot(product.getPrice())
                        .build()));

        return cartRepository.save(cart);
    }


    private CartResponseDTO mapToResponse(Cart cart) {
        BigDecimal totalPrice = cart.getItems().stream()
                .map(i -> i.getPriceSnapshot().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Integer quantityTotal = cart.getItems().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();

        return new CartResponseDTO(
                cart.getId(),
                cart.getItems().stream().map(cartMapper::toItemDTO).toList(),
                totalPrice,
                quantityTotal,
                userMapper.toResponse(cart.getUser() == null ? null : cart.getUser())
        );
    }
}
