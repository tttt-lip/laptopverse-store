package lipari.academy.com.laptopverse.modules.category.dto;

import java.util.UUID;

public record CategoryDTO(
        UUID id,

        String name,

        String slug,

        String description,

        UUID parentId

) {
}