package lipari.academy.com.laptopverse.modules.category.services.impl;


import lipari.academy.com.laptopverse.exception.DuplicateResourceException;
import lipari.academy.com.laptopverse.exception.ResourceNotFoundException;
import lipari.academy.com.laptopverse.modules.category.dto.CategoryDTO;
import lipari.academy.com.laptopverse.modules.category.mapper.CategoryMapper;
import lipari.academy.com.laptopverse.modules.category.model.Category;
import lipari.academy.com.laptopverse.modules.category.repository.CategoryRepository;
import lipari.academy.com.laptopverse.modules.category.services.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryServcieImp implements CategoryService {


    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    public CategoryDTO createCategory(CategoryDTO categoryDTO) {
        if (categoryRepository.existsBySlug(categoryDTO.slug())) {
            throw new DuplicateResourceException("Category is duplicated with slug =  " + categoryDTO.slug());
        }

        Category category = categoryMapper.toEntity(categoryDTO);

        if (categoryDTO.parentId() != null) {
            Category parent = categoryRepository.findById(categoryDTO.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent not found with id: " + categoryDTO.parentId()));

            category.setParent(parent);
        }

        Category savedCategory = categoryRepository.save(category);

        return categoryMapper.toDTO(savedCategory);
    }

    @Override
    public List<CategoryDTO> findAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(categoryMapper::toDTO)
                .toList();
    }

    @Override
    public CategoryDTO findBySlug(String slug) {
        return categoryRepository.findBySlug(slug)
                .map(categoryMapper::toDTO)
                .orElseThrow(() ->new ResourceNotFoundException("Categor not found with id " + slug));

    }


    @Override
    @Transactional
    public void deleteCategoryById(UUID id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Categor not found with id " + id);
        }
        categoryRepository.deleteById(id);
    }
}
