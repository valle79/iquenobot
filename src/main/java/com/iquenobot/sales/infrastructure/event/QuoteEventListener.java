package com.iquenobot.sales.infrastructure.event;

import com.iquenobot.sales.interfaces.event.QuoteEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Async listener for quote domain events. Used for audit/logging purposes;
 * the persisted audit trail (quote_history) is written synchronously by the
 * services to guarantee consistency with the transaction.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QuoteEventListener {

    @Async
    @EventListener
    public void handleQuoteEvent(QuoteEvent event) {
        log.info("Quote audit event: type={} quote={} actor={} channel={} messageId={} details={}",
                event.getType(), event.getQuoteNumber(),
                event.getActorName() != null ? event.getActorName() : "BOT",
                event.getChannel(), event.getChannelMessageId(), event.getDetails());
    }
}
