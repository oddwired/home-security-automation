package com.kshem.services.homesecurity.services.implementation;

import com.kshem.services.homesecurity.exceptions.FileStorageException;
import com.kshem.services.homesecurity.models.FileDownload;
import com.kshem.services.homesecurity.services.StorageDriver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@ConditionalOnProperty(
    value = "file-server.driver",
    havingValue = "LOCAL",
    matchIfMissing = true
)
public class LocalStorageDriverImpl implements StorageDriver {

  @Override
  public void store(MultipartFile file, Path path, String name) {
    try {
      Files.createDirectories(path);

      file.transferTo(Paths.get(path.toString(), name));
    } catch (IOException e) {
      e.printStackTrace();
      throw new FileStorageException("Error saving file", e);
    }
  }

  @Override
  public FileDownload getFile(Path filePath, String fileName, String storedFileName) {
    File file = new File(Paths.get(filePath.toString(), storedFileName).toString());

    if(!file.exists()){
      throw new FileStorageException("File not found", HttpStatus.NOT_FOUND);
    }

    try{
      return new FileDownload(fileName, Files.size(file.toPath()),
              new BufferedInputStream(Files.newInputStream(file.toPath())));
    } catch (IOException e) {
      throw new FileStorageException("File not found", e, HttpStatus.NOT_FOUND);
    }
  }

  public void deleteFile(Path filePath, String fileName, String storedFileName){
    File file = new File(Paths.get(filePath.toString(), storedFileName).toString());

    if(!file.exists()){
      throw new FileStorageException("File not found", HttpStatus.NOT_FOUND);
    }else{
      file.delete();
    }
  }

}
