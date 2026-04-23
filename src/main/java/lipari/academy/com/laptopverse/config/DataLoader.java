package lipari.academy.com.laptopverse.config;

import lipari.academy.com.laptopverse.modules.category.model.Category;
import lipari.academy.com.laptopverse.modules.category.repository.CategoryRepository;
import lipari.academy.com.laptopverse.modules.order.model.Order;
import lipari.academy.com.laptopverse.modules.order.model.OrderItem;
import lipari.academy.com.laptopverse.modules.order.model.OrderStatus;
import lipari.academy.com.laptopverse.modules.order.repository.OrderRepository;
import lipari.academy.com.laptopverse.modules.product.model.Product;
import lipari.academy.com.laptopverse.modules.product.repository.ProductRepository;
import lipari.academy.com.laptopverse.modules.user.model.User;
import lipari.academy.com.laptopverse.modules.user.model.UserRole;
import lipari.academy.com.laptopverse.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.UUID;

@Configuration
@RequiredArgsConstructor
public class DataLoader {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner initData() {
        return args -> {
            // 1. Utenti
            createDefaultUser("Admin", "System", "admin@laptopverse.com", "Admin123!", UserRole.ADMIN);
            createDefaultUser("John", "Doe", "customer@laptopverse.com", "Customer123!", UserRole.CUSTOMER);

            // 2. Categorie
            Category gaming = getOrCreateCategory("Gaming Laptops", "gaming-laptops", "High-performance machines for pro gamers.");
            Category business = getOrCreateCategory("Business Laptops", "business-laptops", "Reliable and secure laptops for work.");
            Category ultrabooks = getOrCreateCategory("Ultrabooks", "ultrabooks", "Thin, light, and powerful.");
            Category workstations = getOrCreateCategory("Workstations", "workstations", "Desktop power in a portable form.");

            // 3. Prodotti - Gaming
            createProduct("Lenovo Legion 5", "LEN-LEG-001", "lenovo-legion-5", "1200.00", 15, "Ryzen 7, RTX 3060, 16GB RAM", gaming);
            createProduct("ASUS ROG Zephyrus G14", "ASU-ROG-014", "asus-rog-zephyrus-g14", "1650.00", 8, "Ryzen 9, RTX 3070, 14-inch 120Hz", gaming);
            createProduct("MSI Raider GE76", "MSI-RAI-076", "msi-raider-ge76", "2500.00", 5, "i9-12900H, RTX 3080 Ti, 32GB RAM", gaming);

            // 4. Prodotti - Business
            createProduct("Dell XPS 13", "DEL-XPS-013", "dell-xps-13", "1100.00", 20, "i7, 16GB RAM, 512GB SSD, InfinityEdge", business);
            createProduct("ThinkPad X1 Carbon", "LEN-X1C-009", "thinkpad-x1-carbon", "1450.00", 12, "Carbon fiber, i7, 16GB RAM, LTE", business);
            createProduct("HP EliteBook 840", "HP-ELI-840", "hp-elitebook-840", "1300.00", 10, "i5, 16GB RAM, Wolf Security", business);

            // 5. Prodotti - Ultrabooks
            createProduct("MacBook Air M2", "APP-AIR-M2", "macbook-air-m2", "1199.00", 25, "Apple M2 chip, 8GB RAM, 256GB SSD, Liquid Retina", ultrabooks);
            createProduct("Microsoft Surface Laptop 5", "MS-SUR-L5", "surface-laptop-5", "999.00", 15, "13.5 inch Touchscreen, i5, Alcantara", ultrabooks);
            createProduct("Razer Book 13", "RAZ-BOK-13", "razer-book-13", "1400.00", 7, "Intel Evo, i7, RGB Keyboard, FHD+ Touch", ultrabooks);

            // 6. Prodotti - Workstations
            createProduct("Apple MacBook Pro 16", "APP-PRO-16", "macbook-pro-16", "2499.00", 10, "M2 Max, 32GB RAM, 1TB SSD", workstations);
            createProduct("HP ZBook Studio G8", "HP-ZBO-G8", "hp-zbook-studio-g8", "2100.00", 4, "RTX A2000, i9, DreamColor Display", workstations);
            createProduct("Dell Precision 5570", "DEL-PRE-557", "dell-precision-5570", "2250.00", 6, "i7-12800H, RTX A1000, 32GB RAM", workstations);
            createProduct("Lenovo ThinkPad P16", "LEN-P16-G1", "thinkpad-p16-gen1", "2800.00", 3, "i9-12950HX, RTX A5500, 64GB RAM", workstations);

            // 7. Ordini di esempio per John
            userRepository.findByEmail("customer@laptopverse.com").ifPresent(customer -> {
                if (orderRepository.findByUserIdOrderByCreatedAtDesc(customer.getId()).isEmpty()) {
                    Product p1 = productRepository.findActiveBySku("LEN-LEG-001").orElseThrow();
                    Product p2 = productRepository.findActiveBySku("APP-AIR-M2").orElseThrow();

                    // Ordine PENDING
                    Order pendingOrder = Order.builder()
                            .orderNumber("ORD-INIT-001")
                            .user(customer)
                            .status(OrderStatus.PENDING)
                            .shippingAddress("123 Main St, Springfield")
                            .totalAmount(p1.getPrice())
                            .build();
                    pendingOrder.addItem(OrderItem.builder()
                            .product(p1)
                            .productNameSnapshot(p1.getName())
                            .skuSnapshot(p1.getSku())
                            .quantity(1)
                            .unitPriceSnapshot(p1.getPrice())
                            .build());
                    orderRepository.save(pendingOrder);

                    // Ordine PAID
                    Order paidOrder = Order.builder()
                            .orderNumber("ORD-INIT-002")
                            .user(customer)
                            .status(OrderStatus.PAID)
                            .shippingAddress("123 Main St, Springfield")
                            .totalAmount(p2.getPrice())
                            .build();
                    paidOrder.addItem(OrderItem.builder()
                            .product(p2)
                            .productNameSnapshot(p2.getName())
                            .skuSnapshot(p2.getSku())
                            .quantity(1)
                            .unitPriceSnapshot(p2.getPrice())
                            .build());
                    orderRepository.save(paidOrder);
                }
            });
        };
    }

    private Category getOrCreateCategory(String name, String slug, String description) {
        return categoryRepository.findBySlug(slug)
                .orElseGet(() -> categoryRepository.save(
                        Category.builder()
                                .name(name)
                                .slug(slug)
                                .description(description)
                                .build()
                ));
    }

    private void createProduct(String name, String sku, String slug, String price, int stock, String specs, Category category) {
        if (!productRepository.existsBySku(sku)) {
            productRepository.save(Product.builder()
                    .name(name)
                    .sku(sku)
                    .slug(slug)
                    .price(new BigDecimal(price))
                    .stockQuantity(stock)
                    .specs(specs)
                    .category(category)
                    .isActive(true)
                    .build());
        }
    }

    private void createDefaultUser(String first, String last, String email, String pass, UserRole role) {
        if (!userRepository.existsByEmail(email)) {
            userRepository.save(User.builder()
                    .firstName(first)
                    .lastName(last)
                    .email(email)
                    .password(passwordEncoder.encode(pass))
                    .role(role)
                    .isEnabled(true)
                    .build());
        }
    }
}
