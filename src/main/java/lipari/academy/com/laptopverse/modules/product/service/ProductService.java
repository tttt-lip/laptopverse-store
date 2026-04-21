package lipari.academy.com.laptopverse.modules.product.service;

import lipari.academy.com.laptopverse.modules.product.dto.ProductRequestDTO;
import lipari.academy.com.laptopverse.modules.product.dto.ProductResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ProductService {

    ProductResponseDTO createProduct(ProductRequestDTO requestDTO);

    ProductResponseDTO updateProduct(UUID id, ProductRequestDTO requestDTO);

    // Soft Delete: isActive = false
    void softDeleteProduct(UUID id);

    Page<ProductResponseDTO> getActiveProducts(Pageable pageable);

    ProductResponseDTO getProductBySlug(String slug);

    Page<ProductResponseDTO> searchProducts(String keyword, Pageable pageable);

    Page<ProductResponseDTO> getProductsByCategory(UUID categoryId, Pageable pageable);
}
