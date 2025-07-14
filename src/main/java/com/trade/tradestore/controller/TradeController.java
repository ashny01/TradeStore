package com.trade.tradestore.controller;


import com.trade.tradestore.dto.TradeRequest;
import com.trade.tradestore.dto.TradeResponse;
import com.trade.tradestore.kafka.TradeProducer;
import com.trade.tradestore.service.TradeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
public class TradeController {

    private final TradeService tradeService;
    private final TradeProducer tradeProducer;

    // Existing: Direct save to DB
    @PostMapping
    public ResponseEntity<TradeResponse> saveTrade(@Valid @RequestBody TradeRequest request) {
        TradeResponse response = tradeService.saveTrade(request);
        return ResponseEntity.ok(response);
    }

    // New: Send to Kafka
    @PostMapping("/publish")
    public ResponseEntity<String> publishTrade(@Valid @RequestBody TradeRequest request) {
        tradeProducer.sendTrade(request);
        return ResponseEntity.ok("Trade sent to Kafka successfully.");
    }
}
