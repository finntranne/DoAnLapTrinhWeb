package com.alotra.service;

import java.io.IOException;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public String upload(MultipartFile file) throws IOException {
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap(
                        "resource_type", "auto"
                ));
        return (String) uploadResult.get("secure_url");
    }

    public void delete(String url) throws IOException {
        if (url == null || url.isEmpty()) return;
        String publicId = url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf("."));
        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }
}