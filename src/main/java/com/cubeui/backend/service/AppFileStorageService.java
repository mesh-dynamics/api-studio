package com.cubeui.backend.service;

import com.cubeui.backend.domain.App;
import com.cubeui.backend.domain.AppFilePath;
import com.cubeui.backend.repository.AppFilePathRepository;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class AppFileStorageService {
  @Autowired
  private AppFilePathRepository appFilePathRepository;

  public AppFilePath storeFilePath(String path, String name, App app) {
    return this.appFilePathRepository.save(
      AppFilePath.builder()
          .fileName(name)
          .app(app)
          .filePath(path)
          .build()
    );
  }

  public void updateFilePathForAppId(AppFilePath appFilePath) {
    this.appFilePathRepository.save(appFilePath);
  }

  @Transactional
  public Optional<AppFilePath> getFilePathByAppId(Long appId) {
    return this.appFilePathRepository.findByAppId(appId);
  }

  @Transactional
  public List<AppFilePath> getFilePathsForAppIds(List<Long> appIds) {
    return this.appFilePathRepository.findByAppIdIn(appIds);
  }

}
