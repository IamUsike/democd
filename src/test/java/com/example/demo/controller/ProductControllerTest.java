package com.example.demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice test — loads only the MVC layer (ProductController + GlobalExceptionHandler).
 * No database, no JdbcTemplate, no full Spring context.
 */
@WebMvcTest(controllers = {ProductController.class, GlobalExceptionHandler.class})
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    private final Product laptop = new Product(1L, "Laptop", 1500);

    // ========================================================
    //  GET /products
    // ========================================================

    @Test
    @DisplayName("GET /products returns 200 and JSON array")
    void getProducts_returns200AndJsonArray() throws Exception {
        when(productService.getProducts())
                .thenReturn(List.of(laptop, new Product(2L, "Mouse", 25)));

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].prodName").value("Laptop"))
                .andExpect(jsonPath("$[1].prodName").value("Mouse"));
    }

    @Test
    @DisplayName("GET /products returns 200 with empty array when no products")
    void getProducts_empty_returns200EmptyArray() throws Exception {
        when(productService.getProducts()).thenReturn(List.of());

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ========================================================
    //  GET /product/{prodId}
    // ========================================================

    @Test
    @DisplayName("GET /product/{id} returns 200 when found")
    void getProdById_found_returns200() throws Exception {
        when(productService.getProdById(1L)).thenReturn(Optional.of(laptop));

        mockMvc.perform(get("/product/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prodId").value(1))
                .andExpect(jsonPath("$.prodName").value("Laptop"))
                .andExpect(jsonPath("$.price").value(1500));
    }

    @Test
    @DisplayName("GET /product/{id} returns 404 when not found")
    void getProdById_notFound_returns404() throws Exception {
        when(productService.getProdById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/product/99"))
                .andExpect(status().isNotFound());
    }

    // ========================================================
    //  POST /product
    // ========================================================

    @Test
    @DisplayName("POST /product returns 200 and created product on valid input")
    void addProduct_valid_returns200AndProduct() throws Exception {
        when(productService.addProduct(any())).thenReturn(laptop);

        var requestBody = objectMapper.writeValueAsString(
                new Product(null, "Laptop", 1500));

        mockMvc.perform(post("/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prodId").value(1))
                .andExpect(jsonPath("$.prodName").value("Laptop"));
    }

    @Test
    @DisplayName("POST /product returns 409 when name is duplicate")
    void addProduct_duplicate_returns409() throws Exception {
        when(productService.addProduct(any()))
                .thenThrow(new DuplicateProductException("Laptop"));

        var requestBody = objectMapper.writeValueAsString(
                new Product(null, "Laptop", 1500));

        mockMvc.perform(post("/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Laptop")));
    }

    // ========================================================
    //  PUT /product/{prodId}
    // ========================================================

    @Test
    @DisplayName("PUT /product/{id} returns 200 and updated product")
    void updateProduct_valid_returns200() throws Exception {
        var updated = new Product(1L, "Laptop Pro", 1800);
        when(productService.updateProduct(any())).thenReturn(updated);

        var requestBody = objectMapper.writeValueAsString(
                new Product(null, "Laptop Pro", 1800));

        mockMvc.perform(put("/product/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prodName").value("Laptop Pro"))
                .andExpect(jsonPath("$.price").value(1800));
    }

    @Test
    @DisplayName("PUT /product/{id} returns 404 when product not found")
    void updateProduct_notFound_returns404() throws Exception {
        when(productService.updateProduct(any()))
                .thenThrow(new ProductNotFoundException(99L));

        var requestBody = objectMapper.writeValueAsString(
                new Product(null, "Ghost", 0));

        mockMvc.perform(put("/product/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound());
    }

    // ========================================================
    //  DELETE /product/{prodId}
    // ========================================================

    @Test
    @DisplayName("DELETE /product/{id} returns 204 on success")
    void deleteProduct_exists_returns204() throws Exception {
        doNothing().when(productService).deleteProduct(1L);

        mockMvc.perform(delete("/product/1"))
                .andExpect(status().isNoContent());

        verify(productService).deleteProduct(1L);
    }

    @Test
    @DisplayName("DELETE /product/{id} returns 404 when not found")
    void deleteProduct_notFound_returns404() throws Exception {
        doThrow(new ProductNotFoundException(99L))
                .when(productService).deleteProduct(99L);

        mockMvc.perform(delete("/product/99"))
                .andExpect(status().isNotFound());
    }
}
