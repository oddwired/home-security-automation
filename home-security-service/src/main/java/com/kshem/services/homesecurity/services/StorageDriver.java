package com.kshem.services.homesecurity.services;

import java.io.InputStream;
import java.nio.file.Path;

import com.kshem.services.homesecurity.models.FileDownload;
import org.springframework.web.multipart.MultipartFile;

public interface StorageDriver {

  /**
   * @param file The file to be stored
   * @param path Path in the storage to save file to
   * @param name Name of the file in the storage
   *
   * @return Success or fail
   * */
  public void store(MultipartFile file, Path path, String name);

  public FileDownload getFile(Path filePath, String fileName, String storedFileName);

  public void deleteFile(Path filePath, String fileName, String storedFileName);
}
