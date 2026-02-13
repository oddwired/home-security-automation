package com.kshem.services.homesecurity.services.implementation;

import com.kshem.services.homesecurity.models.FileDownload;
import com.kshem.services.homesecurity.services.DownloadService;
import com.kshem.services.homesecurity.utils.HttpUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLConnection;

@Service
public class DownloadServiceImpl implements DownloadService {

  @Override
  public void handleDownload(FileDownload fileDownload, HttpServletRequest request, HttpServletResponse response)
          throws IOException {
    String disposition = "inline";
    String contentType = probeContentType(fileDownload.getName());

    if(!contentType.startsWith("image")){
      String acceptHeader = request.getHeader("Accept");
      if(acceptHeader == null || !HttpUtils.accepts(acceptHeader, contentType)){
        disposition = "attachment";
      }
    }

    response.setContentType(contentType);
    response.setContentLength((int) fileDownload.getSize());
    response.setHeader("Content-Disposition", disposition + ";filename=\"" + fileDownload.getName() + "\"");

    FileCopyUtils.copy(fileDownload.getFileInputStream(), response.getOutputStream());
  }

  private String probeContentType(String name){
    String mimeType = URLConnection.guessContentTypeFromName(name);

    if(mimeType != null){
      return mimeType;
    }

    return "application/octet-stream";
  }
}
