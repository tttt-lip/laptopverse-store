package lipari.academy.com.laptopverse.modules.category.controller;


import jakarta.validation.Valid;
import lipari.academy.com.laptopverse.common.ApiResponse;
import lipari.academy.com.laptopverse.modules.category.dto.CategoryDTO;
import lipari.academy.com.laptopverse.modules.category.services.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryDTO>> createCategory(@Valid @RequestBody CategoryDTO categoryDTO) {
        CategoryDTO categoryDTOcreate = categoryService.createCategory(categoryDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(categoryDTOcreate, "Category created"));
    }


    @GetMapping("/alls")
    public ResponseEntity<ApiResponse<List<CategoryDTO>>> findAllCategories() {
        List<CategoryDTO> categoryDTOList = categoryService.findAllCategories();
        return ResponseEntity.ok(ApiResponse.success(categoryDTOList, "Categoires All"));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<CategoryDTO>> getCategoryBySlug(@PathVariable String slug) {
        CategoryDTO category = categoryService.findBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(category, "Category found"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable UUID id) {
        categoryService.deleteCategoryById(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Category deleted successfully"));
    }
}
