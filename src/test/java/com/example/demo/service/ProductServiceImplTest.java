package com.example.demo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl service;

    private Product laptop;

    @BeforeEach
    void setUp() {
        laptop = new Product(1L, "Laptop", 1500);
    }

    // ========================================================
    //  getProducts()
    // ========================================================

    @Test
    @DisplayName("getProducts returns all products from repository")
    void getProducts_returnsAll() {
        // given
        when(productRepository.findAll())
                .thenReturn(List.of(laptop, new Product(2L, "Mouse", 25)));

        // when
        var result = service.getProducts();

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Product::prodName)
                .containsExactly("Laptop", "Mouse");
        verify(productRepository).findAll();
    }

    @Test
    @DisplayName("getProducts returns empty list when repository is empty")
    void getProducts_empty_returnsEmptyList() {
        when(productRepository.findAll()).thenReturn(List.of());

        var result = service.getProducts();

        assertThat(result).isEmpty();
    }

    // ========================================================
    //  getProdById(Long)
    // ========================================================

    @Test
    @DisplayName("getProdById returns product when found")
    void getProdById_found_returnsProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(laptop));

        var result = service.getProdById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().prodName()).isEqualTo("Laptop");
    }

    @Test
    @DisplayName("getProdById returns empty when not found")
    void getProdById_notFound_returnsEmpty() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        var result = service.getProdById(99L);

        assertThat(result).isEmpty();
    }

    // ========================================================
    //  addProduct(Product)
    // ========================================================

    @Test
    @DisplayName("addProduct saves and returns product when name is unique")
    void addProduct_uniqueName_savesAndReturns() {
        // given: no product with this name exists
        when(productRepository.findByName("Laptop")).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenReturn(laptop);

        // when
        var result = service.addProduct(new Product(null, "Laptop", 1500));

        // then
        assertThat(result).isNotNull();
        assertThat(result.prodName()).isEqualTo("Laptop");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("addProduct throws DuplicateProductException when name already exists")
    void addProduct_duplicateName_throwsAndNeverSaves() {
        // given: a product with this name already exists
        when(productRepository.findByName("Laptop")).thenReturn(Optional.of(laptop));

        // when / then
        assertThatThrownBy(() -> service.addProduct(new Product(null, "Laptop", 1500)))
                .isInstanceOf(DuplicateProductException.class)
                .hasMessageContaining("Laptop");

        // save must never be called
        verify(productRepository, never()).save(any());
    }

    // ========================================================
    //  updateProduct(Product)
    // ========================================================

    @Test
    @DisplayName("updateProduct updates and returns product when it exists")
    void updateProduct_exists_updatesAndReturns() {
        // given
        var updated = new Product(1L, "Laptop Pro", 1800);
        when(productRepository.findById(1L)).thenReturn(Optional.of(laptop));
        when(productRepository.update(updated)).thenReturn(updated);

        // when
        var result = service.updateProduct(updated);

        // then
        assertThat(result.prodName()).isEqualTo("Laptop Pro");
        assertThat(result.price()).isEqualTo(1800);
        verify(productRepository).update(updated);
    }

    @Test
    @DisplayName("updateProduct throws ProductNotFoundException when product does not exist")
    void updateProduct_notFound_throws() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateProduct(new Product(99L, "Ghost", 0)))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("99");

        verify(productRepository, never()).update(any());
    }

    // ========================================================
    //  deleteProduct(Long)
    // ========================================================

    @Test
    @DisplayName("deleteProduct removes product when it exists")
    void deleteProduct_exists_deletes() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(laptop));

        service.deleteProduct(1L);

        verify(productRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteProduct throws ProductNotFoundException when product does not exist")
    void deleteProduct_notFound_throws() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteProduct(99L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("99");

        verify(productRepository, never()).deleteById(any());
    }
}
