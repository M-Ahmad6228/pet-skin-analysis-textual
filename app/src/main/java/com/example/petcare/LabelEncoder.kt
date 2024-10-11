package com.example.petcare

class LabelEncoder {
    private val labelToIndex = mutableMapOf<String, Int>()
    private val indexToLabel = mutableMapOf<Int, String>()
    private var currentIndex = 0

    // Fit the encoder to a list of labels
    fun fit(labels: List<String>) {
        for (label in labels) {
            if (label !in labelToIndex) {
                labelToIndex[label] = currentIndex
                indexToLabel[currentIndex] = label
                currentIndex++
            }
        }
    }

    // Transform a single label to its corresponding index
    fun transform(label: String): Int {
        return labelToIndex[label] ?: throw IllegalArgumentException("Label '$label' not found in encoder.")
    }

    // Transform a list of labels to their corresponding indices
    fun transform(labels: List<String>): IntArray {
        return IntArray(labels.size) { transform(labels[it]) }
    }

    // Inverse transform an index back to its corresponding label
    fun inverseTransform(index: Int): String {
        return indexToLabel[index] ?: throw IllegalArgumentException("Index '$index' not found in encoder.")
    }

    // Inverse transform a list of indices back to their corresponding labels
    fun inverseTransform(indices: IntArray): List<String> {
        return indices.map { inverseTransform(it) }
    }

    // Get the unique classes after fitting
    fun getClasses(): List<String> {
        return labelToIndex.keys.toList()
    }
}
