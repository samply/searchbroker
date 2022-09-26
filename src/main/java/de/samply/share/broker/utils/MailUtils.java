package de.samply.share.broker.utils;

import de.samply.common.mailing.EmailBuilder;
import de.samply.common.mailing.MailSender;
import de.samply.common.mailing.MailSending;
import de.samply.common.mailing.OutgoingEmail;
import de.samply.config.util.FileFinderUtil;
import de.samply.share.broker.thread.MailSenderThread;
import de.samply.share.common.utils.ProjectInfo;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * An utility class holding methods to send emails.
 * Templates stored in src/main/resources/mailTemplates.
 */
public class MailUtils {

  private static final Logger logger = LogManager.getLogger(MailUtils.class);

  private static final String MAIL_SUBJECT_REGISTRATION_DE =
      "Ihre Registrierung am Samply.Share Suchbroker";
  private static final String MAIL_SUBJECT_REGISTRATION_EN =
      "Your registration with Samply.Share.Broker";


  /**
   * Send activation mail. Mail content can be changed in the soy templates.
   *
   * @param receiverAddress the email address of the receiver
   * @param token           the token used to verify the mail address
   */
  public static void sendActivationmail(String receiverAddress, String token) {
    OutgoingEmail email = new OutgoingEmail();
    String projectName = ProjectInfo.INSTANCE.getProjectName();
    email.addAddressee(receiverAddress);
    if (projectName.equalsIgnoreCase("dktk")) {
      email.setLocale("de");
      email.setSubject(MAIL_SUBJECT_REGISTRATION_DE);
    } else {
      email.setLocale("en");
      email.setSubject(MAIL_SUBJECT_REGISTRATION_EN);
    }
    email.putParameter("token", token);
    MailSending mailSending = MailSender.loadMailSendingConfig(projectName,
        System.getProperty("catalina.base") + File.separator + "conf",
        ProjectInfo.INSTANCE.getServletContext().getRealPath("/WEB-INF"));

    EmailBuilder builder = initializeBuilder(mailSending);
    builder.addTemplateFile("NewRegistrationContent.soy",
        "NewRegistrationContent");
    email.setBuilder(builder);

    Thread mailSenderThread = new Thread(new MailSenderThread(mailSending, email));
    mailSenderThread.start();
  }

  /**
   * Initializes an EmailBuilder with the two default Soy-Templates: main.soy and footer.soy.
   *
   * @param mailSending mail sending configuration
   * @return an email builder instance
   */
  private static EmailBuilder initializeBuilder(MailSending mailSending) {
    String templateFolder = Utils.getRealPath(mailSending.getTemplateFolder());

    EmailBuilder builder = new EmailBuilder(templateFolder, false);
    builder.addTemplateFile("MainMailTemplate.soy", null);
    builder.addTemplateFile("Footer.soy", "Footer");
    return builder;
  }

  /**
   * Get the email of the statistic recipients and send the statistics.
   * @throws IOException IOException
   */
  public static void sendStatistics() throws IOException {
    OutgoingEmail email = new OutgoingEmail();
    String projectName = ProjectInfo.INSTANCE.getProjectName();
    email.setSubject("Sample Locator Statistic");
    File addressFile = FileFinderUtil.findFile("statistic_notification.txt", projectName,
        System.getProperty("catalina.base") + File.separator + "conf",
        ProjectInfo.INSTANCE.getServletContext().getRealPath("/WEB-INF"));
    Stream<String> streams = Files.lines(addressFile.toPath(), StandardCharsets.UTF_8);
    streams.forEach(email::addAddressee);
    DateFormat parseFormat = new SimpleDateFormat("yyyy-MM-dd");
    String statistics =
        System.getProperty("catalina.base") + File.separator + "logs" + File.separator
            + "statistics" + File.separator + "statistic_" + parseFormat
            .format(new Date(System.currentTimeMillis() - 1000 * 60 * 60 * 24)) + ".xlsx";
    email.getAttachmentPaths().add(statistics);
    MailSending mailSending = MailSender.loadMailSendingConfig(projectName,
        System.getProperty("catalina.base") + File.separator + "conf",
        ProjectInfo.INSTANCE.getServletContext().getRealPath("/WEB-INF"));
    Thread mailSenderThread = new Thread(new MailSenderThread(mailSending, email));
    mailSenderThread.start();
  }
}
