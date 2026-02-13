package com.kshem.services.homesecurity.services.implementation;

import com.kshem.services.homesecurity.config.ConfigProperties;
import com.kshem.services.homesecurity.entities.FileDescriptor;
import com.kshem.services.homesecurity.exceptions.FileStorageException;
import com.kshem.services.homesecurity.models.FileDownload;
import com.kshem.services.homesecurity.repositories.FileDescriptorRepository;
import com.kshem.services.homesecurity.services.StorageDriver;
import com.kshem.services.homesecurity.services.StorageService;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StorageServiceImpl implements StorageService {

  private static final Logger log = LoggerFactory.getLogger(StorageServiceImpl.class.getSimpleName());

  private final FileDescriptorRepository fileDescriptorRepository;

  private final ConfigProperties configProperties;

  private final StorageDriver storageDriver;

  @Autowired
  public StorageServiceImpl(
      @Qualifier("fileDescriptorRepository") FileDescriptorRepository fileDescriptorRepository,
      ConfigProperties configProperties,
      StorageDriver storageDriver) {  // Ignore warning. The right bean will be autowired during boot
    this.fileDescriptorRepository = fileDescriptorRepository;
    this.configProperties = configProperties;
    this.storageDriver = storageDriver;
  }

  @Transactional
  @Override
  public FileDescriptor store(MultipartFile file) {
    String fileName = file.getOriginalFilename();
    String extension = getExtension(fileName);
    long fileSize = file.getSize();
    Date creationDate = new Date();

    FileDescriptor fileDescriptor = new FileDescriptor();
    fileDescriptor.setName(fileName);
    fileDescriptor.setExtension(extension);
    fileDescriptor.setCreateTS(creationDate);
    fileDescriptor.setSize(fileSize);

    String storageName = getStoredFileName(fileDescriptor);

    storageDriver.store(file, getStoragePath(creationDate), storageName);

    fileDescriptorRepository.save(fileDescriptor);

    return fileDescriptor;
  }

  @Override
  public FileDownload retrieve(String fileId) {
    FileDescriptor fileDescriptor = fileDescriptorRepository.findFileDescriptorByFileId(fileId);

    if(fileDescriptor == null){
      throw new FileStorageException("Invalid file id", HttpStatus.BAD_REQUEST);
    }

    return storageDriver.getFile(getStoragePath(fileDescriptor.getCreateTS()), fileDescriptor.getName(),
            getStoredFileName(fileDescriptor));
  }

  @Override
  public void deleteFile(String fileId) {
    FileDescriptor fileDescriptor = fileDescriptorRepository.findFileDescriptorByFileId(fileId);

    if(fileDescriptor == null){
      throw new FileStorageException("Invalid file id", HttpStatus.BAD_REQUEST);
    }

    try {
      storageDriver.deleteFile(getStoragePath(fileDescriptor.getCreateTS()), fileDescriptor.getName(),
              getStoredFileName(fileDescriptor));
    }catch (Exception e){
      e.printStackTrace();
    }finally {
      fileDescriptorRepository.delete(fileDescriptor);
    }

  }

  private String getStoredFileName(FileDescriptor fileDescriptor){
    String storageName;

    if(fileDescriptor.getExtension().isEmpty()){
      storageName = fileDescriptor.getFileId();
    }else{
      storageName = fileDescriptor.getFileId() + "." + fileDescriptor.getExtension();
    }

    return storageName;
  }

  public Path getStoragePath(Date creationDate) {
    String basePath = configProperties.getBaseStoragePath();

    Calendar calendar = Calendar.getInstance();
    calendar.setTime(creationDate);

    String storagePath = String.format("/%d/%d/%d", calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DATE));

    return Paths.get(basePath, storagePath);
  }

  private String getExtension(String fileName) {
    if (!fileName.contains(".")) {
      return "";
    }

    return fileName.substring(fileName.lastIndexOf('.') + 1);
  }

  @Scheduled(cron = "0 55 20 * * *")
  public void createArchive(){
    log.info("Cron Job: Creating archive");
    try {
      archive();
    } catch (Exception e) {
      log.info("Archiving failed");
      e.printStackTrace();
    }
  }

  private void archive() throws IOException {
    Date currentDate = new Date();
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
    String zipName = Paths.get(configProperties.getBaseStoragePath(),
            simpleDateFormat.format(currentDate) + "_archive.zip").toString();

    List<FileDescriptor> fileDescriptors = fileDescriptorRepository.findFileDescriptorsByCreateTSBefore(currentDate);

    FileOutputStream fos = new FileOutputStream(zipName);
    ZipOutputStream zipOut = new ZipOutputStream(fos);

    fileDescriptors.forEach((fileDescriptor -> {

      try {
        FileDownload fileDownload = retrieve(fileDescriptor.getFileId());

        ZipEntry zipEntry = new ZipEntry(fileDescriptor.getName());

        zipOut.putNextEntry(zipEntry);

        byte[] bytes = new byte[1024];
        int length;
        while((length = fileDownload.getFileInputStream().read(bytes)) >= 0) {
          zipOut.write(bytes, 0, length);
        }
        fileDownload.getFileInputStream().close();
        deleteFile(fileDescriptor.getFileId());
      } catch (Exception e) {
        e.printStackTrace();
      }


    }));

    zipOut.close();
    fos.close();
  }
}
