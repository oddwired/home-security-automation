package com.kshem.services.homesecurity.services.implementation;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.kshem.services.homesecurity.exceptions.FileStorageException;
import com.kshem.services.homesecurity.models.FileDownload;
import com.kshem.services.homesecurity.services.StorageDriver;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnBean(AmazonS3.class)
public class AwsS3DriverImpl implements StorageDriver {

  private final AmazonS3 s3;

  public AwsS3DriverImpl(AmazonS3 s3) {
    this.s3 = s3;
  }

  @Override
  public void store(MultipartFile file, Path path, String name) {
    try {
      ObjectMetadata metadata = new ObjectMetadata();
      metadata.setContentType(file.getContentType());
      metadata.setContentLength(file.getSize());

      s3.putObject(path.toString(), name, file.getInputStream(), metadata);
    } catch (AmazonServiceException | IOException e) {
      throw new FileStorageException("Failed to upload file to Amazon S3", e);
    }
  }

  @Override
  public FileDownload getFile(Path filePath, String fileName, String storedFileName) {

    try {
      S3Object object = s3.getObject(filePath.toString(), storedFileName);
      ObjectMetadata metadata = object.getObjectMetadata();
      ///metadata.get
      return new FileDownload(fileName, metadata.getContentLength(), object.getObjectContent());
    } catch (AmazonServiceException e) {
      throw new IllegalStateException("Failed to download file from Amazon S3", e);
    }
  }

  @Override
  public void deleteFile(Path filePath, String fileName, String storedFileName){}
}
