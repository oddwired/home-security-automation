package com.kshem.services.homesecurity.services;

import com.kshem.services.homesecurity.models.FileDownload;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface DownloadService {
  void handleDownload(FileDownload fileDownload, HttpServletRequest request,
                      HttpServletResponse response) throws IOException;
}
