package com.bensler.taggy.ui;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;


class BlobControllerTest {

  private static final byte[] SAMPLE_CONTENT = "LaLiLu".getBytes(Charset.forName("UTF-8"));

  private static final String SAMPLE_CONNTENT_HASH = "4615d7bd8fc925019dccaecd53696ab508f584a928737d74429507aab879dc78";

  @TempDir
  private File tmpDir;

  @Test
  void createBlobController_happyCase() {
    assertDoesNotThrow(() -> new BlobController(tmpDir, new int[] {2, 2, 2}));
  }

  @Test
  void createBlobController_tmpDirDoesNotExist() {
    final IllegalArgumentException iae = assertThrows(IllegalArgumentException.class, () ->
      new BlobController(new File(tmpDir, "subDir"), new int[] {2, 2, 2})
    );

    final String iaeMessage = iae.getMessage();
    assertTrue(iaeMessage.contains("Folder "));
    assertTrue(iaeMessage.contains(tmpDir.getName()));
    assertTrue(iaeMessage.contains(" does not exist."));
  }

  @Test
  void hashBlob() throws NoSuchAlgorithmException, IOException {
    final File file = new File(tmpDir, "SampleContent.bin");
    Files.copy(new ByteArrayInputStream(SAMPLE_CONTENT), file.toPath());

    final String hash = new BlobController(tmpDir, new int[] {2, 2, 2}).hashFile(file);

    assertEquals(SAMPLE_CONNTENT_HASH, hash);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void storeFile(boolean keepSource) throws NoSuchAlgorithmException, IOException {
    final File sourceFile = new File(tmpDir, "sourceFile.dat");
    Files.write(sourceFile.toPath(), SAMPLE_CONTENT);
    final BlobController uut = new BlobController(tmpDir, new int[] {2, 2, 2});
    // act
    final String sourceHash = uut.storeBlob(sourceFile, keepSource);
    final File storedBlob = uut.getFile(sourceHash);
    // assert
    assertEquals(SAMPLE_CONNTENT_HASH, sourceHash);
    assertTrue(keepSource == sourceFile.exists());
    assertTrue(storedBlob.exists());
    assertEquals(SAMPLE_CONNTENT_HASH, storedBlob.getName());
    // first letters of hash String --------------------------------------------------------------vv-vv-vv
    // pattern representation --------------------------------------------------------------v-v-v
    final File expectedFolder = new File(tmpDir.getAbsolutePath() + File.separator + "blobs-2-2-2/46/15/d7");
    assertEquals(expectedFolder, storedBlob.getParentFile());
    try (InputStream is = new FileInputStream(storedBlob)) {
      assertTrue(Arrays.equals(is.readAllBytes(), SAMPLE_CONTENT));
    }
  }

  @Test
  void createBlobController_tmpDirIsNotADir() throws IOException {
    final File tmpDirFile = new File(tmpDir, "bla.dat");

    tmpDirFile.createNewFile();
    final IllegalArgumentException iae = assertThrows(IllegalArgumentException.class, () ->
      new BlobController(tmpDirFile, new int[] {2, 2, 2})
    );

    final String iaeMessage = iae.getMessage();
    assertTrue(iaeMessage.contains(tmpDirFile.getName()));
    assertTrue(iaeMessage.contains(" must be a folder."));
  }

  @Test
  void createBlobController_folderPathComponentMustBeLargerThanZero() {
    final IllegalArgumentException iae = assertThrows(IllegalArgumentException.class, () ->
      new BlobController(tmpDir, new int[] {1, 0, 2})
    );

    assertEquals("All folderPattern elements must be larger than 1. \"0\" violates that.", iae.getMessage());
  }

  @Test
  void createBlobController_sumOfFolderPathComponentsTooLarge() {
    final IllegalArgumentException iae = assertThrows(IllegalArgumentException.class, () ->
      new BlobController(tmpDir, new int[] {1, 2, 4, 8, 16, 32, 5})
    );

    assertEquals("Sum of folderPattern array is 68 but must be <=64.", iae.getMessage());
  }

}
