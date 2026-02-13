package com.kshem.services.homesecurity.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "file-server")
@Data
public class ConfigProperties {
  public enum StorageDriverOption{ LOCAL, AWS_S3, GCP }

  private StorageDriverOption driver;
  private String baseStoragePath = "fileStorage";

  private AwsS3 awsS3;

  @Data
  public static class AwsS3{
    private String accessKey = "";
    private String secretKey = "";
    private String region = "";
    private String bucket = "";
  }

}
