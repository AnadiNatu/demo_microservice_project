package com.microservice_demo.demo_service_1.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
//import io.jsonwebtoken.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryServiceImpl {

    private final Cloudinary cloudinary;

    private static final String PRODUCT_FOLDER = "microservice/products";

    public String uploadProductImage(MultipartFile file, Long productId) {
        validateImageFile(file);

        String publicId =  PRODUCT_FOLDER + productId + "/product_" + productId;

        log.info("[DS1 Cloudinary] Uploading product image | productId={} | filename={} | size={}KB",
                productId, file.getOriginalFilename(), file.getSize() / 1024);

        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "public_id", publicId,
                            "overwrite", true,
                            "resource_type", "image",
                            "transformation", new com.cloudinary.Transformation<>()
                                    .width(800).height(800).crop("limit")
                                    .quality("auto").fetchFormat("auto")
                    ));
            String url = (String) uploadResult.get("secure_url");
            log.info("[DS1 Cloudinary] Product image uploaded | productId={} | url={}", productId, url);
            return url;

        } catch (IOException ex) {
            log.error("[DS1 Cloudinary] Upload failed | productId={} | error={}", productId, ex.getMessage());
            throw new RuntimeException("Failed to upload product image: " + ex.getMessage(), ex);
        }
    }

    public void deleteImage(String publicId) {
        log.info("[DS1 Cloudinary] Deleting image | publicId={}", publicId);
        try {
            @SuppressWarnings("rawtypes")
            Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            String status = (String) result.get("result");
            if ("ok".equals(status)) {
                log.info("[DS1 Cloudinary] Image deleted | publicId={}", publicId);
            } else {
                log.warn("[DS1 Cloudinary] Delete returned status='{}' | publicId={}", status, publicId);
            }
        } catch (IOException ex) {
            log.error("[DS1 Cloudinary] Delete failed | publicId={} | error={}", publicId, ex.getMessage());
            throw new RuntimeException("Failed to delete image: " + ex.getMessage(), ex);
        }
    }

//   Public - ID
    public String extractPublicId(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            String[] parts = url.split("/upload/");
            if (parts.length < 2) return null;
            String afterUpload = parts[1].replaceFirst("v\\d+/", "");
            int lastDot = afterUpload.lastIndexOf('.');
            return lastDot > 0 ? afterUpload.substring(0, lastDot) : afterUpload;
        } catch (Exception ex) {
            log.error("[DS1 Cloudinary] Failed to extract publicId from URL: {}", url, ex);
            return null;
        }
    }

//    Validation
    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("File size exceeds 5 MB limit");
        }
        String contentType = file.getContentType();
        if (contentType == null
                || (!contentType.equals("image/jpeg")
                && !contentType.equals("image/png")
                && !contentType.equals("image/jpg")
                && !contentType.equals("image/gif")
                && !contentType.equals("image/webp"))) {
            throw new IllegalArgumentException(
                    "Unsupported image format. Supported: jpg, png, gif, webp");
        }
    }
}
