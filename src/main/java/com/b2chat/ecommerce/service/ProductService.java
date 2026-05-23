package com.b2chat.ecommerce.service;

import com.b2chat.ecommerce.dto.request.CreateProductRequest;
import com.b2chat.ecommerce.dto.request.UpdateProductRequest;
import com.b2chat.ecommerce.dto.response.ProductResponse;

import java.util.List;

public interface ProductService {
    ProductResponse createProduct(CreateProductRequest request);
    List<ProductResponse> getAllProducts();
    ProductResponse getProductById(Long id);
    ProductResponse updateProduct(Long id, UpdateProductRequest request);
    void deleteProduct(Long id);
}
