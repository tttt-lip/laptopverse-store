package lipari.academy.com.laptopverse.modules.order.mapper;

import lipari.academy.com.laptopverse.modules.order.dto.OrderItemDTO;
import lipari.academy.com.laptopverse.modules.order.dto.OrderResponseDTO;
import lipari.academy.com.laptopverse.modules.order.model.Order;
import lipari.academy.com.laptopverse.modules.order.model.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "productId", source = "product.id")
    OrderItemDTO toItemDTO(OrderItem orderItem);

    OrderResponseDTO toOrderDTO(Order order);

    List<OrderResponseDTO> toOrderDTOList(List<Order> orders);
}
