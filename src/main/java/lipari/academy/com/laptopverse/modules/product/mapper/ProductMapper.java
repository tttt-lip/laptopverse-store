package lipari.academy.com.laptopverse.modules.product.mapper;


import lipari.academy.com.laptopverse.modules.product.dto.ProductRequestDTO;
import lipari.academy.com.laptopverse.modules.product.dto.ProductResponseDTO;
import lipari.academy.com.laptopverse.modules.product.model.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    Product toEntity(ProductRequestDTO productRequestDTO);


    @Mapping(target = "categoryName",source = "category.name")
    @Mapping(target = "categoryId",source = "category.id")
    ProductResponseDTO toDTO(Product product);
}
