package com.bensler.taggy.imprt;

import java.io.File;

class FileToImport {

  private final File file_;
  private String shaSum_;
  private String importObstacle_;
  private String type_;

  FileToImport(File file) {
    file_ = file;
  }

  FileToImport(File file, String shaSum, String importObstacle, String type) {
    file_ = file;
    shaSum_ = shaSum;
    importObstacle_ = importObstacle;
    type_ = type;
  }

  String getName() {
    return file_.getName();
  }

  File getFile() {
    return file_;
  }

  String getShaSum() {
    return shaSum_;
  }

  void setShaSum(String shaSum) {
    shaSum_ = shaSum;
  }

  void setType(String type) {
    type_ = type;
  }

  String getType() {
    return type_;
  }

  Boolean isImportable() {
    return (importObstacle_ == null);
  }

  String getImportObstacle() {
    return importObstacle_;
  }

  void setImportObstacle(String importObstacle) {
    importObstacle_ = importObstacle;
  }

  @Override
  public int hashCode() {
    return file_.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof FileToImport file) && file.file_.equals(file.file_);
  }

}
