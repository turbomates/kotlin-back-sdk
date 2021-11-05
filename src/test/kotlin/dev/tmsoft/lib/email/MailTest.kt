package dev.tmsoft.lib.email

import com.icegreen.greenmail.util.GreenMail
import com.icegreen.greenmail.util.GreenMailUtil
import com.icegreen.greenmail.util.ServerSetup
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMultipart
import kotlin.test.Test
import kotlin.test.assertEquals


class MailTest {
    @Test
    fun `send mail test`() {
        val greenMail = GreenMail(ServerSetup(3025, "localhost", ServerSetup.PROTOCOL_SMTP))
        greenMail.start()
        val subject = "Test Subject"
        val body = "<p>Hi!</p>"
        val mail = JavaxMail(SMTPConfig("localhost", 3025, SMTPSecure.TLS))

        val message =
            Message(
                Address("test@gmail.com", "test user"),
                listOf(Address("test-next@gmail.com", "test next")),
                subject
            ) {
                replyTo = listOf(Address("test-reply@gmail.com", "test reply"))
                html = body
            }
        mail.send(message)
        val messages = greenMail.receivedMessages
        val mp: MimeMultipart = messages[0].content as MimeMultipart
        assertEquals(subject, messages[0].subject);
        assertEquals(body, GreenMailUtil.getBody(mp.getBodyPart(0)).trim())
        assertEquals(
            listOf(InternetAddress("test-reply@gmail.com", "test reply").toString()),
            messages[0].replyTo.map { it.toString() })
        greenMail.stop()
    }
}
