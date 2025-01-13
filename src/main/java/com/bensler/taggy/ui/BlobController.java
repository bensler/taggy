package com.bensler.taggy.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.bensler.taggy.App;
import com.bensler.taggy.persist.Blob;
import com.bensler.taggy.persist.DbAccess;

public class BlobController {

  public static final String BLOB_FOLDER_BASE_NAME = "blobs";
  public static final String DIGEST_TYPE_SHA_256 = "SHA-256";

  private final List<Fragment> pathFragments_;
  private final File blobBasePath_;
  private final MessageDigest digest_;
  private final byte[] buffer_;

  public BlobController(File blobBasePath, int[] folderPattern) throws NoSuchAlgorithmException {
    buffer_ = new byte[1_000_000];
    digest_ = MessageDigest.getInstance(DIGEST_TYPE_SHA_256);
    final int digestStringLength = digest_.getDigestLength() * 2;

    IntStream.of(folderPattern).forEach(anInt -> {
      if (anInt < 1) {
        throw new IllegalArgumentException("All folderPattern elements must be larger than 1. \"%s\" violates that.".formatted(anInt));
      }
    });
    final int folderPatternSum = IntStream.of(folderPattern).sum();
    if (folderPatternSum > digestStringLength) {
      throw new IllegalArgumentException("Sum of folderPattern array is %s but must be <=%s.".formatted(folderPatternSum, digestStringLength));
    }
    if (!blobBasePath.exists()) {
      throw new IllegalArgumentException("Folder \"%s\" does not exist.".formatted(blobBasePath));
    }
    if (!blobBasePath.isDirectory()) {
      throw new IllegalArgumentException("\"%s\" must be a folder.".formatted(blobBasePath));
    }

    final List<Fragment> fragments = new ArrayList<>();
    int start = 0;
    for (int patternComponent : folderPattern) {
      final int end = start + patternComponent;

      fragments.add(new Fragment(start, end));
      start = end;
    }
    pathFragments_ = List.copyOf(fragments);
    final String patternSuffix = IntStream.of(folderPattern).mapToObj(String::valueOf).collect(Collectors.joining("-"));
    blobBasePath_ = new File(blobBasePath, String.join("-", BLOB_FOLDER_BASE_NAME, patternSuffix));
    blobBasePath.mkdirs();
  }

  /** @return the sourceFiles sha256sum */
  public String storeBlob(File sourceFile, boolean keepSource) throws IOException {
    final String sourceHash  = hashBlob(new FileInputStream(sourceFile));

    storeBlob(sourceFile, sourceHash, keepSource);
    return sourceHash;
  }

  public void deleteBlob(Blob blob) {
    final App app = App.getApp();
    final DbAccess dbAccess = app.getDbAccess();

    Optional.ofNullable(blob.getSha256sum()).ifPresent(this::deleteFile);
    Optional.ofNullable(blob.getThumbnailSha()).ifPresent(this::deleteFile);
    dbAccess.remove(blob);
    blob.getTags().stream().forEach(dbAccess::refresh);
    app.getMainFrame().blobRemoved(blob);
  }

  private void deleteFile(String shasum) {
    try {
      Files.deleteIfExists(getFile(shasum).toPath());
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private void storeBlob(File sourceFile, String sourceFileHash, boolean keepSource) throws IOException {
    final File targetFile = getFile(sourceFileHash);

    if (keepSource) {
      Files.copy(sourceFile.toPath(), targetFile.toPath());
    } else {
      Files.move(sourceFile.toPath(), targetFile.toPath());
    }
  }

  String hashBlob(InputStream is) throws IOException {
    final byte[] shaSum;

    synchronized (digest_) {
      try (is) {
        int bytesRead;

        while ((bytesRead = is.read(buffer_)) > -1) {
          digest_.update(buffer_, 0, bytesRead);
        }
      }
      shaSum = digest_.digest();
    }

    final BigInteger shaSumBigInteger = new BigInteger(1, shaSum);
    // shaSum hex String
    return String.format("%0" + (shaSum.length << 1) + "x", shaSumBigInteger).toLowerCase();
  }

  public File getFile(String hash) {
    final List<String> pathFragments = fragmentString(hash);

    File targetDir = blobBasePath_;
    for (String pathFragment : pathFragments) {
      targetDir = new File(targetDir, pathFragment);
    }
    targetDir.mkdirs();
    return new File(targetDir, hash);
  }

  private List<String> fragmentString(String hash) {
    return pathFragments_.stream().map(fragment -> fragment.apply(hash)).toList();
  }

  static class Fragment {

    final int start_, endExcl_;

    Fragment(int start, int endExcl) {
      start_ = start;
      endExcl_ = endExcl;
    }

    String apply(String source) {
      return source.substring(start_, endExcl_);
    }

  }

}
