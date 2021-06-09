package com.cubeui.backend.domain;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.web.multipart.MultipartFile;

public class CustomMultipartFile implements MultipartFile {

  private final byte[] data;
  private final String name;
  private final String contentType;

  public CustomMultipartFile(byte[] data, String name, String contentType) {
    this.data = data;
    this.name = name;
    this.contentType = contentType;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getOriginalFilename() {
    return name;
  }

  @Override
  public String getContentType() {
    return contentType;
  }

  @Override
  public boolean isEmpty() {
    return data == null || data.length == 0;
  }

  @Override
  public long getSize() {
    return data.length;
  }

  @Override
  public byte[] getBytes() throws IOException {
    return data;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return new ByteArrayInputStream(data);
  }

  @Override
  public void transferTo(File dest) throws IOException, IllegalStateException {
    new FileOutputStream(dest).write(data);
  }
}
