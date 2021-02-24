package com.cubeui.backend.service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.cubeui.backend.domain.App;
import com.cubeui.backend.domain.AppFilePath;
import com.cubeui.backend.domain.CustomMultipartFile;
import com.cubeui.backend.service.exception.FileStorageException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.amazonaws.services.s3.AmazonS3;
import org.springframework.web.multipart.MultipartFile;

/** TODO create a interface and do the implementation
 */
@Service
@Slf4j
public class AWSS3AppFileStorageServiceImpl {

  private static List<String> ALLOWED_FILETYPES = List.of("image/jpeg", "image/jpg", "image/png");

  @Value("${aws.bucket.name}")
  private String bucketName;
  @Value("${aws.url}")
  private String imageUrl;
  @Autowired
  private AmazonS3 amazonS3;
  @Autowired
  private AppFileStorageService appFileStorageService;


  @Async("threadPoolTaskExecutor")
  public void storeFile(MultipartFile multipartFile, App app, boolean update) {
    log.info("File Upload In Process");
    try {
      final File file = convertMultiPartFileToFile(multipartFile, app);
      String name = file.getName();
      String path = imageUrl.concat(file.getName());
      uploadFileToS3Bucket(file);
      if(update) {
        Optional<AppFilePath> appFilePath = this.appFileStorageService.getFilePathByAppId(app.getId());
        appFilePath.ifPresent(filePath -> {
          deleteFileFromS3Bucket(filePath.getFileName());
          filePath.setFileName(name);
          filePath.setFilePath(path);
          this.appFileStorageService.updateFilePathForAppId(filePath);
        });
      } else {
        this.appFileStorageService.storeFilePath(path, name, app);
      }
      file.delete();
    } catch(AmazonServiceException ex) {
      log.error("Error while uploading file to S3! " , ex.getMessage());
    }
  }

  private File convertMultiPartFileToFile(MultipartFile multipartFile, App app) {
    try {
      if(multipartFile == null) {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("appImage.jpg");
        byte[] buffer = new byte[inputStream.available()];
        inputStream.read(buffer);
        multipartFile = new CustomMultipartFile(buffer, "appImage.jpg", "image/jpg");
      }
      String fileName = Instant.now().toString() + "-" + app.getName() + "-" + app.getCustomer().getName()
          + "-image." + FilenameUtils.getExtension(multipartFile.getOriginalFilename());
      if(!ALLOWED_FILETYPES.contains(multipartFile.getContentType())) {
        log.error("supported file types are jpg, png and jpeg only" , multipartFile.getContentType());
        throw new FileStorageException("supported file types are jpg and jpeg only " + multipartFile.getContentType());
      }
      final File file = new File(fileName);
      final FileOutputStream outputStream = new FileOutputStream(file);
      outputStream.write(multipartFile.getBytes());
      outputStream.close();
      return file;
    } catch (IOException ex) {
      log.error("Error Converting the multipart-file to File! ", ex.getMessage());
      throw new FileStorageException("Error Converting the multipart-file to File! " + ex.getMessage());
    }
  }

  private void uploadFileToS3Bucket(File file) {
    final PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, file.getName(), file);
    amazonS3.putObject(putObjectRequest);
    log.info("Image uploaded");
  }

  public void deleteFileFromS3Bucket(final String fileName) {
    log.info("Deleting file with name= " + fileName);
    final DeleteObjectRequest deleteObjectRequest = new DeleteObjectRequest(bucketName, fileName);
    amazonS3.deleteObject(deleteObjectRequest);
    log.info("File deleted successfully.");
  }
}
