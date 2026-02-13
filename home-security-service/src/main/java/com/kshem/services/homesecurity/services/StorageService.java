package com.kshem.services.homesecurity.services;

import com.kshem.services.homesecurity.entities.FileDescriptor;
import com.kshem.services.homesecurity.models.FileDownload;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.Date;

public interface StorageService {
  FileDescriptor store(MultipartFile file);
  FileDownload retrieve(String fileId);
  void deleteFile(String fileId);

  Path getStoragePath(Date date);
}
