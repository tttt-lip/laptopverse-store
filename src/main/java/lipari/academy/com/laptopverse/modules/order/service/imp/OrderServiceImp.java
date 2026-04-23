package lipari.academy.com.laptopverse.modules.order.service.imp;

import lipari.academy.com.laptopverse.exception.InsufficientStockException;
import lipari.academy.com.laptopverse.exception.ResourceNotFoundException;
import lipari.academy.com.laptopverse.exception.InvalidStatusTransitionException;
import lipari.academy.com.laptopverse.modules.cart.model.Cart;
import lipari.academy.com.laptopverse.modules.cart.repository.CartRepository;
import lipari.academy.com.laptopverse.modules.order.dto.OrderResponseDTO;
import lipari.academy.com.laptopverse.modules.order.mapper.OrderMapper;
import lipari.academy.com.laptopverse.modules.order.model.Order;
import lipari.academy.com.laptopverse.modules.order.model.OrderItem;
import lipari.academy.com.laptopverse.modules.order.model.OrderStatus;
import lipari.academy.com.laptopverse.modules.order.repository.OrderRepository;
import lipari.academy.com.laptopverse.modules.order.service.OrderService;
import lipari.academy.com.laptopverse.modules.cart.model.CartItem;
import lipari.academy.com.laptopverse.modules.product.model.Product;
import lipari.academy.com.laptopverse.modules.product.repository.ProductRepository;
import lipari.academy.com.laptopverse.modules.user.model.User;
import lipari.academy.com.laptopverse.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImp implements OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;

    @Override
    @Transactional
    public OrderResponseDTO checkout(UUID userId, String shippingAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user with id: " + userId));

        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cannot checkout an empty cart");
        }

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .user(user)
                .status(OrderStatus.PENDING)
                .shippingAddress(shippingAddress)
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal total = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();

            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new InsufficientStockException("Insufficient stock for product: " + product.getName());
            }

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .productNameSnapshot(product.getName())
                    .skuSnapshot(product.getSku())
                    .quantity(cartItem.getQuantity())
                    .unitPriceSnapshot(product.getPrice())
                    .build();

            order.addItem(orderItem);

            BigDecimal itemTotal = product.getPrice().multiply(new BigDecimal(cartItem.getQuantity()));
            total = total.add(itemTotal);


            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);
        }

        order.setTotalAmount(total);

        cart.getItems().clear();
        cartRepository.save(cart);

        Order savedOrder = orderRepository.save(order);
        return orderMapper.toOrderDTO(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(UUID orderId, UUID userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Unauthorized access to order");
        }
        return orderMapper.toOrderDTO(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getOrderByUserId(UUID userId) {
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return orderMapper.toOrderDTOList(orders);
    }

    @Override
    @Transactional
    public OrderResponseDTO updateOrderStatus(UUID orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        validateStatusTransition(order.getStatus(), newStatus);

        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);

        if (newStatus == OrderStatus.CANCELLED) {
            restoreProductStock(updatedOrder);
        }

        return orderMapper.toOrderDTO(updatedOrder);
    }

    @Override
    @Transactional
    public OrderResponseDTO cancelOrder(UUID orderId, UUID userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidStatusTransitionException("");
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order cancelledOrder = orderRepository.save(order);
        
        restoreProductStock(cancelledOrder);

        return orderMapper.toOrderDTO(cancelledOrder);
    }

    @Override
    public OrderResponseDTO paidOrder(UUID orderId, UUID userId) {
        return null;
    }

    private void restoreProductStock(Order order) {
        for (OrderItem orderItem : order.getItems()) {
            Product product = orderItem.getProduct();
            product.setStockQuantity(product.getStockQuantity() + orderItem.getQuantity());
            productRepository.save(product);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable)
                .map(orderMapper::toOrderDTO);
    }

    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        boolean valid = switch (currentStatus) {
            case PENDING -> List.of(OrderStatus.PAID, OrderStatus.CANCELLED).contains(newStatus);
            case PAID -> List.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED).contains(newStatus);
            case SHIPPED -> Objects.equals(OrderStatus.DELIVERED, newStatus);
            case DELIVERED, CANCELLED -> false;
        };

        if (!valid) {
            throw new InvalidStatusTransitionException(
                    "Cannot transition order from " + currentStatus + " to " + newStatus
            );
        }
    }

    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis();
    }
}
