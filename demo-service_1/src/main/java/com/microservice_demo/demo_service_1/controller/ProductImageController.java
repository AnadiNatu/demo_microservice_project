package com.microservice_demo.demo_service_1.controller;

import com.microservice_demo.demo_service_1.entity.Product;
import com.microservice_demo.demo_service_1.exception.errors.ResourceNotFoundException;
import com.microservice_demo.demo_service_1.repository.ProductRepository;
import com.microservice_demo.demo_service_1.service.ProductImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/products/{productId}/images")
public class ProductImageController {

    @Autowired
    private ProductImageService productImageService;

    @Autowired
    private ProductRepository productRepository;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadImage(
            @PathVariable Long productId,
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File must not be empty"));
        }

        try {
            String imageUrl = productImageService.uploadProductImage(productId, file);

            // added: persist the new URL on the product row — previously only returned in the response, never saved
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
            product.setImageUrl(imageUrl);
            productRepository.save(product);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Image uploaded successfully");
            response.put("imageUrl", imageUrl);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to read file: " + e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
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

            String imageUrl = productImageService.uploadProductImage(productId, file);

            // added: persist the new URL on the product row — previously only returned in the response, never saved
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
            product.setImageUrl(imageUrl);
            String newImageUrl = productImageService.updateProductImage(productId, file, oldImageUrl);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Image updated successfully");
            response.put("imageUrl", newImageUrl);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to read file: " + e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Map<String, String>> deleteImage(
            @PathVariable Long productId,
            @RequestParam("imageUrl") String imageUrl) {

        boolean deleted = productImageService.deleteProductImage(imageUrl);

        if (deleted) {
            return ResponseEntity.ok(Map.of("message", "Image deleted successfully"));
        } else {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Could not delete image. Check the imageUrl parameter."));
        }
    }

    @GetMapping("/list")
    public ResponseEntity<String> listImages(@PathVariable Long productId) {
        String prefix = "product-" + productId + "-";
        String result = productImageService.listProductImages(prefix);
        return ResponseEntity.ok(result);
    }
}
