package com.bensler.taggy.ui;

import java.io.File;

public class FileToImport {

  private final File file_;
  private String shaSum_;
  private String importObstacle_;
  private String type_;

  public FileToImport(File file) {
    file_ = file;
  }

  public String getName() {
    return file_.getName();
  }

  public File getFile() {
    return file_;
  }

  public String getShaSum() {
    return shaSum_;
  }

  void setShaSum(String shaSum) {
    shaSum_ = shaSum;
  }

  void setType(String type) {
    type_ = type;
  }

  public String getType() {
    return type_;
  }

  public Boolean isImportable() {
    return (importObstacle_ == null);
  }

  public String getImportObstacle() {
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
