package com.bakery.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

  @Value("${app.upload-dir:uploads}")
  private String uploadDirectory;

  @Override
  public String store(MultipartFile file) {
    if (file == null || file.isEmpty() || !StringUtils.hasText(file.getOriginalFilename())) {
      throw new IllegalArgumentException("Tệp hình ảnh không hợp lệ.");
    }
    try {
      Path directory = Path.of(uploadDirectory).toAbsolutePath().normalize();
      Files.createDirectories(directory);
      String filename = UUID.randomUUID() + "-"
          + Path.of(file.getOriginalFilename()).getFileName();
      Files.copy(file.getInputStream(), directory.resolve(filename));
      return "/uploads/" + filename;
    } catch (IOException exception) {
      throw new IllegalStateException("Không thể lưu tệp hình ảnh.", exception);
    }
  }

  @Override
  public void delete(String filePath) {
    if (filePath == null || !filePath.startsWith("/uploads/")) {
      return;
    }
    try {
      Path directory = Path.of(uploadDirectory).toAbsolutePath().normalize();
      Path target = directory.resolve(filePath.substring("/uploads/".length())).normalize();
      if (!target.startsWith(directory)) {
        throw new IllegalArgumentException("Đường dẫn tệp không hợp lệ.");
      }
      Files.deleteIfExists(target);
    } catch (IOException exception) {
      throw new IllegalStateException("Không thể xóa tệp hình ảnh.", exception);
    }
  }
}
