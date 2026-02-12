package org.memmcol.gridflexbackendservice.service.meter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    public String saveFile(MultipartFile file) throws IOException {
        // Ensure directory exists (idempotent)
        Path uploadPath = Paths.get(uploadDir);
        Files.createDirectories(uploadPath);

        // Generate unique filename
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String uniqueName = UUID.randomUUID().toString() + (extension != null ? "." + extension : "");

        // Save the file
        Path filePath = uploadPath.resolve(uniqueName);
        Files.write(filePath, file.getBytes());

        // Return relative path (you can also return absolute if you prefer)
        return "/uploads/" + uniqueName;
    }
}
