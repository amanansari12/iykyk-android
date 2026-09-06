package com.amanansari.iykyk.data.model

data class ClusteringResult(
    val clusters: List<FaceCluster>,
    val appearanceCounts: Map<Int, Int>
)