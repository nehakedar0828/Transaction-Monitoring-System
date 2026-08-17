import pandas as pd


# Path to the existing credit-card dataset.
DATASET_PATH = "../backend/src/main/resources/dataset/creditcard.csv"


# Load the CSV dataset into a Pandas DataFrame.
df = pd.read_csv(DATASET_PATH)


# Count the number of completely duplicated rows.
duplicate_count = df.duplicated().sum()

# Display the number of duplicate rows.
print("\nDuplicate rows:")
print(duplicate_count)


# Display the number of rows and columns.
print("Dataset shape:")
print(df.shape)


# Display all column names.
print("\nColumns:")
print(df.columns.tolist())


# Display the first five transactions.
print("\nFirst 5 rows:")
print(df.head())


# Display the data types of every column.
print("\nData types:")
print(df.dtypes)


# Check whether any columns contain missing values.
print("\nMissing values:")
print(df.isnull().sum())


# Count genuine and fraudulent transactions.
print("\nClass distribution:")
print(df["Class"].value_counts())


# Calculate the percentage of fraudulent transactions.
fraud_percentage = (
    df["Class"].value_counts(normalize=True)[1] * 100
)

# Display the percentage of fraudulent transactions.
print("\nFraud percentage:")
print(f"{fraud_percentage:.4f}%")