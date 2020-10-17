package com.r307.arbitrader.service;

import com.r307.arbitrader.config.NotificationConfiguration;
import com.r307.arbitrader.service.model.Spread;
import org.knowm.xchange.currency.Currency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.math.BigDecimal;

@Service
@Async
public class NotificationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationService.class);
    public static final String EMAIL_SUBJECT_NEW_ENTRY = "Arbitrader - New Entry Trade";
    public static final String EMAIL_SUBJECT_NEW_EXIT = "Arbitrader - New Exit Trade";

    private final JavaMailSender javaMailSender;
    private final NotificationConfiguration notificationConfiguration;

    @Inject
    public NotificationService(JavaMailSender javaMailSender, NotificationConfiguration notificationConfiguration) {
        this.javaMailSender = javaMailSender;
        this.notificationConfiguration = notificationConfiguration;
    }


    public void sendEmailNotification(String subject, String body) {
        if (!notificationConfiguration.getEmail().getActive()) {
            LOGGER.info("Email notification is disabled");
            return;
        }

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setTo(notificationConfiguration.getEmail().getTo());
        mail.setFrom(notificationConfiguration.getEmail().getFrom());
        mail.setSubject(subject);
        mail.setText(body);

        try {
            javaMailSender.send(mail);
        }
        catch (Exception e) {
            LOGGER.error("Could not send email notification to {}. Reason: {}", notificationConfiguration.getEmail().getTo(), e.getMessage());
        }
    }

    public void sendEmailNotificationBodyForEntryTrade(Spread spread, BigDecimal exitTarget, BigDecimal longVolume,
                                                           BigDecimal longLimitPrice, BigDecimal shortVolume,
                                                           BigDecimal shortLimitPrice) {

        final String longEntryString = String.format("Long entry: %s %s %s @ %s (%s slip) = %s%s\n",
            spread.getLongExchange().getExchangeSpecification().getExchangeName(),
            spread.getCurrencyPair(),
            longVolume.toPlainString(),
            longLimitPrice.toPlainString(),
            longLimitPrice.subtract(spread.getLongTicker().getAsk()).toPlainString(),
            Currency.USD.getSymbol(),
            longVolume.multiply(longLimitPrice).toPlainString());

        final String shortEntryString = String.format("Short entry: %s %s %s @ %s (%s slip) = %s%s\n",
            spread.getShortExchange().getExchangeSpecification().getExchangeName(),
            spread.getCurrencyPair(),
            shortVolume.toPlainString(),
            shortLimitPrice.toPlainString(),
            spread.getShortTicker().getBid().subtract(shortLimitPrice).toPlainString(),
            Currency.USD.getSymbol(),
            shortVolume.multiply(shortLimitPrice).toPlainString());

        final String emailBody = new StringBuilder("***** ENTRY *****\n")
            .append(String.format("Entry spread: %s\n", spread.getIn().toPlainString()))
            .append(String.format("Exit spread target: %s\n", exitTarget.toPlainString()))
            .append(longEntryString)
            .append(shortEntryString)
            .toString();

        sendEmailNotification(EMAIL_SUBJECT_NEW_ENTRY, emailBody);
    }

    public void sendEmailNotificationBodyForExitTrade(Spread spread, BigDecimal longVolume, BigDecimal longLimitPrice,
                                                          BigDecimal shortVolume, BigDecimal shortLimitPrice,
                                                          BigDecimal entryBalance, BigDecimal updatedBalance) {

        final String longCloseString = String.format("Long close: %s %s %s @ %s (%s slip) = %s%s \n",
            spread.getLongExchange().getExchangeSpecification().getExchangeName(),
            spread.getCurrencyPair(),
            longVolume.toPlainString(),
            longLimitPrice.toPlainString(),
            longLimitPrice.subtract(spread.getLongTicker().getBid()).toPlainString(),
            Currency.USD.getSymbol(),
            longVolume.multiply(spread.getLongTicker().getBid()).toPlainString());

        final String shortCloseString = String.format("Short close: %s %s %s @ %s (%s slip) = %s%s \n",
            spread.getShortExchange().getExchangeSpecification().getExchangeName(),
            spread.getCurrencyPair(),
            shortVolume.toPlainString(),
            shortLimitPrice.toPlainString(),
            spread.getShortTicker().getAsk().subtract(shortLimitPrice).toPlainString(),
            Currency.USD.getSymbol(),
            shortVolume.multiply(spread.getShortTicker().getAsk()).toPlainString());

        final BigDecimal profit = updatedBalance.subtract(entryBalance);

        final String emailBody = new StringBuilder("***** EXIT *****\n")
            .append(longCloseString)
            .append(shortCloseString)
            .append(String.format("Combined account balances on entry: $%s\n", entryBalance.toPlainString()))
            .append(String.format("Profit calculation: $%s - $%s = $%s\n", updatedBalance.toPlainString(), entryBalance.toPlainString(), profit.toPlainString()))
            .toString();

        sendEmailNotification(EMAIL_SUBJECT_NEW_EXIT, emailBody);
    }
}
