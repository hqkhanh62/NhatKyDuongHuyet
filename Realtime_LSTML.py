import pandas as pd
import numpy as np
from sklearn.preprocessing import MinMaxScaler
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import LSTM, Dense

# Load data
df = pd.read_csv("glucose_clean.csv")

# sort theo thời gian
df = df.sort_values("timestamp")

values = df["glucose"].values.reshape(-1,1)

# normalize
scaler = MinMaxScaler()
scaled = scaler.fit_transform(values)

# create sequences
def create_dataset(data, window=5):
    X, y = [], []
    for i in range(len(data)-window):
        X.append(data[i:i+window])
        y.append(data[i+window])
    return np.array(X), np.array(y)

X, y = create_dataset(scaled, window=5)

# LSTM model
model = Sequential([
    LSTM(64, return_sequences=True, input_shape=(5,1)),
    LSTM(32),
    Dense(1)
])

model.compile(optimizer='adam', loss='mse')
model.fit(X, y, epochs=50, batch_size=8)

# convert to TFLite
import tensorflow as tf

converter = tf.lite.TFLiteConverter.from_keras_model(model)
tflite_model = converter.convert()

with open("lstm_glucose.tflite", "wb") as f:
    f.write(tflite_model)