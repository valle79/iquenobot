package com.iquenobot.sales.infrastructure.event;

import com.iquenobot.sales.interfaces.event.QuoteEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class QuoteEventPublisher {

    private final ApplicationEventPublisher springPublisher;

    public void publish(QuoteEvent event) {
        log.debug("Publishing quote event: type={} quote={} tenantId={}",
                event.getType(), event.getQuoteNumber(), event.getTenantId());
        springPublisher.publishEvent(event);
    }
}
