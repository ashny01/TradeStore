package com.trade.tradestore.kafka;

import com.trade.tradestore.dto.TradeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TradeProducer {

    private final KafkaTemplate<String, TradeRequest> kafkaTemplate;

    public void sendTrade(TradeRequest tradeRequest) {
        kafkaTemplate.send("trade-topic", tradeRequest);
    }
}

