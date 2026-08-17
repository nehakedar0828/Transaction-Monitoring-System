from pathlib import Path
import joblib
import numpy as np
import pandas as pd


# Get the directory containing predict.py.
BASE_DIR = Path(__file__).resolve().parent

# Go from app/ to ml-service/.
PROJECT_DIR = BASE_DIR.parent

# Define the model directory.
MODEL_DIR = PROJECT_DIR / "model"


# Load the trained Logistic Regression model.
model = joblib.load(MODEL_DIR / "fraud_model.pkl")

# Load the fitted StandardScaler.
scaler = joblib.load(MODEL_DIR / "scaler.pkl")

# Load the selected fraud probability threshold.
threshold = joblib.load(MODEL_DIR / "threshold.pkl")


# The exact feature order used during model training.
FEATURE_COLUMNS = [
    "V1", "V2", "V3", "V4", "V5", "V6", "V7",
    "V8", "V9", "V10", "V11", "V12", "V13",
    "V14", "V15", "V16", "V17", "V18", "V19",
    "V20", "V21", "V22", "V23", "V24", "V25",
    "V26", "V27", "V28",
    "Amount_Log",
    "time_sin",
    "time_cos"
]


def predict_transaction(transaction):
    """
    Predict whether a transaction is fraudulent.

    Expected raw transaction format:

        {
            "Time": ...,
            "V1": ...,
            ...
            "V28": ...,
            "Amount": ...
        }

    Returns:
        Dictionary containing fraud prediction,
        probability, and threshold.
    """

    # Extract the raw transaction time.
    time = transaction["Time"]

    # Extract the raw transaction amount.
    amount = transaction["Amount"]


    # ---------------------------------------------------------
    # Feature engineering
    # ---------------------------------------------------------

    # Transform Amount using the same log transformation
    # used during model training.
    amount_log = np.log1p(amount)


    # Number of seconds in one day.
    seconds_in_day = 24 * 60 * 60

    # Convert the relative timestamp into time within a day.
    time_of_day = time % seconds_in_day

    # Convert seconds into hours.
    hour = time_of_day / 3600


    # Convert hour into an angle on a 24-hour cycle.
    hour_angle = 2 * np.pi * hour / 24

    # Create cyclical time features.
    time_sin = np.sin(hour_angle)
    time_cos = np.cos(hour_angle)


    # ---------------------------------------------------------
    # Build feature vector
    # ---------------------------------------------------------

    # Extract V1 through V28 in the exact order
    # used during model training.
    v_features = [
        transaction[f"V{i}"]
        for i in range(1, 29)
    ]


    # Combine all 31 features.
    features = (
        v_features
        + [
            amount_log,
            time_sin,
            time_cos
        ]
    )

    # ---------------------------------------------------------
    # Scaling
    # ---------------------------------------------------------


    # Create a DataFrame using the exact feature names
    # used when the scaler was trained.
    features = pd.DataFrame(
        [features],
        columns=FEATURE_COLUMNS
    )

    # Apply the saved scaler.
    scaled_features = scaler.transform(features)


    # ---------------------------------------------------------
    # Prediction
    # ---------------------------------------------------------

    # Get the probability that the transaction is fraudulent.
    fraud_probability = model.predict_proba(
        scaled_features
    )[0][1]


    # Apply the threshold selected during validation.
    is_fraud = fraud_probability >= threshold


    # Return the prediction result.
    return {
        "fraud": bool(is_fraud),
        "probability": float(fraud_probability),
        "threshold": float(threshold)
    }


#TESTING

# if __name__ == "__main__":

#     # Create a sample transaction using the raw feature format.
#     sample_transaction = {
#         "Time": 0.0,
#         "Amount": 100.0,
#     }

#     # Add V1 through V28.
#     for i in range(1, 29):
#         sample_transaction[f"V{i}"] = 0.0


#     # Run the prediction.
#     result = predict_transaction(sample_transaction)


#     # Display the result.
#     print("Prediction result:")
#     print(result)

if __name__ == "__main__":

    import pandas as pd

    # Load the original dataset.
    dataset_path = (
        "../backend/src/main/resources/dataset/creditcard.csv"
    )

    df = pd.read_csv(dataset_path)

    # Take the first transaction from the dataset.
    row = df[df["Class"] == 1].iloc[0]

    # Build a raw transaction dictionary.
    sample_transaction = {
        "Time": float(row["Time"]),
        "Amount": float(row["Amount"])
    }

    # Add V1 through V28.
    for i in range(1, 29):
        sample_transaction[f"V{i}"] = float(row[f"V{i}"])

    # Store the actual label separately for verification.
    actual_class = int(row["Class"])

    # Run the prediction.
    result = predict_transaction(sample_transaction)

    print("Actual class:", actual_class)
    print("Prediction result:")
    print(result)