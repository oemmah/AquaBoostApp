package com.example.aquaboost;

/**
 * StandardScaler class for normalizing numeric features using z-score standardization
 * This class applies the transformation: (x - mean) / standard_deviation
 * Used to ensure all numeric features are on the same scale for machine learning models
 */
public class StandardScaler {
    // Array storing the mean values for each feature, calculated during model training
    private float[] means;

    // Array storing the standard deviation values for each feature, calculated during model training
    private float[] stds;

    /**
     * Constructor - Initialize the scaler with pre-calculated statistics
     * @param means Array of mean values for each feature (from training data)
     * @param stds Array of standard deviation values for each feature (from training data)
     */
    public StandardScaler(float[] means, float[] stds) {
        this.means = means;
        this.stds = stds;
    }

    /**
     * Transform input features using z-score standardization
     * Modifies the input array in-place: each feature[i] = (feature[i] - mean[i]) / std[i]
     * This ensures features have mean=0 and std=1, matching the training data distribution
     *
     * @param features Array of numeric features to be normalized (modified in-place)
     *                 Expected order: [age, weight, height] based on DehydrationPredictor usage
     */
    public void transform(float[] features) {
        // Apply z-score normalization to each feature
        for (int i = 0; i < features.length; i++) {
            // Standard formula: (value - mean) / standard_deviation
            features[i] = (features[i] - means[i]) / stds[i];
        }
    }
}