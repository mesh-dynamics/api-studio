package com.cubeui.backend.service;

public interface MailService {

  void sendEmail(String[] to, String subject, String content, boolean isMultipart, boolean isHtml);

}
