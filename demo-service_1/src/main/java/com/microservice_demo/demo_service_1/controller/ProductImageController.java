package com.microservice_demo.demo_service_1.controller;

import com.microservice_demo.demo_service_1.dto.functionality.ProductDto;
import com.microservice_demo.demo_service_1.entity.Product;
import com.microservice_demo.demo_service_1.exception.errors.ResourceNotFoundException;
import com.microservice_demo.demo_service_1.repository.ProductRepository;
import com.microservice_demo.demo_service_1.service.ProductImageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
@RestController
@RequestMapping("/api/products/{productId}/images")
@Slf4j
public class ProductImageController {

    @Autowired
    private ProductImageService productImageService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadImage(
            @PathVariable Long productId,
            @RequestParam("file") MultipartFile file) {

        System.out.println("UPLOAD CONTROLLER HIT");
        System.out.println("Product ID = " + productId);

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File must not be empty"));
        }

        try {

            String imageUrl = productImageService.uploadProductImage(productId, file);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Image uploaded successfully");
            response.put("imageUrl", imageUrl);

            return ResponseEntity.ok(response);
        } catch (IOException ex) {
            ex.printStackTrace();
            log.error("Image upload failed", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                            "error", ex.getMessage() == null ? "Unknown error" : ex.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Image upload failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage() == null ? "Unknown error" : e.getMessage()));
        }
    }

    @PutMapping("/update")
    public ResponseEntity<Map<String, String>> updateImage(
            @PathVariable Long productId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "oldImageUrl", required = false) String oldImageUrl) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File must not be empty"));
        }
        try {
            String imageUrl = productImageService.updateProductImage(productId, file, oldImageUrl);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Image updated successfully");
            response.put("imageUrl", imageUrl);

            return ResponseEntity.ok(response);

        } catch (IOException ex) {
            return ResponseEntity.internalServerError().body(Map.of("error", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.internalServerError().body(Map.of("error", ex.getMessage()));
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Map<String, String>> deleteImage(@PathVariable Long productId) {

        boolean deleted = productImageService.deleteProductImage(productId);

        if (deleted) {
            return ResponseEntity.ok(Map.of("message", "Image deleted successfully"));
        }

        return ResponseEntity.badRequest().body(Map.of("error", "Product has no image to delete."));
    }

    @GetMapping("/list")
    public ResponseEntity<String> listImages(@PathVariable Long productId) {

        String prefix = "product-" + productId + "-";

        String response = productImageService.listProductImages(prefix);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/get")
    @PreAuthorize("hasAnyRole('ROLE_USER','ROLE_ADMIN')")
    public ResponseEntity<Map<String, String>> getProductImage(@PathVariable Long productId) {

        ProductDto product = productImageService.getProduct(productId);

        Map<String, String> response = new HashMap<>();

        response.put("imageUrl", product.getImageUrl());

        return ResponseEntity.ok(response);
    }

}