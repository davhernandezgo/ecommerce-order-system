package com.b2chat.ecommerce.service;

import com.b2chat.ecommerce.dto.request.CreateProductRequest;
import com.b2chat.ecommerce.dto.request.UpdateProductRequest;
import com.b2chat.ecommerce.dto.response.ProductResponse;
import com.b2chat.ecommerce.entity.Product;
import com.b2chat.ecommerce.exception.ResourceNotFoundException;
import com.b2chat.ecommerce.repository.ProductRepository;
import com.b2chat.ecommerce.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Unit Tests")
class ProductServiceTest {

    @Mock ProductRepository productRepository;
    @InjectMocks ProductServiceImpl productService;

    @Test
    @DisplayName("createProduct — persists and returns response")
    void createProduct_success() {
        CreateProductRequest req = new CreateProductRequest();
        req.setName("Gadget"); req.setPrice(new BigDecimal("9.99")); req.setStockQuantity(100);

        Product saved = Product.builder().id(1L).name("Gadget")
                .price(new BigDecimal("9.99")).stockQuantity(100).build();
        given(productRepository.save(any(Product.class))).willReturn(saved);

        ProductResponse response = productService.createProduct(req);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Gadget");
    }

    @Test
    @DisplayName("getAllProducts — returns list of all products")
    void getAllProducts_returnsList() {
        Product p1 = Product.builder().id(1L).name("A").price(BigDecimal.ONE).stockQuantity(5).build();
        Product p2 = Product.builder().id(2L).name("B").price(BigDecimal.TEN).stockQuantity(3).build();
        given(productRepository.findAll()).willReturn(List.of(p1, p2));

        List<ProductResponse> result = productService.getAllProducts();

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("updateProduct — partial update only changes provided fields")
    void updateProduct_partialUpdate() {
        Product existing = Product.builder().id(1L).name("Old Name")
                .price(new BigDecimal("5.00")).stockQuantity(10).build();
        given(productRepository.findById(1L)).willReturn(Optional.of(existing));
        given(productRepository.save(any(Product.class))).willAnswer(inv -> inv.getArgument(0));

        UpdateProductRequest req = new UpdateProductRequest();
        req.setName("New Name");  // only update name

        ProductResponse response = productService.updateProduct(1L, req);

        assertThat(response.getName()).isEqualTo("New Name");
        assertThat(response.getPrice()).isEqualTo(new BigDecimal("5.00")); // unchanged
    }

    @Test
    @DisplayName("deleteProduct — throws ResourceNotFoundException for unknown ID")
    void deleteProduct_notFound() {
        given(productRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> productService.deleteProduct(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
