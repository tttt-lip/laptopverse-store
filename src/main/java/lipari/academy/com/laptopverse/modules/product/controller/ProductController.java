package lipari.academy.com.laptopverse.modules.product.controller;

import jakarta.validation.Valid;
import lipari.academy.com.laptopverse.common.ApiResponse;
import lipari.academy.com.laptopverse.modules.product.dto.ProductRequestDTO;
import lipari.academy.com.laptopverse.modules.product.dto.ProductResponseDTO;
import lipari.academy.com.laptopverse.modules.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponseDTO>> create(@Valid @RequestBody ProductRequestDTO requestDTO) {
        ProductResponseDTO data = productService.createProduct(requestDTO);
        return new ResponseEntity<>(ApiResponse.success(data, "Product created"), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponseDTO>> update(@PathVariable UUID id, @Valid @RequestBody ProductRequestDTO requestDTO) {
        ProductResponseDTO data = productService.updateProduct(id, requestDTO);
        return ResponseEntity.ok(ApiResponse.success(data, "Product update"));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable UUID id) {
        productService.softDeleteProduct(id);
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<ProductResponseDTO>> getBySlug(@PathVariable String slug) {
        ProductResponseDTO data = productService.getProductBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(data,"List Products Recuperated"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponseDTO>>> getAll(@PageableDefault(size = 10) Pageable pageable) {
        Page<ProductResponseDTO> data = productService.getActiveProducts(pageable);
        return ResponseEntity.ok(ApiResponse.success(data, "Products list"));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<ProductResponseDTO>>> search(@RequestParam String keyword, Pageable pageable) {
        Page<ProductResponseDTO> data = productService.searchProducts(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(data, "Search keyword: " + keyword));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<Page<ProductResponseDTO>>> getByCategory(@PathVariable UUID categoryId, Pageable pageable) {
        Page<ProductResponseDTO> data = productService.getProductsByCategory(categoryId, pageable);
        return ResponseEntity.ok(ApiResponse.success(data, "Products for category"));
    }
}
