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

import com.sendgrid.Content;
import com.sendgrid.Email;
import com.sendgrid.Mail;
import com.sendgrid.Method;
import com.sendgrid.Personalization;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class SendGridMailService implements MailService {

  @Value("${spring.mail.emailsender}")
  private String emailSender;
  @Value("${sendgrid.api.key}")
  private String apiKey;

  @Async("threadPoolTaskExecutor")
  public void sendEmail(String[] to, String subject, String content, boolean isMultipart, boolean isHtml) {
    log.debug("Send email [multipart '{}' and html '{}'] to '{}' with subject '{}' and content={}",
        isMultipart, isHtml, to, subject, content);
    Email from = new Email(emailSender);
    Personalization personalization = new Personalization();
    for(String em: to) {
      personalization.addTo(new Email(em));
    }
    String type = isHtml ? "text/html" : "text/plain";
    Content contentObject = new Content(type, content);
    Mail mail = new Mail();
    mail.setFrom(from);
    mail.setSubject(subject);
    mail.addPersonalization(personalization);
    mail.addContent(contentObject);
    SendGrid sg = new SendGrid(apiKey);
    Request request = new Request();
    try {
      request.setMethod(Method.POST);
      request.setEndpoint("mail/send");
      request.setBody(mail.build());
      Response response = sg.api(request);
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
