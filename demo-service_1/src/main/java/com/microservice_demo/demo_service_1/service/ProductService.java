package com.microservice_demo.demo_service_1.service;

import com.microservice_demo.demo_service_1.dto.functionality.CreateProductDto;
import com.microservice_demo.demo_service_1.dto.functionality.ProductDto;
import com.microservice_demo.demo_service_1.entity.Product;
import com.microservice_demo.demo_service_1.entity.Users;
import com.microservice_demo.demo_service_1.exception.errors.BadRequestException;
import com.microservice_demo.demo_service_1.exception.errors.ResourceNotFoundException;
import com.microservice_demo.demo_service_1.feign.DemoService2FeignClient;
import com.microservice_demo.demo_service_1.repository.ProductRepository;
import com.microservice_demo.demo_service_1.repository.UserRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final DemoService2FeignClient demoService2Client;
    private final CloudinaryServiceImpl cloudinaryService;

    @Transactional
    @CacheEvict(value = {"products" , "productPages" , "activeProducts" , "categoryProducts"} , allEntries = true)
    public ProductDto createProduct(CreateProductDto dto){
        log.info("Creating new product : {}" , dto.getProductName());

        if (dto.getSku() != null && !dto.getSku().isEmpty()){
            productRepository.findBySku(dto.getSku()).ifPresent(p -> {
                log.error("Product with SKU {} already exists" , dto.getSku());
                throw new BadRequestException("Product with SKU " + dto.getSku() + "already exists");
            });
        }

        Users createdBy = null;
        if (dto.getCreatedByUserId() != null){
            createdBy = userRepository.findById(dto.getCreatedByUserId()).orElseThrow(() -> {
                log.error("User not found : {}" , dto.getCreatedByUserId());
                return new ResourceNotFoundException("User not found");
            });

        }

        Product product = Product.builder()
                .name(dto.getProductName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .stockQuantity(dto.getStockQuantity())
                .category(dto.getCategory())
                .sku(dto.getSku())
                .createdBy(createdBy)
                .active(true)
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("Product created successfully with ID : {} " , savedProduct);

        return toDto(savedProduct);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#productId")
    public ProductDto getProduct(Long productId) {
        log.info("Fetching product with ID: {}", productId);
        Product product = productRepository.findByIdWithCreatedBy(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
        log.info("Product found: {}", product.getName());
        return toDto(product);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "productPages", key = "#page + '_' + #size + '_' + #sortBy")
    public Page<ProductDto> getAllProducts(int page, int size, String sortBy) {
        log.info("Fetching products — page={} size={} sortBy={}", page, size, sortBy);

        String safeSort = isSafeProductSortField(sortBy) ? sortBy : "createdOn";
        Pageable pageable = PageRequest.of(page, size, Sort.by(safeSort).descending());
        return productRepository.findAll(pageable).map(this::toDto);
    }

    @Cacheable(value = "activeProducts" , key = "#page + '_' + #size")
    public Page<ProductDto> getActiveProducts(int page , int size){
        log.info("Fetching active products - Page : {} , Size : {}" , page , size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdOn"));
        Page<Product> productPage = productRepository.findByActiveTrue(pageable);

        log.info("Found {} active products", productPage.getTotalElements());
        return productPage.map(this::toDto);
    }

    @Cacheable(value = "categoryProducts" , key = "#category + '_' + #page + '_' + #size")
    public Page<ProductDto> getProductsByCategory(String category , int page , int size){
        log.info("Fetching products by category : {} - Page : {} , Size : {}" , category , page , size);

        Pageable pageable = PageRequest.of(page , size , Sort.by("name"));
        Page<Product> productPage = productRepository.findByActiveTrueAndCategory(category , pageable);

        log.info("Found {} products in category : {}" , productPage.getTotalElements());
        return productPage.map(this::toDto);
    }

    public Page<ProductDto> searchProducts(String keyword, int page, int size) {
        log.info("Searching products with keyword: {} - Page: {}, Size: {}", keyword, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<Product> productPage = productRepository.searchProducts(keyword, pageable);

        log.info("Found {} products matching keyword: {}", productPage.getTotalElements(), keyword);
        return productPage.map(this::toDto);
    }

    @Transactional
    @CacheEvict(value = {"products", "productPages", "activeProducts", "categoryProducts"}, allEntries = true)
    public ProductDto updateStock(Long productId, Integer quantity) {
        log.info("Updating stock for product ID: {} - New quantity: {}", productId, quantity);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error("Product not found: {}", productId);
                    return new ResourceNotFoundException("Product not found: " + productId);
                });

        product.setStockQuantity(quantity);
        Product updated = productRepository.save(product);

        log.info("Stock updated successfully for product: {}", product.getName());
        return toDto(updated);
    }

    @Transactional
    @CacheEvict(value = {"products" , "productPages" , "activeProducts" , "categoryProducts"} , allEntries = true)
    public ProductDto deactivateProduct(Long productId){
        log.info("Deactivating product ID : {}" , productId);
        Product product = productRepository.findById(productId).orElseThrow(() -> {
            log.error("Product not found : {}" , productId);
            return new ResourceNotFoundException("Product not found");
        });

        product.setActive(false);
        Product updated = productRepository.save(product);

        log.info("Product deactivate : {}" , product.getName());
        return toDto(updated);
    }

//    Used by Demo Service 2
    @Transactional(readOnly = true)
     public List<ProductDto> getProductsByIds(List<Long> productIds){
        log.info("Fetching products by IDs : {}" , productIds);

        List<Product> products = productRepository.findByProductIdIn(productIds);
        log.info("Found {} products out of {} requested IDs" , products.size() , productIds.size());

        return products.stream().map(this::toDto).collect(Collectors.toList());
     }

     @CircuitBreaker(name = "demoService2" , fallbackMethod = "getOrderStatsFallback")
     @Retry(name = "demoService2")
     public Long getProductOrderCount(Long productId){
        log.info("Fetching order count for product ID : {} from Demo-Service 2" , productId);

        Long orderCount = demoService2Client.getProductOrderCount(productId);
        log.info("Product {} has been ordered {} times " , productId , orderCount);

        return orderCount;
    }

    private Long getOrderStatsFallback(Long productId , Exception ex){
        log.warn("Fallback triggered for getProductOrderCount - ProductID : {} , Error : {}" , productId , ex.getMessage());
        return 0L;
    }

    @Transactional
    @CacheEvict(value = {"products" , "productPages" , "activeProducts" , "categoryProducts"} , allEntries = true)
    public ProductDto uploadProductImage(Long productId , MultipartFile file){
        log.info("[DS1] Uploading image for productId={}", productId);

        Product product = productRepository.findById(productId).orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        if (product.getImageUrl() != null) {
            String oldPublicId = cloudinaryService.extractPublicId(product.getImageUrl());

            if (oldPublicId != null) {
                cloudinaryService.deleteImage(oldPublicId);
            }
        }
            String url = cloudinaryService.uploadProductImage(file, productId);
            product.setImageUrl(url);
            Product saved = productRepository.save(product);

            log.info("[DS1] Product image uploaded | productId={} | url={}", productId, url);
            return toDto(saved);
    }

    @Transactional
    @CacheEvict(value = {"products" , "productPages" , "activeProducts" , "categoryProducts"} , allEntries = true)
    public ProductDto removeProductImage(Long productId){
        log.info("[DS1] Removing image for productId={}", productId);

        Product product = productRepository.findById(productId).orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        if (product.getImageUrl() != null){
            String publicId = cloudinaryService.extractPublicId(product.getImageUrl());

            if (publicId != null){
                cloudinaryService.deleteImage(publicId);
            }
            product.setImageUrl(null);
            productRepository.save(product);

            log.info("[DS1] Product image removed | productId={}", productId);
        } else {
            log.info("[DS1] No image to remove for productId={}", productId);
        }
        return toDto(product);
    }


//    Helpers
    private ProductDto toDto(Product product) {
        Long createdByUserId = null;
        String createdByUsername = null;

        try {
            if (product.getCreatedBy() != null) {
                createdByUserId = product.getCreatedBy().getUserId();
                createdByUsername = product.getCreatedBy().getName();
            }
        } catch (Exception ex) {
            // Lazy proxy not initialised — leave fields null rather than crashing
            log.debug("Could not resolve createdBy for productId={}", product.getProductId());
        }

        return ProductDto.builder()
                .productId(product.getProductId())
                .productName(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .category(product.getCategory())
                .sku(product.getSku())
                .active(product.getActive())
                .imageUrl(product.getImageUrl())
                .createdByUserId(createdByUserId)
                .createdByUsername(createdByUsername)
                .createdOn(product.getCreatedOn())
                .updatedOn(product.getUpdatedOn())
                .build();
    }

    private boolean isSafeProductSortField(String field) {
        return field != null && List.of(
                "createdOn", "updatedOn", "name", "price",
                "stockQuantity", "category"
        ).contains(field);
    }
}
