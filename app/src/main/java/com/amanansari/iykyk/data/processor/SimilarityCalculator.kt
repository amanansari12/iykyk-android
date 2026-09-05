package com.amanansari.iykyk.data.processor

import javax.inject.Inject
import kotlin.math.sqrt

class SimilarityCalculator @Inject constructor() {

    fun cosineSimilarity(
        embedding1: FloatArray,
        embedding2: FloatArray
    ): Float {
        require(embedding1.size == embedding2.size) {
            "Embedding sizes must be equal"
        }

        var dotProduct = 0.0
        var magnitude1 = 0.0
        var magnitude2 = 0.0

        for (i in embedding1.indices) {
            dotProduct += embedding1[i] * embedding2[i]
            magnitude1 += embedding1[i] * embedding1[i]
            magnitude2 += embedding2[i] * embedding2[i]
        }

        val denominator = sqrt(magnitude1) * sqrt(magnitude2)

        if (denominator == 0.0) {
            return 0f
        }

        return (dotProduct / denominator).toFloat()
    }
}