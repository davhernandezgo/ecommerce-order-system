package com.b2chat.ecommerce.service.impl;

import com.b2chat.ecommerce.dto.request.CreateProductRequest;
import com.b2chat.ecommerce.dto.request.UpdateProductRequest;
import com.b2chat.ecommerce.dto.response.ProductResponse;
import com.b2chat.ecommerce.entity.Product;
import com.b2chat.ecommerce.exception.ResourceNotFoundException;
import com.b2chat.ecommerce.repository.ProductRepository;
import com.b2chat.ecommerce.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic for product catalog management with Redis caching.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final String CACHE_PRODUCTS      = "products";
    private static final String CACHE_PRODUCT_LIST  = "productList";

    private final ProductRepository productRepository;

    @Override
    @Transactional
    @Caching(evict = {@CacheEvict(value = CACHE_PRODUCT_LIST, allEntries = true)},
             put   = {@CachePut(value = CACHE_PRODUCTS, key = "#result.id")})
    public ProductResponse createProduct(CreateProductRequest request) {
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .build();

        Product saved = productRepository.save(product);
        log.info("Product created: {} (id={})", saved.getName(), saved.getId());
        return ProductResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_PRODUCT_LIST, key = "'all'")
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_PRODUCTS, key = "#id")
    public ProductResponse getProductById(Long id) {
        return productRepository.findById(id)
                .map(ProductResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    @Override
    @Transactional
    @Caching(evict = {@CacheEvict(value = CACHE_PRODUCT_LIST, allEntries = true)},
             put   = {@CachePut(value = CACHE_PRODUCTS, key = "#id")})
    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        if (request.getName() != null)          product.setName(request.getName());
        if (request.getDescription() != null)   product.setDescription(request.getDescription());
        if (request.getPrice() != null)         product.setPrice(request.getPrice());
        if (request.getStockQuantity() != null) product.setStockQuantity(request.getStockQuantity());

        Product updated = productRepository.save(product);
        log.info("Product updated: id={}", updated.getId());
        return ProductResponse.from(updated);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_PRODUCTS, key = "#id"),
            @CacheEvict(value = CACHE_PRODUCT_LIST, allEntries = true)
    })
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product", id);
        }
        productRepository.deleteById(id);
        log.info("Product deleted: id={}", id);
    }
}
