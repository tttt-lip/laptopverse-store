package lipari.academy.com.laptopverse.modules.category.repository;

import lipari.academy.com.laptopverse.modules.category.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
