# Transaction Monitoring System

A real-time transaction monitoring platform that simulates live financial transactions, analyzes them using a Machine Learning microservice, and visualizes risk through an interactive dashboard.

> 🚧 This project is currently under development.

---

## Project Goal

The objective of this project is to build an end-to-end transaction monitoring system that combines backend engineering with applied machine learning.

The system will:

- Simulate a live stream of financial transactions
- Detect anomalous transactions using a Machine Learning model
- Generate risk scores and explanations for each transaction
- Store transaction and prediction data in PostgreSQL
- Provide a real-time dashboard for monitoring and analysis

---

## Tech Stack

### Backend
- Java 21
- Spring Boot
- Spring Data JPA
- PostgreSQL
- Maven

### Machine Learning
- Python
- FastAPI
- Scikit-learn

### Frontend
- HTML
- CSS
- JavaScript
- Chart.js

### DevOps
- Docker
- Docker Compose

---

## Planned Architecture

```text
                Transaction Dataset
                        │
                        ▼
              Transaction Replay Engine
                        │
                        ▼
                 Spring Boot Backend
                        │
        ┌───────────────┴───────────────┐
        ▼                               ▼
 PostgreSQL Database          ML Microservice (FastAPI)
                                        │
                                        ▼
                         Risk Score + Prediction
                                        │
                                        ▼
                          Monitoring Dashboard
```

---

## Repository Structure

```
transaction-monitoring-system/
│
├── backend/
├── ml-service/
├── frontend/
├── docs/
├── dataset/
├── docker-compose.yml
├── README.md
└── .gitignore
```

---

## Development Roadmap

- [x] Day 1 – Repository setup and project structure
- [ ] Backend foundation
- [ ] PostgreSQL integration
- [ ] Transaction replay engine
- [ ] Machine Learning service
- [ ] Backend ↔ ML integration
- [ ] Dashboard
- [ ] Dockerization
- [ ] Documentation and testing

---

## Current Status

**Phase:** Project Initialization

Day 1 focuses on setting up the repository, defining the architecture, and preparing the development environment.

Business logic will be implemented in the following phases.

---

## License

This project is licensed under the MIT License.