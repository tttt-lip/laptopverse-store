package lipari.academy.com.laptopverse.modules.product.service.imp;

import lipari.academy.com.laptopverse.exception.DuplicateResourceException;
import lipari.academy.com.laptopverse.exception.ResourceNotFoundException;
import lipari.academy.com.laptopverse.modules.category.model.Category;
import lipari.academy.com.laptopverse.modules.category.repository.CategoryRepository;
import lipari.academy.com.laptopverse.modules.product.dto.ProductRequestDTO;
import lipari.academy.com.laptopverse.modules.product.dto.ProductResponseDTO;
import lipari.academy.com.laptopverse.modules.product.mapper.ProductMapper;
import lipari.academy.com.laptopverse.modules.product.model.Product;
import lipari.academy.com.laptopverse.modules.product.repository.ProductRepository;
import lipari.academy.com.laptopverse.modules.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductServiceImp implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional
    public ProductResponseDTO createProduct(ProductRequestDTO requestDTO) {

        if (productRepository.existsBySku(requestDTO.sku())) {
            throw new DuplicateResourceException("Product existed with sku" + requestDTO.sku());
        }

        Category category = categoryRepository.findById(requestDTO.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id " + requestDTO.categoryId()));


        Product product = productMapper.toEntity(requestDTO);
        product.setCategory(category);

        //Generazione Slug
        String slug = product.getName().toLowerCase()
                .replaceAll("[^a-z0-9 ]", "")
                .replaceAll(" ", "-");

        int count = 1;
        while (productRepository.existsBySlug(slug)) {
            slug = slug + "-" + count++;
        }
        product.setSlug(slug);

        Product saved = productRepository.save(product);
        return productMapper.toDTO(saved);

    }

    @Override
    public ProductResponseDTO updateProduct(UUID id, ProductRequestDTO requestDTO) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + id));

        if (!product.getSku().equals(requestDTO.sku()) && productRepository.existsBySku(requestDTO.sku())) {
            throw new DuplicateResourceException("Prudct existed with sku : " + requestDTO.sku());
        }

        Category category = categoryRepository.findById(requestDTO.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id " + requestDTO.categoryId()));

        product.setName(requestDTO.name());
        product.setSku(requestDTO.sku());
        product.setPrice(requestDTO.price());
        product.setStockQuantity(requestDTO.stockQuantity());
        product.setSpecs(requestDTO.specs());
        product.setCategory(category);


        if (!product.getName().equals(requestDTO.name())) {
            String slug = product.getName().toLowerCase()
                    .replaceAll("[^a-z0-9 ]", "")
                    .replaceAll(" ", "-");

            int count = 1;
            while (productRepository.existsBySlug(slug) && !slug.equals(product.getSlug())) {
                slug = slug + "-" + count++;
            }
            product.setSlug(slug);

        }
        return productMapper.toDTO(productRepository.save(product));
    }

    @Override
    public void deleteProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proudct not found with id = " + id));

        product.setIsActive(false);
        productRepository.save(product);
    }

    @Override
    public Page<ProductResponseDTO> getActiveProducts(Pageable pageable) {
        return productRepository.findByIsActiveTrue(pageable)
                .map(productMapper::toDTO);
    }

    @Override
    public ProductResponseDTO getProductBySlug(String slug) {
        return productRepository.findActiveBySlug(slug)
                .map(productMapper::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Proudct not found with id = " + slug));
    }

    @Override
    public Page<ProductResponseDTO> searchProducts(String keyword, Pageable pageable) {
        return productRepository.searchActiveProducts(keyword, pageable)
                .map(productMapper::toDTO);
    }

    @Override
    public Page<ProductResponseDTO> getProductsByCategory(UUID categoryId, Pageable pageable) {
        return productRepository.findByCategory(categoryId, pageable)
                .map(productMapper::toDTO);
    }
}
