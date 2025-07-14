package com.trade.tradestore.kafka;

import com.trade.tradestore.dto.TradeRequest;
import com.trade.tradestore.service.TradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TradeListener {

    private final TradeService tradeService;

    @KafkaListener(topics = "trade-topic", groupId = "trade-group", containerFactory = "kafkaListenerContainerFactory")
    public void consume(TradeRequest tradeRequest) {
        log.info("Received trade from Kafka: {}", tradeRequest);
        try {
            tradeService.saveTrade(tradeRequest);
        } catch (Exception e) {
            log.error("Failed to save trade: {}", tradeRequest.getTradeId(), e);
        }
    }
}

