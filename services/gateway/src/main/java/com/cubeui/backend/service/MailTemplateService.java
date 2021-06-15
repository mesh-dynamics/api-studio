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

import static com.cubeui.backend.security.Constants.DEFAULT_LANGUAGE;

import com.cubeui.backend.domain.TimeLineData;
import com.cubeui.backend.domain.User;
import com.cubeui.backend.service.utils.Utils;
import io.md.dao.Replay;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

@Slf4j
@Service
@Transactional
public class MailTemplateService {

    @Autowired
    private SpringTemplateEngine templateEngine;
    @Autowired
    private MessageSource messageSource;
    @Autowired
    private SendGridMailService sendGridMailService;

    @Value("${spring.mail.frontend.baseUrl}")
    private String baseUrl;

    @Value("${spring.mail.frontend.activationEndpoint}")
    private String activationEndpoint;

    @Value("${spring.mail.frontend.resetEndpoint}")
    private String resetEndpoint;

    @Async("threadPoolTaskExecutor")
    /* Send email from template
     * Used for multiple types of mails.
     * Email Id is passed separately since the receiver email address might be different from
     * the user's email id; like in case of sending admin mail on activating a user.
     */
    public void sendEmailFromTemplate(User user, String emailId, String templateName, String titleKey, String... values) {
        Locale locale = Locale.forLanguageTag(DEFAULT_LANGUAGE);
        Context context = new Context();
        context.setVariable("user", user);
        context.setVariable("baseUrl", baseUrl);
        context.setVariable("loginUrl", baseUrl);
        context.setVariable("resetUrl", baseUrl + resetEndpoint);
        context.setVariable("activationUrl", baseUrl + activationEndpoint);
        if(values.length > 0 ) {
            context.setVariable("domain", values[0]);
        }
        String content = templateEngine.process(templateName, context);
        String subject = messageSource.getMessage(titleKey, null, locale);
        sendGridMailService.sendEmail(new String[]{emailId}, subject, content, true, true);
    }

    @Async("threadPoolTaskExecutor")
    public void sendTestReportTemplate(Replay replay, TimeLineData timeLineData, List<String> emails) {
        Locale locale = Locale.forLanguageTag(DEFAULT_LANGUAGE);
        Context context = new Context();
        context.setVariable("replay", replay);
        context.setVariable("timeLineData", timeLineData);
        String content = templateEngine.process("testReportEmail", context);
        String subject = messageSource.getMessage("email.test.report", null, locale);
        sendGridMailService.sendEmail(emails.toArray(new String[0]), subject, content, true, true);
    }

    @Async("threadPoolTaskExecutor")
    public void sendActivationEmail(User user) {
        log.debug("Sending activation email to '{}'", user.getUsername());
        sendEmailFromTemplate(user, user.getUsername(), "activationEmail", "email.activation.title");
    }

    @Async
    public void sendCreationEmail(User user) {
        log.debug("Sending creation email to '{}'", user.getUsername());
        sendEmailFromTemplate(user, user.getUsername(), "creationEmail", "email.creation.title");
    }

    @Async("threadPoolTaskExecutor")
    public void sendCreationEmailAdmin(User user) {
        String adminEmail = user.getCustomer().getEmail();
        String domain = Utils.getDomainFromEmail(user.getUsername());
        log.debug("Sending creation notification email to admin at '{}'", adminEmail);
        sendEmailFromTemplate(user, adminEmail, "creationEmailAdmin", "email.creation.title", domain);
    }

    @Async("threadPoolTaskExecutor")
    public void sendPasswordResetMail(User user) {
        log.debug("Sending password reset email to '{}'", user.getUsername());
        sendEmailFromTemplate(user, user.getUsername(), "passwordResetEmail", "email.reset.title");
    }

  @Async("threadPoolTaskExecutor")
  public void sendPasswordResetSuccessfulMail(User user) {
    log.debug("Sending password reset successful email to '{}'", user.getUsername());
    sendEmailFromTemplate(user, user.getUsername(), "passwordResetSuccessfulEmail", "email.reset.success.title");
  }
}
