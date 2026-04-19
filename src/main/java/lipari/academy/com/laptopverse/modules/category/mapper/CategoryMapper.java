package lipari.academy.com.laptopverse.modules.category.mapper;


import lipari.academy.com.laptopverse.modules.category.dto.CategoryDTO;
import lipari.academy.com.laptopverse.modules.category.model.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "parent",ignore = true)
    Category toEntity(CategoryDTO categoryDTO);

    @Mapping(source = "parent.id", target = "parentId")
    CategoryDTO toDTO(Category category);
}