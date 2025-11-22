package com.bensler.taggy.imprt;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import com.bensler.decaf.util.Pair;
import com.bensler.taggy.persist.Blob;

class FileToImport {

  public enum ImportObstacle {
    SHA_MISSING("Sha256 sum not yet computed"),
    UNSUPPORTED_TYPE("File type is not supported"),
    DUPLICATE("File already imported"),
    DUPLICATE_CHECK_MISSING("Ongoing duplicate check"),
    IMPORT_ERROR("Error on import");

    private final String humanReadableName_;

    ImportObstacle(String humanReadableName) {
      humanReadableName_ = humanReadableName;
    }

    String getHumanReadableName() {
      return humanReadableName_;
    }
  }

  private final File file_;
  private final Path relativePath_;
  private final long fileSize_;
  private String shaSum_;
  private Optional<Pair<ImportObstacle, Optional<String>>> importObstacle_;
  private String type_;
  private Blob blob_;

  FileToImport(Path basePath, File file) {
    fileSize_ = (file_ = file).length();
    relativePath_ = basePath.relativize(file_.toPath());
    type_ = List.of(file.getPath().split("\\.")).getLast();
    setImportObstacle(ImportObstacle.SHA_MISSING, null);
  }

  FileToImport(FileToImport template, String shaSum, ImportObstacle obstacle, String obstacleMsg, String type, Blob blob) {
    fileSize_ = (file_ = template.file_).length();
    relativePath_ = template.relativePath_;
    shaSum_ = shaSum;
    setImportObstacle(obstacle, obstacleMsg);
    type_ = type;
    blob_ = blob;
  }

  String getName() {
    return file_.getName();
  }

  String getRelativePath() {
    return relativePath_.toString();
  }

  File getFile() {
    return file_;
  }

  long getFileSize() {
    return fileSize_;
  }

  String getShaSum() {
    return shaSum_;
  }

  void setShaSum(String shaSum) {
    shaSum_ = shaSum;
    setImportObstacle(ImportObstacle.DUPLICATE_CHECK_MISSING, null);
  }

  void setType(String type) {
    type_ = type;
  }

  String getType() {
    return type_;
  }

  void setBlob(Blob blob) {
    blob_ = blob;
  }

  Blob getBlob() {
    return blob_;
  }

  boolean isImportable() {
    return importObstacle_.isEmpty();
  }

  boolean hasObstacle(ImportObstacle obstacle) {
    return importObstacle_.map(Pair::getLeft).equals(Optional.ofNullable(obstacle));
  }

  String getImportObstacleAsString() {
    return importObstacle_.map(pair -> pair.map((obstacle, optionalMsg) ->
      obstacle.getHumanReadableName() + optionalMsg.map(msg -> " (%s)".formatted(msg)).orElse("")
    )).orElse("");
  }

  void setImportObstacle(ImportObstacle obstacle, String additionalMsg) {
    importObstacle_ = Optional.ofNullable(obstacle).map(lObstacle -> new Pair<>(lObstacle, Optional.ofNullable(additionalMsg)));
  }

  @Override
  public int hashCode() {
    return file_.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof FileToImport file) && file_.equals(file.file_);
  }


}
