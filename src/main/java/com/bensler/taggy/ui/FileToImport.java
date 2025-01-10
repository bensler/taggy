package com.bensler.taggy.ui;

import java.io.File;
import java.util.Optional;

public class FileToImport {

  private final File file_;
  private String shaSum_;
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

  FileToImport setType(Optional<String> type) {
    type_ = type.orElse(null);
    return this;
  }

  public String getType() {
    return type_;
  }

  @Override
  public int hashCode() {
    return file_.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof FileToImport file) && file.file_.equals(obj);
  }

}
