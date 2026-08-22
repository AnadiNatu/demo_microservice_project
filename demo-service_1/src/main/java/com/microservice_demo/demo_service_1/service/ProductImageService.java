package com.microservice_demo.demo_service_1.service;

import com.microservice_demo.demo_service_1.aop.StopWatch;
import com.microservice_demo.demo_service_1.config.SupabaseConfig;
//import org.apache.http.HttpEntity;
import com.microservice_demo.demo_service_1.dto.functionality.ProductDto;
import com.microservice_demo.demo_service_1.entity.Product;
import com.microservice_demo.demo_service_1.exception.errors.ResourceNotFoundException;
import com.microservice_demo.demo_service_1.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.apache.commons.io.FilenameUtils.getExtension;

@Service
public class ProductImageService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private SupabaseConfig supabaseConfig;

    @Autowired
    private ProductRepository productRepository;

    public String uploadProductImage(Long productId, MultipartFile file) throws IOException {
        System.out.println("UPLOAD SERVICE START");
        System.out.println("Product Id : " + productId);
        Product product = productRepository.findById(productId).orElseThrow(() -> new RuntimeException("Product not found : " + productId));
        System.out.println(product);
        String extension = getExtension(file.getOriginalFilename());
//        String filename = "product-" + productId + "-" + UUID.randomUUID() + "." + extension;
        String filename = "products/" + productId + "/" + UUID.randomUUID() + "." + extension;
        String uploadUrl = supabaseConfig.getStorageBaseUrl() + "/" + filename;
        // Upload endpoint (NOT public)
//        String uploadUrl = supabaseConfig.getStorageBaseUrl() + "/" + filename;
        HttpHeaders headers = buildHeaders(file.getContentType());

        HttpEntity<byte[]> request = new HttpEntity<>(file.getBytes(), headers);

        System.out.println("Filename = " + filename);
        System.out.println("Upload URL = " + uploadUrl);

//        ResponseEntity<String> response = restTemplate.exchange(uploadUrl, HttpMethod.POST, request, String.class);
        try{
            ResponseEntity<String> response= restTemplate.exchange(uploadUrl, HttpMethod.POST, request,String.class);
            System.out.println(response.getStatusCode());
            System.out.println(response.getBody());
        }
        catch(HttpClientErrorException ex){
            System.out.println(ex.getStatusCode());
            System.out.println(ex.getResponseBodyAsString());
            throw ex;
        }

//        String publicUrl = supabaseConfig.getPublicUrl(filename);
        String publicUrl = supabaseConfig.getPublicUrl(filename);
        product.setImageUrl(publicUrl);
        productRepository.save(product);

        return publicUrl;
    }


    @StopWatch
    @Transactional
    public String updateProductImage(Long productId, MultipartFile file)
            throws IOException {
        Product product = productRepository.findById(productId).orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        String oldUrl = product.getImageUrl();

        if (oldUrl != null && !oldUrl.isBlank()) {

            String oldFileName = extractFileNameFromUrl(oldUrl);
            if (oldFileName != null) {
                deleteImageByFileName(oldFileName);
            }
        }

        return uploadProductImage(productId, file);
    }

    public boolean deleteProductImage(Long productId) {

        Product product =
                productRepository.findById(productId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Product not found"));

        String imageUrl = product.getImageUrl();

        if (imageUrl == null ||
                imageUrl.isBlank()) {
            return false;
        }

        String fileName =
                extractFileNameFromUrl(imageUrl);

        if (fileName == null) {
            return false;
        }

        boolean deleted = deleteImageByFileName(fileName);

        if (deleted) {
            product.setImageUrl(null);
            productRepository.save(product);
        }

        return deleted;
    }

    private boolean deleteImageByFileName(String fileName){
        String deleteUrl = supabaseConfig.getStorageBaseUrl() + "/" + fileName;

        HttpHeaders headers = new HttpHeaders();
        headers.set("apiKey" , supabaseConfig.getSupabaseApiKey());
        headers.set("Authorization" , "Bearer " + supabaseConfig.getSupabaseApiKey());

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try{
            ResponseEntity<String> response = restTemplate.exchange(deleteUrl , HttpMethod.DELETE , requestEntity , String.class);
            return (response.getStatusCode() == HttpStatus.OK) || (response.getStatusCode() == HttpStatus.NO_CONTENT);

        }catch (HttpClientErrorException ex){
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND){
                return true;
            }

            throw new RuntimeException("Failed to delete");
        }
    }

    public String getProductImageUrl(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }
        return supabaseConfig.getPublicUrl(fileName);
    }

    public String listProductImages(String prefix){
//        String listUrl = supabaseConfig.getStorageBaseUrl() + "/storage/v1/object/list/" + supabaseConfig.getBucket();
        String listUrl = supabaseConfig.getSupabaseUrl()
                        +"/storage/v1/object/list/"
                        + supabaseConfig.getBucket();

        HttpHeaders headers = new HttpHeaders();
        headers.set("apiKey" , supabaseConfig.getSupabaseApiKey());
        headers.set("Authorization" , "Bearer " + supabaseConfig.getSupabaseApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String , Object> body = new HashMap<>();

        body.put("limit", 100);
        body.put("offset", 0);
        if (prefix != null && !prefix.isEmpty()) {
            body.put("prefix", prefix);
        }

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                listUrl,
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        return response.getBody();
    }

//    Helper methods
private HttpHeaders buildHeaders(String contentType) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("apikey", supabaseConfig.getSupabaseApiKey());
    headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseApiKey());
    headers.set("x-upsert", "true");
    headers.setContentType(
            contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM
    );
    return headers;
}

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "jpg";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private String extractFileNameFromUrl(String imageUrl) {
        // URL format: https://xxx.supabase.co/storage/v1/object/public/bucket-name/filename.ext
        String marker = supabaseConfig.getBucket() + "/";
        int index = imageUrl.indexOf(marker);
        if (index == -1) return null;
        return imageUrl.substring(index + marker.length());
    }

    public ProductDto getProduct(Long productId) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new RuntimeException("Product not found with id: " + productId));

        return ProductDto.builder()
                .productId(product.getProductId())
//                .productName(product.getProduct())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .category(product.getCategory())
                .imageUrl(product.getImageUrl())
                .build();
    }
}
