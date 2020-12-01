package com.cubeui.backend.service;

import com.cubeui.backend.domain.App;
import com.cubeui.backend.domain.AppFile;
import com.cubeui.backend.domain.CustomMultipartFile;
import com.cubeui.backend.repository.AppFileRepository;
import com.cubeui.backend.service.exception.FileRetrievalException;
import com.cubeui.backend.service.exception.FileStorageException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class AppFileStorageService {

  private static List<String> ALLOWED_FILETYPES = List.of("image/jpeg", "image/jpg", "image/png");
  @Autowired
  private AppFileRepository appFileRepository;

  public AppFile storeFile(MultipartFile multipartFile, App app) {
    try {
      if(multipartFile == null) {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("appImage.jpg");
        byte[] buffer = new byte[inputStream.available()];
        inputStream.read(buffer);
        multipartFile = new CustomMultipartFile(buffer, "appImage.jpg", "image/jpg");
      }

      String fileName = app.getName() + "-image." + FilenameUtils.getExtension(multipartFile.getOriginalFilename());

      if(!ALLOWED_FILETYPES.contains(multipartFile.getContentType())) {
        log.error("supported file types are jpg and jpeg only" , multipartFile.getContentType());
        throw new FileStorageException("supported file types are jpg and jpeg only " + multipartFile.getContentType());
      }

      return this.appFileRepository.save(
          AppFile.builder()
              .fileName(fileName)
              .fileType(multipartFile.getContentType())
              .data(compressBytes(multipartFile.getBytes()))
              .app(app).build());

    } catch (IOException ex) {
      log.error("Could not store multipartFile. Please try again!", ex.getMessage());
      throw new FileStorageException("Could not store multipartFile. Please try again! " + ex.getMessage());
    }
  }

  @Transactional
  public Optional<AppFile> getFileByAppId(Long appId) {
    final Optional<AppFile> retrievedAppFile = this.appFileRepository.findByAppId(appId);
    return retrievedAppFile.map(appFile ->
       AppFile.builder().fileName(appFile.getFileName())
        .fileType(appFile.getFileType())
        .data(decompressBytes(appFile.getData()))
        .app(appFile.getApp()).build()
    );
  }

  @Transactional
  public void deleteFileByAppId(Long appId) {
    this.appFileRepository.findByAppId(appId).ifPresent(appFile -> this.appFileRepository.delete(appFile));
  }

  @Transactional
  public List<AppFile> getFilesFoAppIds(List<Long> appIds) {
    List<AppFile> appFilesList = this.appFileRepository.findByAppIdIn(appIds);
    List<AppFile> appFiles = new ArrayList<>();
    appFilesList.forEach(appFile -> {
      appFiles.add(AppFile.builder().fileName(appFile.getFileName())
          .fileType(appFile.getFileType())
          .data(decompressBytes(appFile.getData()))
          .app(appFile.getApp()).build());
    });
    return appFiles;
  }

  public static byte[] compressBytes(byte[] data) throws IOException {
    Deflater deflater = new Deflater();
    deflater.setInput(data);
    deflater.finish();
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
    byte[] buffer = new byte[1024];
    while (!deflater.finished()) {
      int count = deflater.deflate(buffer);
      outputStream.write(buffer, 0, count);
    }
    outputStream.close();
    return outputStream.toByteArray();
  }

// uncompress the image bytes before returning it
  public static byte[] decompressBytes(byte[] data) {
    Inflater inflater = new Inflater();
    inflater.setInput(data);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
    byte[] buffer = new byte[1024];
    try {
      while (!inflater.finished()) {
        int count = inflater.inflate(buffer);
        outputStream.write(buffer, 0, count);
      }
      outputStream.close();
    } catch (IOException | DataFormatException ex) {
      log.error("Error while decompressing the file ", ex.getMessage());
      throw new FileRetrievalException("Error while decompressing the file " + ex.getMessage());
    }
    return outputStream.toByteArray();
  }

}
