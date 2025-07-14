package com.trade.tradestore.service;

import com.trade.tradestore.dto.TradeRequest;
import com.trade.tradestore.dto.TradeResponse;
import com.trade.tradestore.model.Trade;
import com.trade.tradestore.repository.TradeRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TradeService {
    private final TradeRepository tradeRepository;

    public TradeResponse saveTrade(TradeRequest request) {
        // Maturity date validation
        if (request.getMaturityDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Trade rejected: Maturity date is in the past.");
        }

        // Check latest version of existing trade with same tradeId
        Optional<Trade> latestTradeOpt = tradeRepository.findTopByTradeIdOrderByVersionDesc(request.getTradeId());

        if (latestTradeOpt.isPresent()) {
            Trade existing = latestTradeOpt.get();
            if (request.getVersion() < existing.getVersion()) {
                throw new IllegalArgumentException("Trade rejected: Lower version received.");
            }
        }

        // Save or override trade
        Trade trade = Trade.builder()
                .tradeId(request.getTradeId())
                .version(request.getVersion())
                .counterPartyId(request.getCounterPartyId())
                .bookId(request.getBookId())
                .maturityDate(request.getMaturityDate())
                .createdDate(LocalDate.now())
                .expired("N")
                .build();

        tradeRepository.save(trade);

        return TradeResponse.builder()
                .tradeId(trade.getTradeId())
                .version(trade.getVersion())
                .counterPartyId(trade.getCounterPartyId())
                .bookId(trade.getBookId())
                .maturityDate(trade.getMaturityDate())
                .createdDate(trade.getCreatedDate())
                .expired(trade.getExpired())
                .build();
    }

    @Scheduled(cron = "0 0 * * * *") // runs every hour
    public void expireMaturedTrades() {
        LocalDate today = LocalDate.now();
        List<Trade> expiredTrades = tradeRepository.findByMaturityDateBeforeAndExpired(today, "N");
        for (Trade trade : expiredTrades) {
            trade.setExpired("Y");
            tradeRepository.save(trade);
            System.out.println("Trade marked expired: "+ trade.getTradeId());
        }
    }
}
