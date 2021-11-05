package dev.tmsoft.lib.email

import java.util.Properties
import javax.mail.Authenticator
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart


class JavaxMail(val config: SMTPConfig) : Mail {
    private val session: Session
    private val transport: Transport

    init {
        val props = Properties()
        props["mail.smtp.host"] = config.host
        props["mail.smtp.port"] = config.port
        if (config.smtpSecure == SMTPSecure.SSL) {
            props["mail.smtp.socketFactory.port"] = config.port
            props["mail.smtp.ssl.enable"] = "true"
            props["mail.smtp.socketFactory.class"] = "javax.net.ssl.SSLSocketFactory"
        }
        var authenticator: Authenticator? = null
        if (config.username != null || config.password != null) {
            props["mail.smtp.auth"] = "true"
            authenticator = object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(config.username, config.password);
                }

            }
        }
        session = Session.getDefaultInstance(props, authenticator)
        transport = session.transport
        transport.connect()
    }

    override fun send(message: Message): Boolean {
        transport.sendMessage(
            convertMessage(message),
            message.to.map { it.convert() }.toTypedArray()
        )
        return true
    }

    private fun convertMessage(message: Message): MimeMessage {
        return MimeMessage(session).apply {
            subject = message.subject
            replyTo = message.replyTo.map { it.convert() }.toTypedArray()
            setFrom(message.from.convert())
            addRecipients(javax.mail.Message.RecipientType.TO, message.to.map { it.convert() }.toTypedArray())
            val content = MimeMultipart()
            message.html?.let {
                val mimeBodyPart = MimeBodyPart()
                mimeBodyPart.setContent(it, "text/html")
                content.addBodyPart(mimeBodyPart)
            }
            message.plain?.let {
                val mimeBodyPart = MimeBodyPart()
                mimeBodyPart.setText(it)
                content.addBodyPart(mimeBodyPart)
            }
            message.attaches.forEach {
                val attachmentBodyPart = MimeBodyPart()
                attachmentBodyPart.attachFile(it)
                content.addBodyPart(attachmentBodyPart)
            }
            setContent(content)
        }
    }

    private fun Address.convert(): InternetAddress {
        return InternetAddress(email, name)
    }
}
