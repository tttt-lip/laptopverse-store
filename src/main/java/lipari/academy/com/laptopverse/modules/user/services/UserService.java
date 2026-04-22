package lipari.academy.com.laptopverse.modules.user.services;


import lipari.academy.com.laptopverse.modules.user.dto.LoginRequest;
import lipari.academy.com.laptopverse.modules.user.dto.LoginResponse;
import lipari.academy.com.laptopverse.modules.user.dto.UserRequestDTO;
import lipari.academy.com.laptopverse.modules.user.dto.UserResponseDTO;
import lipari.academy.com.laptopverse.modules.user.model.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.UUID;

public interface UserService {
    UserResponseDTO register(UserRequestDTO dto, UserDetails currentUser);

    LoginResponse login(LoginRequest loginRequest);

    User findUserByEmail(String email);

    List<UserResponseDTO> getAllUser();


    void softDeleteById(UUID id);

    UserResponseDTO updateUserById(UUID userId, lipari.academy.com.laptopverse.modules.user.dto.UserUpdateDTO userUpdateDTO);

}
