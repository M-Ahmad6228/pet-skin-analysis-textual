from sklearn.preprocessing import LabelEncoder
import numpy as np

# Initialize the label encoder globally
label_encoder = LabelEncoder()
def fit_encoder(orignal_labels):
    labels = np.asarray(orignal_labels).ravel()
    label_encoder.fit(labels)  # Fit once
    return label_encoder.transform(labels)

def decode_labels(predicted_class):
    pc = np.asarray(predicted_class).ravel()
    return label_encoder.inverse_transform(pc)

