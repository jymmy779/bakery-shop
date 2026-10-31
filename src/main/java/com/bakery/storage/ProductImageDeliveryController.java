package com.bakery.storage;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductImageDeliveryController {
  private static final int MAX_WEBP_DIMENSION = 1600;

  private final Path uploadDirectory;

  public ProductImageDeliveryController(@Value("${app.upload-dir:uploads}") String uploadDirectory) {
    this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
  }

  @GetMapping("/uploads/{filename:.+}")
  public ResponseEntity<byte[]> getImage(@PathVariable String filename,
      @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept)
      throws IOException {
    Path source = uploadDirectory.resolve(filename).normalize();
    if (!source.startsWith(uploadDirectory) || !Files.isRegularFile(source)) {
      return ResponseEntity.notFound().build();
    }

    boolean acceptsWebp = accept != null && accept.toLowerCase(Locale.ROOT).contains("image/webp");
    byte[] body = acceptsWebp ? getOrCreateWebp(source) : Files.readAllBytes(source);
    boolean servedAsWebp = acceptsWebp && isWebp(body);
    MediaType contentType = servedAsWebp
        ? MediaType.parseMediaType("image/webp") : mediaTypeFor(source);

    return ResponseEntity.ok()
        .contentType(contentType)
        .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(365)).cachePublic().immutable())
        .header(HttpHeaders.VARY, HttpHeaders.ACCEPT)
        .body(body);
  }

  private byte[] getOrCreateWebp(Path source) throws IOException {
    Path cached = WebpImageCache.pathFor(source);
    if (Files.isRegularFile(cached)
        && Files.getLastModifiedTime(cached).compareTo(Files.getLastModifiedTime(source)) >= 0) {
      return Files.readAllBytes(cached);
    }

    BufferedImage image = ImageIO.read(source.toFile());
    if (image == null) {
      return Files.readAllBytes(source);
    }

    BufferedImage optimized = resizeToMaximum(image, MAX_WEBP_DIMENSION);
    Files.createDirectories(cached.getParent());
    Path temporary = cached.resolveSibling(UUID.randomUUID() + ".tmp");
    try {
      if (!ImageIO.write(optimized, "webp", temporary.toFile())) {
        return Files.readAllBytes(source);
      }
      Files.move(temporary, cached, StandardCopyOption.REPLACE_EXISTING);
      return Files.readAllBytes(cached);
    } finally {
      Files.deleteIfExists(temporary);
    }
  }

  private BufferedImage resizeToMaximum(BufferedImage source, int maxDimension) {
    int sourceWidth = source.getWidth();
    int sourceHeight = source.getHeight();
    int longestSide = Math.max(sourceWidth, sourceHeight);
    if (longestSide <= maxDimension) {
      return source;
    }

    double scale = (double) maxDimension / longestSide;
    int targetWidth = Math.max(1, (int) Math.round(sourceWidth * scale));
    int targetHeight = Math.max(1, (int) Math.round(sourceHeight * scale));
    int imageType = source.getColorModel().hasAlpha()
        ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
    BufferedImage resized = new BufferedImage(targetWidth, targetHeight, imageType);
    Graphics2D graphics = resized.createGraphics();
    try {
      graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
          RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
          RenderingHints.VALUE_ANTIALIAS_ON);
      graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
    } finally {
      graphics.dispose();
    }
    return resized;
  }

  private boolean isWebp(byte[] body) {
    return body.length >= 12 && body[0] == 'R' && body[1] == 'I'
        && body[2] == 'F' && body[3] == 'F' && body[8] == 'W'
        && body[9] == 'E' && body[10] == 'B' && body[11] == 'P';
  }

  private MediaType mediaTypeFor(Path source) {
    try {
      String contentType = Files.probeContentType(source);
      return contentType == null ? MediaType.APPLICATION_OCTET_STREAM
          : MediaType.parseMediaType(contentType);
    } catch (IOException | IllegalArgumentException exception) {
      return MediaType.APPLICATION_OCTET_STREAM;
    }
  }
}
