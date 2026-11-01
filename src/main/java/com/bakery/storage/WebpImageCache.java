package com.bakery.storage;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

final class WebpImageCache {

  private WebpImageCache() {}

  static Path pathFor(Path source) {
    String key = sha256(source.getFileName().toString());
    return source.getParent().resolve(".webp-cache").resolve(key + ".max1600.webp");
  }

  private static String sha256(String value) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest(value.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }
}
