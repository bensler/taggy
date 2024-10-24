package com.bensler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;

public class ImageMetadataExtractor {

  public final static DateTimeFormatter dateParser = DateTimeFormatter
      .ofPattern("yyyy:MM:dd HH:mm:ss")
      .withZone(TimeZone.getDefault().toZoneId());

  public static void main(String[] args) throws IOException {
    final ImageMetadataExtractor extractor = new ImageMetadataExtractor(new File(args[0]).toPath());

    extractor.parseTree();
  }

  private final Path dataBasePath;
  private final SortedSet<String> failedFiles;
  private final Set<String> successfullFiles;

  private ImageMetadataExtractor(Path pDataBasePath) {
    dataBasePath = pDataBasePath;
    failedFiles = new TreeSet<>();
    successfullFiles = new HashSet<>();
  }

  private void parseTree() throws IOException {
    Files.walk(dataBasePath)
    .map(path -> path.toFile())
    .filter(file -> file.isFile())
    .forEach(this::extractMeta);

    if (!failedFiles.isEmpty()) {
      System.out.println(" ====================== failed files ===================== ");
      System.out.println(failedFiles.stream().collect(Collectors.joining("\n")));
    }
  }

  private void extractMeta(File file) {
    try {
      final ImageMetadata metaData = Imaging.getMetadata(file);

      if (metaData == null) {
        failedFiles.add(file.getAbsolutePath());
      } else {
        if (metaData instanceof JpegImageMetadata jpgMeta) {
          final Optional<TiffField> dateField = List.of(
            TiffTagConstants.TIFF_TAG_DATE_TIME,
            ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL,
            ExifTagConstants.EXIF_TAG_DATE_TIME_DIGITIZED
          ).stream()
          .map(tag -> Optional.ofNullable(jpgMeta.findEXIFValue(tag)))
          .flatMap(fieldValue -> fieldValue.stream())
          .findFirst();

          if (dateField.isPresent()) {
            final Instant modifiedInstant = Instant.from(dateParser.parse(dateField.get().getStringValue()));

            Files.setLastModifiedTime(file.toPath(), FileTime.from(modifiedInstant));
            successfullFiles.add(file.getAbsolutePath());
          } else {
            failedFiles.add(file.getAbsolutePath());
          }
        }
      }
    } catch (IllegalArgumentException | ImageReadException | IOException e) {
      System.out.println("exc: " + file.getAbsolutePath());
    }
  }

}
