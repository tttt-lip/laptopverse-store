package lipari.academy.com.laptopverse.modules.category.services;

import lipari.academy.com.laptopverse.modules.category.dto.CategoryDTO;

import java.util.List;
import java.util.UUID;

public interface CategoryService {

    CategoryDTO createCategory(CategoryDTO categoryDTO);

    List<CategoryDTO> findAllCategories();

    CategoryDTO findBySlug(String slug);

    void deleteCategoryById(UUID id);
}
