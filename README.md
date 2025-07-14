# 🏦 Trade Store — Real-time Streaming with Spring Boot + Kafka + MongoDB

This project implements a high-throughput trade ingestion system using:

- **Spring Boot** (Java 17)
- **Apache Kafka** (streaming)
- **MongoDB** (NoSQL database)
- **JUnit** + **Embedded Kafka** for TDD
- **GitHub Actions** for CI with OSS scan

---

## 🚀 Features

- Trade ingestion via Kafka (`trade-topic`)
- Real-time async storage into MongoDB
- REST APIs to publish trades and fetch data
- Scheduled job to mark expired trades
- Full unit test suite with `EmbeddedKafka`
- Secure CI/CD pipeline with OWASP vulnerability check

---

## 📦 Technologies

| Tool         | Purpose                         |
|--------------|----------------------------------|
| Spring Boot  | Backend microservice             |
| Apache Kafka | Streaming ingestion              |
| MongoDB      | NoSQL database for trade store   |
| JUnit        | Unit testing (TDD)               |
| GitHub Actions | CI/CD pipeline with OWASP scan |
| Docker       | Kafka + MongoDB runtime          |

---

## 🧪 API Endpoints

| Method | Endpoint              | Description                          |
|--------|------------------------|--------------------------------------|
| POST   | `/api/trades`          | Save trade directly to Mongo         |
| POST   | `/api/trades/publish`  | Publish trade to Kafka               |

Example Payload:

```json
{
  "tradeId": "T123",
  "version": 1,
  "counterPartyId": "CP-1",
  "bookId": "B1",
  "maturityDate": "2025-12-31"
}
