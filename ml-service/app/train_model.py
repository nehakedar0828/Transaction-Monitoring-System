"""
=============================================================================
EXPERIMENTAL EXPLORATION SCRIPT — NOT USED IN PRODUCTION RUNTIME
=============================================================================
This script explores an unsupervised IsolationForest baseline on 29 raw features.
It does NOT export any model artifacts and is NOT invoked by the production service.

Production Runtime Architecture:
- Inference Service: app/predict.py
- Production Estimator: LogisticRegression (class_weight='balanced', max_iter=1000)
- Production Artifacts: model/fraud_model.pkl, model/scaler.pkl, model/threshold.pkl
- Production Features: 31 engineered features (V1..V28, Amount_Log, time_sin, time_cos)
- Production Threshold: 0.95
- Training Source: notebooks/preprocessing.ipynb

Retained strictly for experimental record and baseline comparison.
=============================================================================
"""

import pandas as pd

from sklearn.preprocessing import StandardScaler
from sklearn.ensemble import IsolationForest

from sklearn.metrics import (
    confusion_matrix,
    classification_report,
    precision_score,
    recall_score,
    f1_score
)


# Path to the existing credit-card dataset.
DATASET_PATH = "../backend/src/main/resources/dataset/creditcard.csv"


# Load the dataset into a Pandas DataFrame.
df = pd.read_csv(DATASET_PATH)


# Display the shape of the dataset.
print("Dataset shape:", df.shape)


# Select the features that the model will use.
# Class is deliberately excluded because it is the actual fraud label.
features = [f"V{i}" for i in range(1, 29)] + ["Amount"]


# Create the feature matrix.
X = df[features]


# Create the actual fraud labels.
y = df["Class"]


# Display the selected features.
print("\nFeatures used by the model:")
print(features)


# Display the shape of the feature matrix.
print("\nFeature matrix shape:")
print(X.shape)


# Display the label shape.
print("\nLabel shape:")
print(y.shape)


# Create a scaler to standardize the feature values.
scaler = StandardScaler()


# Fit the scaler using the feature data and transform the features.
X_scaled = scaler.fit_transform(X)


# Display the resulting shape.
print("\nScaled feature matrix shape:")
print(X_scaled.shape)


# Calculate the actual fraud proportion in the dataset.
# This will be used as the initial contamination value.
fraud_ratio = y.mean()

# Display the fraud ratio.
print("\nFraud ratio:")
print(f"{fraud_ratio:.6f}")


# Create the Isolation Forest anomaly-detection model.
model = IsolationForest(
    n_estimators=200,       # Build 200 decision trees.
    contamination=fraud_ratio,  # Expected proportion of anomalies.
    random_state=42,        # Makes training reproducible.
    n_jobs=-1               # Use all available CPU cores.
)


# Train the model using only the transaction features.
model.fit(X_scaled)


# Generate anomaly predictions.
# Isolation Forest returns:
#  1  -> normal transaction
# -1  -> anomaly
predictions = model.predict(X_scaled)

# Convert Isolation Forest predictions into the same format
# as the actual Class labels.
# Isolation Forest: -1 = anomaly, 1 = normal.
# Our dataset:      1 = fraud,   0 = genuine.
y_pred = (predictions == -1).astype(int)


# Calculate the confusion matrix.
cm = confusion_matrix(y, y_pred)

# Display the confusion matrix.
print("\nConfusion Matrix:")
print(cm)


# Calculate precision.
precision = precision_score(y, y_pred)

# Calculate recall.
recall = recall_score(y, y_pred)

# Calculate F1-score.
f1 = f1_score(y, y_pred)


# Display the metrics.
print("\nPrecision:", precision)
print("Recall:", recall)
print("F1 Score:", f1)


# Display the complete classification report.
print("\nClassification Report:")
print(classification_report(y, y_pred))


# Display how many transactions were classified as anomalies.
print("\nModel prediction distribution:")
print(pd.Series(predictions).value_counts())