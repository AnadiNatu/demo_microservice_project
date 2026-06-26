package com.microservice_demo.demo_service_1.service;

import com.microservice_demo.demo_service_1.config.SupabaseConfig;
//import org.apache.http.HttpEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
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

    public String uploadProductImage(Long productId , MultipartFile file) throws IOException {
        String extension = getExtension(file.getOriginalFilename());
        String filename = "product-" +productId+ "-" + UUID.randomUUID() + "." + extension;
        String uploadUrl = supabaseConfig.getStorageBaseUrl() + "/" + filename;

        HttpHeaders headers = buildHeaders(file.getContentType());
        HttpEntity<byte[]> requestEntity = new HttpEntity<byte[]>(file.getBytes() , headers);

        ResponseEntity<String> response = restTemplate.exchange(uploadUrl , HttpMethod.POST , requestEntity , String.class);

        if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED){
            return supabaseConfig.getPublicUrl(filename);
        }
        throw new RuntimeException("Failed to upload image to Supabase . Status: " + response.getStatusCode());
    }

    public String updateProductImage(Long productId , MultipartFile file , String oldImageUrl) throws IOException {
        if (oldImageUrl != null && !oldImageUrl.isEmpty()){
            String oldFileName = extractFileNameFromUrl(oldImageUrl);
            if (oldImageUrl != null){
                deleteImageByFileName(oldFileName);
            }
        }
        return uploadProductImage(productId, file);
    }

    public boolean deleteProductImage(String imageUrl){
        if (imageUrl==null || imageUrl.isEmpty()){
            return false;
        }

        String fileName = extractFileNameFromUrl(imageUrl);
        if (fileName == null){
            return false;
        }
        return deleteImageByFileName(fileName);
    }

    private boolean deleteImageByFileName(String fileName){
        String deleteUrl = supabaseConfig.getSupabaseUrl() + "/" + fileName;

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
        String listUrl = supabaseConfig.getStorageBaseUrl() + "/storage/v1/object/list/" + supabaseConfig.getBucket();

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

}
