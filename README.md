# Nexus E-commerce Assistant

Nexus is an intelligent e-commerce assistant built with **Spring Boot 3.4** and **Java 21**.

## Features
- **AI Integration**: Powered by Google GenAI (Gemini) via Spring AI.
- **Data Store**: Redis Stack used for Vector Store and Caching.
- **API**: RESTful endpoints for e-commerce operations.

## Prerequisites
- Java 21
- Maven
- Docker (for Redis Stack)

## Getting Started

### 1. Start Infrastructure
To start Redis Stack (Mapped to ports 6380 and 8002 to avoid conflicts):
```bash
docker-compose up -d
```

### 2. Build Project
```bash
mvn clean install
```

### 3. Run Application
```bash
mvn spring-boot:run
```

## Configuration
Configure API keys in `src/main/resources/application.properties`.
