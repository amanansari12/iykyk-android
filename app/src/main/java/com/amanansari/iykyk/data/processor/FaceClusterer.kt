package com.amanansari.iykyk.data.processor

import android.util.Log
import com.amanansari.iykyk.data.model.FaceCluster
import com.amanansari.iykyk.data.model.FaceEmbeddingResult
import javax.inject.Inject

class FaceClusterer @Inject constructor(
    private val similarityCalculator: SimilarityCalculator
) {

    companion object {
        private const val MATCH_THRESHOLD = 0.72f
    }

    fun cluster(
        embeddings: List<FaceEmbeddingResult>
    ): List<FaceCluster> {

        val clusters = mutableListOf<FaceCluster>()

        embeddings.forEach { face ->

            var bestCluster: FaceCluster? = null
            var bestScore = Float.NEGATIVE_INFINITY

            clusters.forEach { cluster ->

                val clusterScore = cluster.members.maxOfOrNull { member ->
                    similarityCalculator.cosineSimilarity(
                        embedding1 = face.embedding,
                        embedding2 = member.embedding
                    )
                } ?: Float.NEGATIVE_INFINITY

                if (clusterScore > bestScore) {
                    bestScore = clusterScore
                    bestCluster = cluster
                }

            }

            if (bestCluster != null && bestScore >= MATCH_THRESHOLD) {
                bestCluster.members.add(face)

                Log.d(
                    "FaceClusterer",
                    "Face timestamp=${face.detectedFace.timestampMs} " +
                            "→ Cluster ${bestCluster.id}, score=$bestScore"
                )

            } else {
                val newCluster = FaceCluster(
                    id = clusters.size + 1,
                    members = mutableListOf(face)
                )

                clusters.add(newCluster)

                Log.d(
                    "FaceClusterer",
                    "New Cluster ${newCluster.id} " +
                            "for timestamp=${face.detectedFace.timestampMs}, " +
                            "bestScore=$bestScore"
                )
            }

        }

        return clusters
    }
}