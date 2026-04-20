package lipari.academy.com.laptopverse.modules.product.repository;

import lipari.academy.com.laptopverse.modules.product.model.Product;
import org.hibernate.annotations.processing.SQL;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    Page<Product> findByIsActiveTrue(Pageable pageable);

    Optional<Product> findActiveBySlug(String slug);

    Optional<Product> findActiveBySku(String sku);

    Optional<Product> findBySlugAndIsActiveTrue(String slug);

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND " +
            "(LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.sku) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> searchActiveProducts(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId AND p.isActive = true")
    Page<Product> findByCategory(@Param("categoryId") UUID categoryId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId AND p.isActive = true")
    Page<Product> findByCategoryIdAndISActiveTrue(@Param("categoryId") UUID categoryId, Pageable pageable);

    boolean existsBySku(String sku);

    boolean existsBySlug(String slug);

    long countByCategoryId(UUID categoryId);
}
