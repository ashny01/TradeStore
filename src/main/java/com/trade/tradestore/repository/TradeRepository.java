package com.trade.tradestore.repository;

import com.trade.tradestore.model.Trade;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TradeRepository extends MongoRepository<Trade, String> {
    List<Trade> findByTradeId(String tradeId);
    Optional<Trade> findTopByTradeIdOrderByVersionDesc(String tradeId);
    List<Trade> findByMaturityDateBeforeAndExpired(LocalDate date, String expired);
}
