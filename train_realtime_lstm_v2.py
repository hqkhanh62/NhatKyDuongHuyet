import pandas as pd
import numpy as np
from sklearn.preprocessing import MinMaxScaler
import tensorflow as tf
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import LSTM, Dense, Dropout, Input
import os
import time

def train_and_export():
    # 1. Load Data - Prioritize the auto-exported file from Android
    android_export_path = r"G:\NhatKyDuongHuyet_PRO_MAX_FINAL\app\src\main\assets\glucose_latest.csv" # Adjusted for your local dev
    if not os.path.exists(android_export_path):
        android_export_path = r"G:\MyGithub\TensorFlowsLite\glucose.csv"

    print(f"Loading data from {android_export_path}...")
    try:
        df = pd.read_csv(android_export_path)
    except:
        print("Waiting for data...")
        return

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
    if len(values) < 10: return

    # 3. Normalize (Using same range for consistency)
    scaler = MinMaxScaler(feature_range=(0, 1))
    scaled_data = scaler.fit_transform(values)

    # 4. Create sequences
    def create_sequences(data, seq_length=5):
        X, y = [], []
        for i in range(len(data) - seq_length):
            X.append(data[i:i+seq_length])
            y.append(data[i+seq_length])
        return np.array(X, dtype=np.float32), np.array(y, dtype=np.float32)

    X, y = create_sequences(scaled_data, seq_length=5)

    # 5. Build Model
    model = Sequential([
        Input(shape=(5, 1)),
        LSTM(64, return_sequences=True),
        Dropout(0.1),
        LSTM(32),
        Dense(1)
    ])
    model.compile(optimizer='adam', loss='mse')

    # 6. Train
    print("AI is learning from new data...")
    model.fit(X, y, epochs=150, batch_size=4, verbose=0)

    # 7. Convert
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS, tf.lite.OpsSet.SELECT_TF_OPS]
    converter._experimental_lower_tensor_list_ops = False
    tflite_model = converter.convert()

    # 8. Save
    output_path = os.path.join("app", "src", "main", "assets", "lstm_model.tflite")
    with open(output_path, "wb") as f:
        f.write(tflite_model)
    print(f"✅ Success! New brain model saved to {output_path}")

if __name__ == "__main__":
    print("=== AI AUTO-TRAIN WATCHER STARTED ===")
    last_mtime = 0
    file_to_watch = r"G:\MyGithub\TensorFlowsLite\glucose.csv" # Point this to your sync folder

    while True:
        try:
            mtime = os.path.getmtime(file_to_watch)
            if mtime > last_mtime:
                train_and_export()
                last_mtime = mtime
        except:
            pass
        time.sleep(10) # Check every 10 seconds
