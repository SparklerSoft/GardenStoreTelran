package org.garden.com.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.garden.com.entity.Cart;
import org.garden.com.entity.CartItem;
import org.garden.com.entity.User;
import org.garden.com.repository.CartItemsJpaRepository;
import org.garden.com.repository.CartJpaRepository;
import org.garden.com.repository.UserJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.garden.com.entity.Product;
import org.garden.com.exceptions.ProductInvalidArgumentException;
import org.garden.com.exceptions.ProductNotFoundException;
import org.garden.com.repository.ProductJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductJpaRepository repository;

    @Autowired
    private CartJpaRepository cartJpaRepository;

    @Autowired
    private CartItemsJpaRepository itemsJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private Validator validator;

    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);

    @Override
    public Product createProduct(Product product) {
        log.info("Creating product: {}", product);
        validateProduct(product);
        Product createdProduct = repository.save(product);
        log.info("Product created: {}", createdProduct);
        return createdProduct;
    }

    @Override
    public List<Product> getFilteredProducts(Long categoryId, Double minPrice, Double maxPrice, Boolean discount, String sort) {
        log.info("Fetching filtered products. CategoryId: {}, MinPrice: {}, MaxPrice: {}, Discount: {}, Sort: {}", categoryId, minPrice, maxPrice, discount, sort);
        List<Product> products = repository.findFilteredProducts(categoryId, minPrice, maxPrice, discount, sort);
        log.info("Found {} filtered products", products.size());
        return products;
    }

    @Override
    public Product editProduct(long id, Product product) {
        log.info("Editing product with ID {}: {}", id, product);
        validateProduct(product);
        Product existingProduct = repository.findById(id).orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));

        existingProduct.setName(product.getName());
        existingProduct.setDescription(product.getDescription());
        existingProduct.setPrice(product.getPrice());
        existingProduct.setCategory(product.getCategory());
        existingProduct.setImageUrl(product.getImageUrl());
        existingProduct.setUpdatedAt(LocalDateTime.now());

        Product updatedProduct = repository.save(existingProduct);
        log.info("Product updated: {}", updatedProduct);
        return updatedProduct;
    }

    @Override
    public Product findProductById(long id) {
        log.info("Fetching product with ID: {}", id);
        Product product = repository.findById(id).orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));
        log.info("Found product: {}", product);
        return product;
    }

    @Override
    public ResponseEntity<Void> deleteProduct(long id) {
        log.info("Deleting product with ID: {}", id);
        if (repository.existsById(id)) {
            repository.deleteById(id);
            log.info("Product with ID {} deleted", id);
            return ResponseEntity.status(HttpStatus.OK).build();
        } else {
            log.warn("Product not deleted: {}", id);
            throw new ProductNotFoundException("Product not found with id: " + id);
        }
    }

    @Override
    public CartItem addProductToCart(Product product, long quantity, Long userId) {
        log.info("Addition the product with ID: {} to user's cart(user ID: {})", product, userId);
        validateProduct(product);
        User user = userJpaRepository.findById(userId).get();
        if (!cartJpaRepository.findById(user.getCart().getId()).isPresent()){
            Cart cart = new Cart();
            log.info("Creating a new cart with ID: {}", cart.getId());

            cart.setUser(userJpaRepository.findById(userId).get());
            cart.setCartItemsList(new ArrayList<>());
            CartItem cartItem = new CartItem();
            cartItem.setProduct(product);
            cartItem.setQuantity(quantity);
            cartItem.setCart(cart);
            CartItem saved = itemsJpaRepository.save(cartItem);
            cart.getCartItemsList().add(cartItem);
            cartJpaRepository.save(cart);

            log.info("Product with ID: {} added to cart", saved.getProduct().getId());
            return saved;
        }

        Cart userCart = user.getCart();
        CartItem item = new CartItem();
        item.setProduct(product);
        item.setCart(userCart);
        item.setQuantity(quantity);
        CartItem saved = itemsJpaRepository.save(item);
        userCart.getCartItemsList().add(item);

        log.info("Product with ID: {} added to cart", saved.getProduct().getId());
        return saved;

    }


    private void validateProduct(Product product) {
        log.info("Validating product: {}", product);
        Set<ConstraintViolation<Product>> violations = validator.validate(product);
        if (!violations.isEmpty()) {
            log.warn("Validation exception: {}", product);
            throw new ProductInvalidArgumentException(violations.iterator().next().getMessage());
        }
    }
}
