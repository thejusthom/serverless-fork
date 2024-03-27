package com.csye6225.serverless.service;

import com.google.cloud.functions.CloudEventsFunction;
import com.google.events.cloud.pubsub.v1.Message;
import com.google.events.cloud.pubsub.v1.MessagePublishedData;
import com.google.gson.Gson;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.cloudevents.CloudEvent;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Objects;

@Service
public class EmailVerificationService implements CloudEventsFunction {

    private static final String DB_URL = System.getenv("DB_URL");
    private static final String DB_USERNAME = System.getenv("DB_USERNAME");
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");

    Logger logger = LoggerFactory.getLogger(EmailVerificationService.class);
    public void sendEmailVerification() {
        String verifyBaseUrl = "http://localhost:8080/v1/user/verify?";
        String decodedMessage = "tjthejus@gmail.com:0f09aa06-8293-4162-910e-bae04445645a";
        String[] parts = decodedMessage.split(":", 2);
        String username = parts[0];
        String token = parts[1];
        String verificationLink = verifyBaseUrl + "username=" + username + "&token=" + token;
        HttpResponse<JsonNode> request = Unirest.post("https://api.mailgun.net/v3/" + "thejusthomson.me" + "/messages")
                .basicAuth("api", "8a485137a99060e4b44205a16de392aa-f68a26c9-e1226ae7")
                .queryString("from", "Verification <noreply@thejusthomson.me>")
                .queryString("to", "tjthejus@gmail.com")
                .queryString("subject", "Webapp - Verification Link")
                .queryString("html", buildHtmlContent("tjthejus@gmail.com",verificationLink))
                .asJson();
        System.out.println((request.getBody()));
        insertMailSentTimeToDB(username);
    }

    @Override
    public void accept(CloudEvent cloudEvent) {
        String cloudEventData = new String(Objects.requireNonNull(cloudEvent.getData()).toBytes());
        Gson gson = new Gson();
        String verifyBaseUrl = "http://thejusthomson.me:8080/v1/user/verify?";
        MessagePublishedData data = gson.fromJson(cloudEventData, MessagePublishedData.class);
        Message message = data.getMessage();
        String encodedMessage = message.getData();
        String decodedMessage = new String(Base64.getDecoder().decode(encodedMessage));
        String[] parts = decodedMessage.split(":", 2);
        String username = parts[0];
        String token = parts[1];
        String verificationLink = verifyBaseUrl + "username=" + username + "&token=" + token;
        Unirest.post("https://api.mailgun.net/v3/" + "thejusthomson.me" + "/messages")
                .basicAuth("api", "8a485137a99060e4b44205a16de392aa-f68a26c9-e1226ae7")
                .queryString("from", "Verification <noreply@thejusthomson.me>")
                .queryString("to", decodedMessage)
                .queryString("subject", "Webapp - Verification Link")
                .queryString("html", buildHtmlContent(username,verificationLink))
                .asJson();
        insertMailSentTimeToDB(username);
    }

    private String buildHtmlContent(String username, String verificationLink) {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>Email Verification</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <p>Hello "+username+",</p>\n" +
                "    <p>Thank you for signing up! Please click the link below to verify your email address:</p>\n" +
                "    <p><a href=\"" + verificationLink + "\">Verify</a></p>\n" +
                "    <p>If you did not sign up for this account, please ignore this email.</p>\n" +
                "    <p>Best regards,</p>\n" +
                "    <p>Webapp</p>\n" +
                "</body>\n" +
                "</html>";
    }

    private void insertMailSentTimeToDB(String username) {
        try (Connection connection = createConnectionPool().getConnection()) {

            logger.info("Inside insertMailSentTimeToDB");

            String sql = "UPDATE webapp.user set verification_mail_sent_time = NOW() where username=?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1,username);
            statement.execute();
            int rowsUpdated = statement.executeUpdate();
            if (rowsUpdated > 0) {
                logger.info("Record updated successfully for: "+username);
            } else {
                logger.info("No records were updated.");
            }
        } catch (SQLException e) {
            logger.error("SQL Exception: " + e.getMessage());
        }
    }
    public DataSource createConnectionPool() {
        HikariConfig config = new HikariConfig();
        logger.info(DB_URL+" : "+DB_USERNAME+" : "+DB_PASSWORD);
        config.setJdbcUrl(String.format(DB_URL));
        config.setUsername(DB_USERNAME);
        config.setPassword(DB_PASSWORD);
        config.addDataSourceProperty("ipTypes", "PUBLIC,PRIVATE");
        return new HikariDataSource(config);
    }
}
