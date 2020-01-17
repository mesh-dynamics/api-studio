package com.cubeui.backend.service;

import com.cubeui.backend.domain.User;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.spring5.SpringTemplateEngine;

import org.thymeleaf.context.Context;
import org.apache.commons.lang3.CharEncoding;
import lombok.extern.slf4j.Slf4j;

import javax.mail.internet.MimeMessage;
import java.util.Locale;

import static com.cubeui.backend.security.Constants.DEFAULT_LANGUAGE;

@Slf4j
@Service
@Transactional
public class MailService {

    private final JavaMailSender javaMailSender;
    private final MessageSource messageSource;
    private final SpringTemplateEngine templateEngine;

    @Value("${spring.mail.frontend.baseUrl}")
    private String baseUrl;

    @Value("${spring.mail.frontend.activationEndpoint}")
    private String activationEndpoint;

    @Value("${spring.mail.frontend.loginEndpoint}")
    private String loginEndpoint;

    @Value("${spring.mail.frontend.resetEndpoint}")
    private String resetEndpoint;

    @Value("${spring.mail.username}")
    private String emailSender;

    public MailService(JavaMailSender javaMailSender, MessageSource messageSource, SpringTemplateEngine templateEngine) {
        this.javaMailSender = javaMailSender;
        this.messageSource = messageSource;
        this.templateEngine = templateEngine;
    }

    @Async
    public void sendEmail() {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo("abc@gmail.com", "xyz@gmail.com", "def@yahoo.com");
        msg.setSubject("Test email from CubeIO");
        msg.setText("Hello World! \n Regards:\n CubeIO");
        msg.setFrom(emailSender);
        javaMailSender.send(msg);
    }

    @Async
    public void sendEmail(String to, String subject, String content, boolean isMultipart, boolean isHtml) {
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

    @Async
    public void sendEmailFromTemplate(User user, String emailId, String templateName, String titleKey) {
        Locale locale = Locale.forLanguageTag(DEFAULT_LANGUAGE);
        Context context = new Context();
        context.setVariable("user", user);
        context.setVariable("baseUrl", baseUrl);
        context.setVariable("loginUrl", baseUrl + loginEndpoint);
        context.setVariable("resetUrl", baseUrl + resetEndpoint);
        context.setVariable("activationUrl", baseUrl + activationEndpoint);
        String content = templateEngine.process(templateName, context);
        String subject = messageSource.getMessage(titleKey, null, locale);
        sendEmail(emailId, subject, content, true, true);
    }

    @Async
    public void sendActivationEmail(User user) {
        log.debug("Sending activation email to '{}'", user.getUsername());
        sendEmailFromTemplate(user,  user.getUsername(), "activationEmail", "email.activation.title");
    }

    @Async
    public void sendCreationEmail(User user) {
        log.debug("Sending creation email to '{}'", user.getUsername());
        sendEmailFromTemplate(user,  user.getUsername(), "creationEmail", "email.creation.title");
    }

    @Async
    public void sendCreationEmailAdmin(User user) {
        String adminEmail = user.getCustomer().getEmail();
        log.debug("Sending creation notification email to admin at '{}'", adminEmail);
        sendEmailFromTemplate(user,  user.getUsername(), "creationEmailAdmin", "email.creation.title");
    }

    @Async
    public void sendPasswordResetMail(User user) {
        log.debug("Sending password reset email to '{}'", user.getUsername());
        sendEmailFromTemplate(user, user.getUsername(), "passwordResetEmail", "email.reset.title");
    }

  @Async
  public void sendPasswordResetSuccessfulMail(User user) {
    log.debug("Sending password reset successful email to '{}'", user.getUsername());
    sendEmailFromTemplate(user, user.getUsername(), "passwordResetSuccessfulEmail", "email.reset.success.title");
  }
}
