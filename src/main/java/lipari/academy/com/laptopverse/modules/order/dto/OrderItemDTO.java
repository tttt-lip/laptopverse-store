package lipari.academy.com.laptopverse.modules.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class OrderItemDTO {
    private UUID productId;
    private String productNameSnapshot;
    private String skuSnapshot;
    private Integer quantity;
    private BigDecimal unitPriceSnapshot;
}
