package com.kshem.services.homesecurity.config;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {


  @Autowired
  private ConfigProperties configProperties;

  @Bean
  @ConditionalOnProperty(
      value = "file-server.driver",
      havingValue = "AWS_S3"
  )
  public AmazonS3 s3() {
    AWSCredentials awsCredentials =
        new BasicAWSCredentials(configProperties.getAwsS3().getAccessKey(),
            configProperties.getAwsS3().getSecretKey());
    return AmazonS3ClientBuilder
        .standard()
        .withRegion(configProperties.getAwsS3().getRegion())
        .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
        .build();

  }
}
