package lipari.academy.com.laptopverse.modules.user.mapper;


import lipari.academy.com.laptopverse.modules.user.dto.UserRequestDTO;
import lipari.academy.com.laptopverse.modules.user.dto.UserResponseDTO;
import lipari.academy.com.laptopverse.modules.user.dto.UserUpdateDTO;
import lipari.academy.com.laptopverse.modules.user.model.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "id", ignore = true)
    User toEntity(UserRequestDTO dto);

    UserResponseDTO toResponse(User userEntity);

    @Mapping(target = "id", ignore = true)
    void updateEntityFromDto(UserUpdateDTO dto, @MappingTarget User user);
}
