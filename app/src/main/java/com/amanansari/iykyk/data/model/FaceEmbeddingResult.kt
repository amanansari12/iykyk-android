package com.amanansari.iykyk.data.model

class FaceEmbeddingResult(
    val detectedFace: DetectedFace,
    val embedding: FloatArray,
    val sharpness: Double,
    val visibleRatio: Float
)