package com.kshem.services.homesecurity.controllers;

import com.kshem.services.homesecurity.entities.FileDescriptor;
import com.kshem.services.homesecurity.exceptions.GeneralException;
import com.kshem.services.homesecurity.repositories.FileDescriptorRepository;
import com.kshem.services.homesecurity.services.DownloadService;
import com.kshem.services.homesecurity.services.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
public class FileController {

  private Logger log = LoggerFactory.getLogger(FileController.class);

  private final StorageService storageService;
  private final DownloadService downloadService;
  private final FileDescriptorRepository fileDescriptorRepository;

  public FileController(StorageService storageService, DownloadService downloadService,
                        FileDescriptorRepository fileDescriptorRepository) {
    this.storageService = storageService;
    this.downloadService = downloadService;
    this.fileDescriptorRepository = fileDescriptorRepository;
  }

  @PostMapping("/files")
  public ResponseEntity<FileDescriptor> handleFileUpload(@RequestParam("file") MultipartFile file) {
    return ResponseEntity.accepted().body(storageService.store(file));
  }

  @GetMapping("files/{id}")
  public void downloadFile(@PathVariable String id, HttpServletRequest request, HttpServletResponse response){
    try{
      downloadService.handleDownload(storageService.retrieve(id), request, response);
    } catch (IOException e) {
      throw new GeneralException("Error handling file download", e);
    }
  }

  @GetMapping("files")
  private ResponseEntity<Page<FileDescriptor>> listFiles(
          @RequestParam(value = "offset", required = false, defaultValue = "0") int offset,
          @RequestParam(value = "limit", required = false, defaultValue = "20") int limit
  ){
    Pageable pageable = PageRequest.of(offset, limit);

    return ResponseEntity.ok(fileDescriptorRepository.findAllByOrderByIdDesc(pageable));
  }
}
