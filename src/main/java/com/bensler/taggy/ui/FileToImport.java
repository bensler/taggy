package com.bensler.taggy.ui;

import java.io.File;

public class FileToImport {

  private final File file_;

  public FileToImport(File file) {
    file_ = file;
  }

  public String getName() {
    return file_.getName();
  }

  public File getFile() {
    return file_;
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
