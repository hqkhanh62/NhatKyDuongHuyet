import pandas as pd
import numpy as np
import tensorflow as tf
from sklearn.preprocessing import MinMaxScaler
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import LSTM, Dense, Dropout

# 1. Load Data
# Note: Update path if necessary
csv_path = r"F:\Khanh Download\glucose (1).csv"
df = pd.read_csv(csv_path)

# 2. Preprocess
# Extract values (we'll use 'Duong huyet truoc' and 'Duong huyet sau' combined or separately)
# For this example, let's look at all measurements in sequence
data = []
for index, row in df.iterrows():
    if not pd.isna(row['ÄÆ°á»ng huyáº¿t trÆ°á»\n›c (mmol/L)']):
        data.append(row['ÄÆ°á»ng huyáº¿t trÆ°á»\n›c (mmol/L)'])
    if not pd.isna(row['ÄÆ°á»ng huyáº¿t sau 2 giá» (mmol/L)']):
        data.append(row['ÄÆ°á»ng huyáº¿t sau 2 giá» (mmol/L)'])

# Convert to numpy array
data = np.array(data).reshape(-1, 1)

# Normalize
scaler = MinMaxScaler(feature_range=(0, 1))
scaled_data = scaler.fit_transform(data)

# Create sequences (5 time steps to predict the next)
X, y = [], []
for i in range(5, len(scaled_data)):
    X.append(scaled_data[i-5:i, 0])
    y.append(scaled_data[i, 0])

X, y = np.array(X), np.array(y)
X = np.reshape(X, (X.shape[0], X.shape[1], 1))

# 3. Build Model
model = Sequential([
    LSTM(units=50, return_sequences=True, input_shape=(X.shape[1], 1)),
    Dropout(0.2),
    LSTM(units=50),
    Dropout(0.2),
    Dense(units=1)
])

model.compile(optimizer='adam', loss='mean_squared_error')
model.fit(X, y, epochs=20, batch_size=32)

# 4. Save and Convert to TFLite
model.save("lstm_model.h5")

converter = tf.lite.TFLiteConverter.from_keras_model(model)
tflite_model = converter.convert()

with open("lstm_model.tflite", "wb") as f:
    f.write(tflite_model)

# Save scaler params for Android (Min/Max)
print(f"Scaler Min: {scaler.data_min_}")
print(f"Scaler Max: {scaler.data_max_}")
print("Training complete. Copy 'lstm_model.tflite' to your project's assets folder.")
