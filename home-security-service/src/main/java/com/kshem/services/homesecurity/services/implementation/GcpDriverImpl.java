package com.kshem.services.homesecurity.services.implementation;

import com.kshem.services.homesecurity.models.FileDownload;
import com.kshem.services.homesecurity.services.StorageDriver;

import java.nio.file.Path;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnProperty(
    value = "file-server.driver",
    havingValue = "GCP"
)
public class GcpDriverImpl implements StorageDriver {

  @Override
  public void store(MultipartFile file, Path path, String name) {

  }

  @Override
  public FileDownload getFile(Path filePath, String fileName, String storedFileName) {
    return null;
  }

  @Override
  public void deleteFile(Path filePath, String fileName, String storedFileName){}

}
