package com.iquenobot.sales.application;

import com.iquenobot.sales.domain.entity.Quote;
import com.iquenobot.sales.domain.entity.QuoteHistory;
import com.iquenobot.sales.domain.repository.QuoteHistoryRepository;
import com.iquenobot.shared.enums.QuoteHistoryAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuoteHistoryService {

    private final QuoteHistoryRepository quoteHistoryRepository;

    /**
     * Persists a history entry for the given quote. The actor is the current
     * user when provided; null means the BOT generated/executed the action.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Quote quote, QuoteHistoryAction action, UUID actorId, String actorName,
                       String channel, String channelMessageId, String details) {
        QuoteHistory entry = QuoteHistory.builder()
                .tenantId(quote.getTenantId())
                .quote(quote)
                .action(action)
                .performedBy(actorId)
                .actorName(actorName != null ? actorName : "Bot")
                .channel(channel)
                .channelMessageId(channelMessageId)
                .details(truncateDetails(details))
                .build();
        quoteHistoryRepository.save(entry);
        log.info("Quote history recorded: quote={} action={} actor={}", quote.getQuoteNumber(), action, entry.getActorName());
    }

    /**
     * El detalle puede contener mensajes de error muy largos; la columna es
     * varchar(1000), por lo que se recorta para no violar la restricción.
     */
    private String truncateDetails(String details) {
        if (details == null || details.length() <= 1000) {
            return details;
        }
        return details.substring(0, 997) + "...";
    }
}
