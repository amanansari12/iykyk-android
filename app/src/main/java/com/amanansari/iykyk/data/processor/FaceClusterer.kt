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

        val finalClusters = consolidateClusters(clusters)

        logCluster1VsCluster2(finalClusters)

        Log.d(
            "FaceClusterer",
            "Final clusters: ${finalClusters.size}"
        )

        finalClusters.forEach { cluster ->
            Log.d(
                "FaceClusterer",
                "Final Cluster ${cluster.id}: ${cluster.members.size} members"
            )
        }

        return finalClusters
    }


    private fun consolidateClusters(
        clusters: MutableList<FaceCluster>
    ): List<FaceCluster> {

        var changed: Boolean

        do {
            changed = false

            val smallClusters = clusters
                .filter { it.members.size <= 6 }
                .sortedBy { it.members.size }

            outer@ for (smallCluster in smallClusters) {

                val candidateClusters = clusters
                    .filter { it.id != smallCluster.id }
                    .filter { it.members.size > smallCluster.members.size }

                for (candidateCluster in candidateClusters) {

                    // -----------------------------------------
                    // Singleton recovery
                    // -----------------------------------------
                    if (smallCluster.members.size == 1) {

                        val fragmentMember = smallCluster.members.first()

                        val bestCandidate = candidateCluster.members.maxByOrNull {
                            similarityCalculator.cosineSimilarity(
                                embedding1 = fragmentMember.embedding,
                                embedding2 = it.embedding
                            )
                        }

                        if (bestCandidate != null) {

                            val similarity =
                                similarityCalculator.cosineSimilarity(
                                    embedding1 = fragmentMember.embedding,
                                    embedding2 = bestCandidate.embedding
                                )

                            val timeDifference =
                                kotlin.math.abs(
                                    fragmentMember.detectedFace.timestampMs -
                                            bestCandidate.detectedFace.timestampMs
                                )

                            val singletonSimilarityThreshold = 0.80f
                            val maxTemporalGapMs = 1000L

                            if (
                                similarity >= singletonSimilarityThreshold &&
                                timeDifference <= maxTemporalGapMs
                            ) {
                                candidateCluster.members.add(
                                    fragmentMember
                                )

                                clusters.remove(smallCluster)

                                Log.d(
                                    "FaceClusterer",
                                    "Merged singleton cluster " +
                                            "${smallCluster.id} into cluster " +
                                            "${candidateCluster.id}" +
                                            " | similarity=$similarity" +
                                            " | timeDifference=$timeDifference"
                                )

                                changed = true
                                break@outer
                            }
                        }

                        continue
                    }

                    // -----------------------------------------
                    // Existing multi-member consolidation
                    // -----------------------------------------
                    val memberScores = smallCluster.members.map { fragmentMember ->

                        candidateCluster.members.maxOf { candidateMember ->

                            similarityCalculator.cosineSimilarity(
                                embedding1 = fragmentMember.embedding,
                                embedding2 = candidateMember.embedding
                            )
                        }
                    }

                    val strongMatches = memberScores.count {
                        it >= MATCH_THRESHOLD
                    }

                    val requiredMatches = maxOf(
                        2,
                        (smallCluster.members.size + 1) / 2
                    )

                    val sortedScores = memberScores.sorted()
                    val medianScore = sortedScores[
                        sortedScores.size / 2
                    ]

                    if (
                        strongMatches >= requiredMatches &&
                        medianScore >= MATCH_THRESHOLD
                    ) {

                        candidateCluster.members.addAll(
                            smallCluster.members
                        )

                        clusters.remove(smallCluster)

                        Log.d(
                            "FaceClusterer",
                            "Merged cluster ${smallCluster.id} " +
                                    "into cluster ${candidateCluster.id}" +
                                    " | strongMatches=$strongMatches" +
                                    " | required=$requiredMatches" +
                                    " | median=$medianScore"
                        )

                        changed = true
                        break@outer
                    }
                }
            }

        } while (changed)

        return clusters
    }


    //! Temporary Function
    private fun logCluster1VsCluster2(
        clusters: List<FaceCluster>
    ) {
        val cluster1 = clusters.firstOrNull { it.id == 1 } ?: return
        val cluster2 = clusters.firstOrNull { it.id == 2 } ?: return

        Log.d(
            "ClusterComparison",
            "========== C1 vs C2 =========="
        )

        cluster1.members.forEach { member1 ->

            val scores = cluster2.members.map { member2 ->
                similarityCalculator.cosineSimilarity(
                    embedding1 = member1.embedding,
                    embedding2 = member2.embedding
                )
            }

            val bestScore = scores.maxOrNull() ?: 0f

            Log.d(
                "ClusterComparison",
                "C1 timestamp=${member1.detectedFace.timestampMs} " +
                        "best C2 similarity=$bestScore"
            )
        }

        Log.d(
            "ClusterComparison",
            "=============================="
        )
    }





    fun logMemberSimilarities(
        clusters: List<FaceCluster>
    ) {
        val clusterIdsToCheck = listOf(6, 8, 9, 10)

        val mainCluster = clusters.firstOrNull { it.id == 5 }
            ?: return

        clusterIdsToCheck.forEach { candidateId ->

            val candidateCluster =
                clusters.firstOrNull { it.id == candidateId }
                    ?: return@forEach

            Log.d(
                "MemberStats",
                "========== C5 vs C$candidateId =========="
            )

            candidateCluster.members.forEachIndexed { candidateIndex, candidate ->

                val similarities = mainCluster.members.map { main ->

                    val similarity =
                        similarityCalculator.cosineSimilarity(
                            embedding1 = candidate.embedding,
                            embedding2 = main.embedding
                        )

                    similarity
                }

                val sortedSimilarities =
                    similarities.sortedDescending()

                val topK =
                    minOf(5, sortedSimilarities.size)

                val topValues =
                    sortedSimilarities.take(topK)

                Log.d(
                    "MemberStats",
                    "C$candidateId member[$candidateIndex] " +
                            "timestamp=${candidate.detectedFace.timestampMs} " +
                            "top$topK=$topValues"
                )
            }
        }

        Log.d(
            "MemberStats",
            "=========================================="
        )
    }

    fun logClusterTimeline(
        clusters: List<FaceCluster>
    ) {
        Log.d(
            "ClusterTimeline",
            "========== CLUSTER TIMELINE =========="
        )

        val clusterIdsToCheck = setOf(5, 6, 8, 9, 10)

        clusters
            .filter { it.id in clusterIdsToCheck }
            .flatMap { cluster ->
                cluster.members.map { member ->
                    cluster.id to member.detectedFace.timestampMs
                }
            }
            .sortedBy { it.second }
            .forEach { (clusterId, timestamp) ->

                Log.d(
                    "ClusterTimeline",
                    "timestamp=$timestamp → C$clusterId"
                )
            }

        Log.d(
            "ClusterTimeline",
            "======================================"
        )
    }

    fun logClusterBoundingBoxes(
        clusters: List<FaceCluster>
    ) {
        Log.d(
            "ClusterBoxes",
            "========== C5/C6 BOUNDING BOXES =========="
        )

        clusters
            .filter { it.id == 5 || it.id == 6 }
            .flatMap { cluster ->
                cluster.members.map { member ->
                    Triple(
                        cluster.id,
                        member.detectedFace.timestampMs,
                        member.detectedFace.boundingBox
                    )
                }
            }
            .sortedWith(
                compareBy<Triple<Int, Long, android.graphics.Rect>>(
                    { it.second },
                    { it.first }
                )
            )
            .forEach { (clusterId, timestamp, box) ->

                Log.d(
                    "ClusterBoxes",
                    "timestamp=$timestamp | " +
                            "C$clusterId | " +
                            "box=$box"
                )
            }

        Log.d(
            "ClusterBoxes",
            "=========================================="
        )
    }

    fun logClusterPairStatistics(
        clusters: List<FaceCluster>
    ) {
        Log.d("ClusterStats", "========== CLUSTER PAIR STATISTICS ==========")

        for (i in 0 until clusters.size) {
            for (j in i + 1 until clusters.size) {

                val clusterA = clusters[i]
                val clusterB = clusters[j]

                val similarities = mutableListOf<Float>()

                clusterA.members.forEach { memberA ->
                    clusterB.members.forEach { memberB ->

                        val similarity =
                            similarityCalculator.cosineSimilarity(
                                embedding1 = memberA.embedding,
                                embedding2 = memberB.embedding
                            )

                        similarities.add(similarity)
                    }
                }

                if (similarities.isEmpty()) {
                    continue
                }

                val sortedSimilarities =
                    similarities.sortedDescending()

                val topK =
                    minOf(3, sortedSimilarities.size)

                val topValues =
                    sortedSimilarities.take(topK)

                val topKMean =
                    topValues.average()

                val topKMin =
                    topValues.minOrNull() ?: 0.0

                Log.d(
                    "ClusterStats",
                    "C${clusterA.id} vs C${clusterB.id} | " +
                            "comparisons=${similarities.size} | " +
                            "top$topK=$topValues | " +
                            "top${topK}Mean=$topKMean | " +
                            "top${topK}Min=$topKMin"
                )
            }
        }

        Log.d("ClusterStats", "==============================================")
    }


}