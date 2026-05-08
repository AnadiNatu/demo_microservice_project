package com.microservice_demo.auth_service.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import jakarta.mail.Multipart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    private static final String PROFILE_FOLDER = "microservice/profiles";
//    private static final String PRODUCT_FOLDER = "multiuser/products"; // Not needed for this microservice
//    private static final String DOCUMENT_FOLDER = "multiuser/documents"; // Not needed for this microservice

    public String uploadProfilePhoto(MultipartFile file , String userId){
        validateImageFile(file);

        String publicId = PROFILE_FOLDER + "/user_" + userId;
        log.info("[CLOUDINARY] Uploading profile photo | userId={} | filename={} | size={}KB", userId, file.getOriginalFilename(), file.getSize() / 1024);

        try{
            @SuppressWarnings("rawtypes")
            Map uploadResult = cloudinary.uploader().upload(file.getBytes()  ,
                    ObjectUtils.asMap(
                            "public_id" , publicId,
                            "overwrite" , true,
                            "resource_type" , "image",
                            "folder" , PROFILE_FOLDER,
                            "transformation" , new com.cloudinary.Transformation<>().width(400).height(400).crop("fill").gravity("face").quality("auto").fetchFormat("auto")
                    ));

            String url = (String) uploadResult.get("secure_url");
            log.info("[CLOUDINARY] Profile photo uploaded successfully | userId={} | url={}", userId, url);
            return url;
        }catch (IOException ex){
            log.error("[CLOUDINARY] Upload failed | userId = {} | error = {} " , userId , ex.getMessage());
            throw new RuntimeException("Failed to upload profile photo :- " + ex.getMessage() , ex);
        }
    }


    public void deleteImage(String publicId){
        log.info("[CLOUDINARY] Deleting image | publicId={}", publicId);

        try{
            @SuppressWarnings("rawtypes")
            Map result = cloudinary.uploader().destroy(publicId , ObjectUtils.emptyMap());
            String resultStatus = (String) result.get("result");
            if ("ok".equals(resultStatus)){
                log.info("[CLOUDINARY] Image deleted successfully | publicId={}", publicId);
            }else{
                log.info("[CLOUDINARY] Image deleted returned status: {} | publicId = {}" , resultStatus , publicId);
            }
        }catch (IOException ex){
            log.error("[CLOUDINARY] Delete failed | publicId={} | error={}", publicId, ex.getMessage(), ex);
            throw new RuntimeException("Failed to delete image: " + ex.getMessage(), ex);
        }
    }



//    Helper Methods
private void validateImageFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
        throw new IllegalArgumentException("File is empty");
    }

    if (file.getSize() > 10 * 1024 * 1024) {
        throw new IllegalArgumentException("File size exceeds 10MB limit");
    }
    String contentType;
    contentType = file.getContentType();
    if (contentType == null || !contentType.startsWith("image/")) {
        throw new IllegalArgumentException("File must be an image (jpg, png, gif, webp)");
    }
    if (!contentType.equals("image/jpeg") &&
            !contentType.equals("image/png") &&
            !contentType.equals("image/gif") &&
            !contentType.equals("image/jpeg") &&
            !contentType.equals("image/jpg") &&
            !contentType.equals("image/webp")) {
        throw new IllegalArgumentException("Unsupported image format. Supported: jpg, png, gif, webp");
    }
}

    public String extractPublicId(String url){
        if (url == null || url.isBlank()){
            return null;
        }

        try{
            String[] parts = url.split("/upload/");
            if (parts.length < 2) return null;

//            Remove optional version prefix
            String afterUpload = parts[1].replaceFirst("v\\d+/" , "");

//            Remove file extension
            int lastDot = afterUpload.lastIndexOf('.');
            return (lastDot > 0) ? afterUpload.substring(0 , lastDot) : afterUpload;
        }catch (Exception ex){
            log.error("[CLOUDINARY] Failed to extract public ID from URL : {}" , url , ex);
            return null;
        }
    }

    public String getUrl(String publicId){
        return cloudinary.url().generate(publicId);
    }
}
