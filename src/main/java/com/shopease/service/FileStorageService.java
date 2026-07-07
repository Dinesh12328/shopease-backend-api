package com.shopease.service;

import com.shopease.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Service
public class FileStorageService {
    @Value("${app.upload-dir}") private String uploadDir;
    private static final Set<String> TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    public String store(MultipartFile file) {
        if (file.isEmpty() || !TYPES.contains(file.getContentType()))
            throw new BadRequestException("Only non-empty JPEG, PNG, or WebP images are allowed");
        String original = Optional.ofNullable(file.getOriginalFilename()).orElse("image");
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')).toLowerCase() : "";
        String filename = UUID.randomUUID() + ext;
        try {
            Path directory = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(directory);
            Path target = directory.resolve(filename).normalize();
            if (!target.startsWith(directory)) throw new BadRequestException("Invalid file name");
            file.transferTo(target);
            return "/uploads/" + filename;
        } catch (IOException ex) {
            throw new BadRequestException("Could not store image");
        }
    }
}
