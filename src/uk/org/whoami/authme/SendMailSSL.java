package uk.org.whoami.authme;

import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.bukkit.Bukkit;

import uk.org.whoami.authme.cache.auth.PlayerAuth;
import uk.org.whoami.authme.settings.Settings;
 
public class SendMailSSL {
	
	public AuthMe instance;
	
	public SendMailSSL(AuthMe instance) {
		this.instance = instance;
	}
	public void main(final PlayerAuth auth, final String newPass) {
		
				Properties props = new Properties();
				props.put("mail.smtp.host", Settings.getmailSMTP);
				props.put("mail.smtp.socketFactory.port", String.valueOf(Settings.getMailPort));
				props.put("mail.smtp.socketFactory.class",
						"javax.net.ssl.SSLSocketFactory");
				props.put("mail.smtp.auth", "true");
				props.put("mail.smtp.port", String.valueOf(Settings.getMailPort));
		 
				Session session = Session.getInstance(props,
					new javax.mail.Authenticator() {
						protected PasswordAuthentication getPasswordAuthentication() {
							return new PasswordAuthentication(Settings.getmailAccount,Settings.getmailPassword);
						}
					});

				try {
		 
					final Message message = new MimeMessage(session);
					message.setFrom(new InternetAddress(Settings.getmailAccount));
					message.setRecipients(Message.RecipientType.TO,
							InternetAddress.parse(auth.getEmail()));
					message.setSubject("Your New AuthMe Password");
					message.setText("Dear " + auth.getNickname() + ", " + "\n\n This is your new AuthMe password for the server : \n\n" + instance.getServer().getServerName() + "\n\n" + newPass + "\n\n Do not forget to change password after login! \n /changepassword " + newPass +" <newPass>");
					
					Bukkit.getScheduler().runTaskAsynchronously(instance, new Runnable() {

						@Override
						public void run() {
							try {
								Transport.send(message);
							} catch (MessagingException e) {
								e.printStackTrace();
							}
							
						}
						
					});
					

					ConsoleLogger.info("Recovery Email sent to : " + auth.getNickname());
		 
				} catch (MessagingException e) {
					throw new RuntimeException(e);
				}
			}
}