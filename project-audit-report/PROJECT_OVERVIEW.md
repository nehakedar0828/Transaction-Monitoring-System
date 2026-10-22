# PROJECT OVERVIEW

## 1. Executive Summary

The **Transaction Monitoring System** is an end-to-end distributed system designed to detect fraudulent and anomalous credit card transactions. It combines a robust enterprise backend built with **Java 21** and **Spring Boot 4.1.0 (SNAPSHOT)**, an analytical machine learning microservice built with **Python 3.13** and **FastAPI**, and persistence managed via **PostgreSQL** and **Spring Data JPA**.

The system enables financial institutions or risk analysts to:
1. Ingest real-world transaction data (the Kaggle European Cardholders Credit Card Fraud Detection benchmark).
2. Store transaction records alongside anonymized PCA feature vectors (`V1` to `V28`).
3. Query and filter transactions via REST APIs with dynamic multi-attribute specifications, pagination, and sorting.
4. Integrate with an ML inference microservice that computes fraud probabilities using feature-engineered cyclical time transformations and an optimized Logistic Regression classifier.
5. Provide aggregate portfolio risk metrics, fraud volumes, monetary impact distributions, and processing status summaries for compliance dashboards.

---

## 2. Business Problem & Intended Users

### The Problem
Financial fraud causes tens of billions of dollars in losses annually. Credit card transactions occur at massive volumes with severe class imbalance (typically less than 0.2% fraud). Traditional rule-based engines suffer from high false-negative rates and rigid thresholds. This system implements an intelligent detection architecture that combines machine learning probability scoring with scalable backend persistence and analytical reporting.

### Intended Users
1. **Fraud & Risk Analysts:** Review suspicious transactions, inspect anomalous feature patterns, and monitor ongoing fraud rates across the portfolio.
2. **Compliance & Operations Officers:** Track transaction processing states (`PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`) and generate financial loss summaries.
3. **Software & ML Engineers:** Continuously benchmark models, train supervised and unsupervised detectors, and tune operational probability thresholds to minimize false alerts.

---

## 3. Technology Stack & Component Responsibilities

| Tier / Category | Technology | Version / Spec | Role & Responsibility |
|---|---|---|---|
| **Language (Backend)** | Java | 21 (LTS) | Core backend programming language providing strong typing and modern concurrency. |
| **Framework (Backend)** | Spring Boot | 4.1.0-SNAPSHOT | Microservice framework providing dependency injection, REST controllers, and auto-configuration. |
| **Data Access / ORM** | Spring Data JPA / Hibernate | Integrated | Entity mapping, transaction boundaries, JPA Criteria API specifications, and derived queries. |
| **Database** | PostgreSQL | 15+ (Local: port 5432) | Relational database holding transactions, feature vectors, and prediction results. |
| **HTTP Client** | Spring Framework `RestClient` | Integrated | Modern synchronous HTTP client configured in [RestClientConfig.java](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/backend/src/main/java/com/nehakedar/backend/config/RestClientConfig.java) to invoke the FastAPI ML service. |
| **CSV Parsing** | OpenCSV | 5.10 | Streaming reader used in [DatasetImportService.java](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/backend/src/main/java/com/nehakedar/backend/service/DatasetImportService.java) to parse dataset records. |
| **Boilerplate Reduction** | Project Lombok | Integrated | Generates getters, setters, builders, and constructors via annotation processing. |
| **Validation** | Jakarta Bean Validation | Hibernate Validator | Validates query parameters (`@Min`, `@PositiveOrZero`). |
| **Language (ML)** | Python | 3.13 | Scripting, data exploration, feature engineering, and model training. |
| **Web Framework (ML)** | FastAPI | 0.115+ | High-performance ASGI microservice serving `/predict` and `/` healthcheck endpoints. |
| **Data Science / ML** | Scikit-learn, Pandas, NumPy | 1.6+, 2.2+, 2.2+ | Feature engineering (log transform, cyclical time encoding), scaling (`StandardScaler`), model training (`LogisticRegression`, `IsolationForest`). |
| **Model Serialization** | Joblib | 1.4+ | Serializes and deserializes model binaries (`fraud_model.pkl`, `scaler.pkl`, `threshold.pkl`). |
| **Containerization** | Docker Compose | Specification | Root [docker-compose.yml](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/docker-compose.yml) (currently unconfigured 0-byte file). |
| **Frontend** | N/A | None | Planned HTML/CSS/Chart.js dashboard (directory exists but no source files committed). |

---

## 4. Overall Architecture Overview

The system is organized into a decoupled, microservice-based architecture:

```
                      ┌────────────────────────────────┐
                      │   Client / Frontend Consumer   │
                      └──────────────┬─────────────────┘
                                     │ HTTP REST
                                     ▼
                      ┌────────────────────────────────┐
                      │      Spring Boot Backend       │
                      │          (Port 8080)           │
                      │                                │
                      │  • TransactionController       │
                      │  • DashboardController         │
                      │  • FraudDetectionController    │
                      │  • DatasetImportController     │
                      └───────┬────────────────┬───────┘
          JDBC / Hibernate    │                │  RestClient (HTTP POST /predict)
                              ▼                ▼
     ┌──────────────────────────────┐    ┌──────────────────────────────┐
     │     PostgreSQL Database      │    │     FastAPI ML Service       │
     │         (Port 5432)          │    │         (Port 8000)          │
     │                              │    │                              │
     │  • transactions              │    │  • StandardScaler            │
     │  • transaction_features      │    │  • Cyclical Time Encoding    │
     │  • predictions               │    │  • LogisticRegression Model  │
     └──────────────────────────────┘    └──────────────────────────────┘
```

1. **Ingestion & Storage:** Transaction data is loaded via `/api/import` from `creditcard.csv` into PostgreSQL normalized tables (`transactions`, `transaction_features`).
2. **Analysis & Filtering:** Clients fetch paginated records via `/api/transactions` and `/api/transactions/search` with dynamic criteria builders.
3. **Inference Bridge:** Backend routes transactions to FastAPI at `http://127.0.0.1:8000/predict` using Spring's `RestClient`.
4. **Dashboard Aggregations:** Summary statistics, fraud monetary values, status distributions, and recent fraud incidents are aggregated in SQL via [DashboardService.java](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/backend/src/main/java/com/nehakedar/backend/service/DashboardService.java).

---

## 5. Implementation Status Matrix

| Feature / Capability | Planned vs Implemented | File Reference | Verified Implementation Details |
|---|---|---|---|
| **Spring Boot Application Foundation** | **Fully Implemented** | [BackendApplication.java](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/backend/src/main/java/com/nehakedar/backend/BackendApplication.java) | Configured with Spring Web, Data JPA, Validation, Lombok, and PostgreSQL driver. |
| **Transaction CRUD & Pagination** | **Fully Implemented** | [TransactionController.java](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/backend/src/main/java/com/nehakedar/backend/controller/TransactionController.java) | `GET /api/transactions` supporting `page`, `size`, `sortBy`, `direction`, and `fraud` filters. |
| **Transaction ID Lookup** | **Fully Implemented** | [TransactionController.java](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/backend/src/main/java/com/nehakedar/backend/controller/TransactionController.java) | `GET /api/transactions/{id}` with 404 handler. |
| **Dynamic Specification Filtering** | **Fully Implemented** | [TransactionSpecification.java](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/backend/src/main/java/com/nehakedar/backend/specifications/TransactionSpecification.java) | `GET /api/transactions/search` with `minAmount`, `maxAmount`, and `fraud` boolean predicates. |
| **Dashboard Summary Aggregations** | **Fully Implemented** | [DashboardController.java](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/backend/src/main/java/com/nehakedar/backend/controller/DashboardController.java) | Computes total transactions, fraud count, fraud percentage, total amounts, fraud amounts, status groups, and top 10 recent frauds. |
| **Dataset Batch CSV Ingestion** | **Partially Implemented** | [DatasetImportService.java](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/backend/src/main/java/com/nehakedar/backend/service/DatasetImportService.java) | Row-by-row OpenCSV import from classpath resource; missing `@Transactional`, batch flushing, and duplicate handling. |
| **FastAPI ML Service** | **Fully Implemented** | [main.py](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/ml-service/app/main.py) | Endpoints `GET /` and `POST /predict` taking 30 input features and returning predictions. |
| **ML Feature Preprocessing & Scaling** | **Fully Implemented** | [predict.py](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/ml-service/app/predict.py) | Computes `Amount_Log = log1p(Amount)`, cyclical time `sin`/`cos`, loads `scaler.pkl`, and standardizes 31 features. |
| **ML Inference Pipeline** | **Fully Implemented** | [predict.py](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/ml-service/app/predict.py) | Loads trained `fraud_model.pkl` and threshold `0.95`, outputting probability and binary decision. |
| **Backend ↔ ML Integration** | **Partially Implemented** | [FraudDetectionService.java](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/backend/src/main/java/com/nehakedar/backend/service/FraudDetectionService.java) | REST proxy `POST /api/fraud/predict` works; `predictTransaction(Transaction)` method exists but is not wired into any ingestion or lifecycle workflow. |
| **Prediction Persistence** | **Partially Implemented** | [Prediction.java](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/backend/src/main/java/com/nehakedar/backend/entity/Prediction.java) | `Prediction` entity and `PredictionRepository` exist; however, predictions generated via API or ML are **never saved** to the database in active code. |
| **Transaction Processing Engine / Replay** | **Missing / Planned** | [README.md](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/README.md) | Described in roadmap as "Transaction Replay Engine"; no scheduler, Kafka consumer, or streaming runner currently exists. |
| **Frontend Dashboard UI** | **Missing / Planned** | `frontend/` directory | Directory exists in git repository but is empty. No HTML, CSS, JavaScript, or Chart.js code is committed. |
| **Docker & Container Deployment** | **Missing / Planned** | [docker-compose.yml](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/docker-compose.yml) | File is empty (0 bytes). No Dockerfiles exist in `backend/` or `ml-service/`. |
| **Automated Test Suite** | **Minimal / Missing** | [BackendApplicationTests.java](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/backend/src/test/java/com/nehakedar/backend/BackendApplicationTests.java) | Only default `contextLoads()` test exists. No unit tests, mock tests, or integration tests implemented. |
