import pandas as pd
import numpy as np
from sklearn.preprocessing import MinMaxScaler
import tensorflow as tf
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import LSTM, Dense, Dropout, Input
import os

# 1. Load Data
csv_path = r"G:\MyGithub\TensorFlowsLite\glucose.csv"
print(f"Loading data from {csv_path}...")
df = pd.read_csv(csv_path)

# 2. Preprocessing
glucose_series = []
df['Ngày'] = pd.to_datetime(df['Ngày'])
df = df.sort_values('Ngày')

for index, row in df.iterrows():
    before = row['Đường huyết trước (mmol/L)']
    after = row['Đường huyết sau 2 giờ (mmol/L)']
    if pd.notnull(before): glucose_series.append(float(before))
    if pd.notnull(after): glucose_series.append(float(after))

values = np.array(glucose_series).reshape(-1, 1)

if len(values) < 10:
    print("Error: Not enough data points.")
    exit()

print(f"Total data points: {len(values)}")

# 3. Normalize
scaler = MinMaxScaler(feature_range=(0, 1))
scaled_data = scaler.fit_transform(values)

print(f"CALIBRATION_MIN = {scaler.data_min_[0]}f")
print(f"CALIBRATION_MAX = {scaler.data_max_[0]}f")

# 4. Create sequences
def create_sequences(data, seq_length=5):
    X, y = [], []
    for i in range(len(data) - seq_length):
        X.append(data[i:i+seq_length])
        y.append(data[i+seq_length])
    return np.array(X, dtype=np.float32), np.array(y, dtype=np.float32)

X, y = create_sequences(scaled_data, seq_length=5)

# 5. Build LSTM Model
# Using Input layer and default activations for better TFLite compatibility
model = Sequential([
    Input(shape=(5, 1)),
    LSTM(64, return_sequences=True),
    Dropout(0.1),
    LSTM(32),
    Dense(1)
])

model.compile(optimizer='adam', loss='mse')

# 6. Train
print("Starting training...")
model.fit(X, y, epochs=150, batch_size=4, verbose=0)

# 7. Convert to TFLite with fix for LSTM
converter = tf.lite.TFLiteConverter.from_keras_model(model)

# Fixing the LSTM conversion error
converter.target_spec.supported_ops = [
    tf.lite.OpsSet.TFLITE_BUILTINS,
    tf.lite.OpsSet.SELECT_TF_OPS
]
converter._experimental_lower_tensor_list_ops = False

tflite_model = converter.convert()

# 8. Save Model
output_path = os.path.join("app", "src", "main", "assets", "lstm_model.tflite")
with open(output_path, "wb") as f:
    f.write(tflite_model)

print(f"Model saved to {output_path}")
