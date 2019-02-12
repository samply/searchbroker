package de.samply.share.broker.thread;

import de.samply.common.mailing.MailSender;
import de.samply.common.mailing.MailSending;
import de.samply.common.mailing.OutgoingEmail;

/**
 * A Thread that sends a given email via a given mail server
 */
public class MailSenderThread implements Runnable {
    
    private MailSending mailSending;
    private OutgoingEmail email;
    
    public MailSenderThread(MailSending mailSending, OutgoingEmail email) {
        this.mailSending = mailSending;
        this.email = email;
    }

    @Override
    public void run() {
        MailSender mailSender = new MailSender(mailSending);
        mailSender.send(email);
    }

}
