package lipari.academy.com.laptopverse.modules.cart.repository;

import lipari.academy.com.laptopverse.modules.cart.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID> {
    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items i WHERE  c.user.id = :userId")
    Optional<Cart> findByUserId(@Param("userId") UUID userId);
}
