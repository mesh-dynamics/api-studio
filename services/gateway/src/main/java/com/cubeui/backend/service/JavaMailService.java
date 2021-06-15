/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cubeui.backend.service;

import javax.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.CharEncoding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class JavaMailService implements MailService {

  @Autowired
  private JavaMailSender javaMailSender;

  @Value("${spring.mail.emailsender}")
  private String emailSender;

  @Async("threadPoolTaskExecutor")
  public void sendEmail() {
    SimpleMailMessage msg = new SimpleMailMessage();
    msg.setTo("abc@gmail.com", "xyz@gmail.com", "def@yahoo.com");
    msg.setSubject("Test email from CubeIO");
    msg.setText("Hello World! \n Regards:\n CubeIO");
    msg.setFrom(emailSender);
    javaMailSender.send(msg);
  }

  @Async("threadPoolTaskExecutor")
  public void sendEmail(String[] to, String subject, String content, boolean isMultipart, boolean isHtml) {
    log.debug("Send email [multipart '{}' and html '{}'] to '{}' with subject '{}' and content={}",
        isMultipart, isHtml, to, subject, content);
    MimeMessage mimeMessage = javaMailSender.createMimeMessage();
    try {
      MimeMessageHelper message = new MimeMessageHelper(mimeMessage, isMultipart, CharEncoding.UTF_8);
      message.setTo(to);
      message.setFrom(emailSender);
      message.setSubject(subject);
      message.setText(content, isHtml);
      javaMailSender.send(mimeMessage);
      log.debug("Email sent to User '{}'", to);
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.warn("Email could not be sent to user '{}'", to, e);
      } else {
        log.warn("Email could not be sent to user '{}': {}", to, e.getMessage());
      }
    }
  }
}
