package com.amanansari.iykyk.data.processor

import android.util.Log
import com.amanansari.iykyk.data.model.FaceCluster
import javax.inject.Inject

class AppearanceCounter @Inject constructor() {

    companion object {
        private const val MAX_GAP_MS = 500L
    }

//    fun countAppearances(
//        cluster: FaceCluster
//    ): Int {
//
//        if (cluster.members.isEmpty()) {
//            return 0
//        }
//
//        val timestamps = cluster.members
//            .map { it.detectedFace.timestampMs }
//            .sorted()
//
//       Log.d(
//            "AppearanceCounter",
//            "Cluster ${cluster.id} timestamps=$timestamps"
//        )
//
//        var appearanceCount = 1
//
//        for (i in 1 until timestamps.size) {
//
//            val gap = timestamps[i] - timestamps[i - 1]
//
//
//            Log.d(
//                "AppearanceCounter",
//                "Cluster ${cluster.id}: " +
//                        "timestamp=${timestamps[i - 1]} → ${timestamps[i]}, " +
//                        "gap=$gap"
//            )
//
//            if (gap > MAX_GAP_MS) {
//                appearanceCount++
//            }
//        }
//
//        return appearanceCount
//    }


    fun countAppearances(
        cluster: FaceCluster
    ): Int {

        if (cluster.members.isEmpty()) {
            return 0
        }

        val timestamps = cluster.members
            .map { it.detectedFace.timestampMs }
            .sorted()

        var appearanceCount = 1
        var appearanceStart = timestamps.first()

        for (i in 1 until timestamps.size) {

            val previousTimestamp = timestamps[i - 1]
            val currentTimestamp = timestamps[i]

            val gap = currentTimestamp - previousTimestamp

            if (gap > MAX_GAP_MS) {

                Log.d(
                    "AppearanceCounter",
                    "Cluster ${cluster.id}: " +
                            "appearance $appearanceCount = " +
                            "${appearanceStart}ms to ${previousTimestamp}ms"
                )

                appearanceCount++
                appearanceStart = currentTimestamp
            }
        }

        Log.d(
            "AppearanceCounter",
            "Cluster ${cluster.id}: " +
                    "appearance $appearanceCount = " +
                    "${appearanceStart}ms to ${timestamps.last()}ms"
        )

        Log.d(
            "AppearanceCounter",
            "Cluster ${cluster.id} → " +
                    "$appearanceCount appearances"
        )

        return appearanceCount
    }
}