package com.kshem.services.homesecurity.models;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.InputStream;

@Data
@AllArgsConstructor
public class FileDownload {
    private String name;
    private long size;
    private InputStream fileInputStream;
}
