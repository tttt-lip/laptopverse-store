package lipari.academy.com.laptopverse.modules.order.controller;

import jakarta.validation.Valid;
import lipari.academy.com.laptopverse.common.ApiResponse;
import lipari.academy.com.laptopverse.modules.order.dto.CheckoutRequest;
import lipari.academy.com.laptopverse.modules.order.dto.OrderResponseDTO;
import lipari.academy.com.laptopverse.modules.order.dto.UpdateStatusRequest;
import lipari.academy.com.laptopverse.modules.order.service.OrderService;
import lipari.academy.com.laptopverse.modules.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> checkout(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CheckoutRequest checkoutRequest) {

        OrderResponseDTO data = orderService.checkout(currentUser.getId(), checkoutRequest.getShippingAddress());
        return new ResponseEntity<>(ApiResponse.success(data, "Order placed successfully"), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponseDTO>>> getUserOrders(
            @AuthenticationPrincipal User currentUser) {

        List<OrderResponseDTO> data = orderService.getOrderByUserId(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(data, "Orders retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> getOrderById(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id) {

        OrderResponseDTO data = orderService.getOrderById(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(data, "Order found"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> updateStatus(
            @PathVariable UUID id,
            @RequestBody UpdateStatusRequest request) {

        OrderResponseDTO data = orderService.updateOrderStatus(id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success(data, "Order status updated successfully"));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<OrderResponseDTO>>> getAllOrdersForAdmin(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        Page<OrderResponseDTO> data = orderService.getAllOrders(pageable);
        return ResponseEntity.ok(ApiResponse.success(data, "All orders retrieved successfully"));
    }
}
