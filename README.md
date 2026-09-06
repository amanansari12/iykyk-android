# Video-based Unique-Person Collage for Android

### An on-device Android application that processes portrait videos, detects faces, groups repeated appearances of the same person, counts their continuous appearances, selects representative frames, and generates a shareable collage.
---

## Screenshots

<!-- Drop screenshots or a short screen recording in here — a picture of this flow sells it better than any paragraph below. -->

| Home | Processing | Result | Saved Collages |
|---|---|---|---|
| <!-- screenshot --> | <!-- screenshot --> | <!-- screenshot --> | <!-- screenshot --> |

---

## What it does

1. Pick a portrait video from your gallery.
2. App decodes it, finds faces, and figures out which faces belong to the same person — even across separate appearances scattered through the clip.
3. For each person, it counts how long and how many times they were continuously visible, and picks their sharpest, most front-facing, eyes-open frame.
4. Everyone gets combined into one 9:16 collage, ready to save to your gallery or share straight from the app.

Two people sharing the screen count as two separate appearances, not one. One person stepping in and out of frame while another stays visible doesn't reset that second person's count. A blurry whip-pan through the crowd counts for nobody.

---

## How it works

Everything below runs locally, in a background coroutine, off the main thread.

```
Video
  │
  ▼
Metadata (duration, timing)
  │
  ▼
Sequential frame extraction  ── every ~200ms
  │
  ▼
Face detection                ── ML Kit, accurate mode, dedup overlapping boxes
  │
  ▼
Quality gating                ── size / crop / sharpness checks
  │
  ▼
Face embeddings                ── FaceNet-512 (on-device TFLite/LiteRT)
  │
  ▼
Clustering                     ── cosine similarity, greedy + consolidation pass
  │
  ▼
Appearance counting            ── continuous-visibility rule, 500ms gap tolerance
  │
  ▼
Best-shot selection            ── frontality + sharpness + eyes-open + smile
  │
  ▼
Collage rendering              ── 1080×1920 Story layout, custom Canvas
  │
  ▼
Save to gallery / Share / Keep in Saved Collages
```

### 1. Frame extraction

Frames are pulled roughly every **200 ms** using sequential decoding rather than repeated random seeks on `MediaMetadataRetriever` — seeking to arbitrary timestamps one at a time is noticeably slower, so the extractor walks the video forward instead.

### 2. Face detection

Each sampled frame goes through **ML Kit's Face Detection** in accurate mode with full classification enabled (smile probability, eye-open probability, head-pose angles). Overlapping duplicate detections on the same face are suppressed using a simple containment-based check before anything downstream sees them.

### 3. Quality gating

Not every detected face is good enough to embed. A face is dropped before it reaches the embedding model if it's:

- smaller than **80px** in either dimension,
- clipped more than **20%** outside the frame,
- larger than **80%** of the frame area (a false-positive sanity check), or
- blurrier than a minimum sharpness score (Laplacian variance of the grayscale crop, threshold **50.0**).

This keeps garbage detections from polluting the identity clusters later.

### 4. Face embeddings

Surviving faces are cropped and resized to 160×160 and passed through **FaceNet-512** (see [Face Embedding Model](#face-embedding-model) below) to get a 512-dimensional vector per face. Two crops of the same person land close together in that vector space; two different people land far apart.

### 5. Clustering

Faces are grouped by **cosine similarity between embeddings**, not by any assumption about how many people are in the video. Each face is greedily assigned to whichever existing cluster it's most similar to, above a match threshold; if nothing crosses that bar, it starts a new cluster. A second consolidation pass then folds small clusters into larger ones when enough of their members agree strongly with the same larger group — including recovering one-off singleton clusters that landed close in both similarity and time to a bigger cluster. See [Similarity Threshold](#similarity-threshold) for the exact numbers.

### 6. Appearance counting

An appearance is a **continuous visible segment** for one identity — it doesn't restart just because someone else entered or left the same frame. Timestamps for a cluster are sorted, and a gap of more than **500 ms** between consecutive sightings starts a new appearance. That tolerance exists because a face can be missed in a single sampled frame (motion, brief occlusion) without the person actually having left; 500ms was the smallest gap that stopped those misses from being counted as separate appearances during testing.

### 7. Best-shot selection

For every identity, one representative frame is chosen with a weighted score across:

| Signal | Weight | What it rewards |
|---|---:|---|
| Frontality | 35% | Head yaw/pitch close to facing the camera |
| Sharpness | 30% | In-focus, not motion-blurred |
| Eyes open | 25% | Average of left/right eye-open probability |
| Smiling | 10% | Pleasant expression |

The chosen frame is **cropped generously** around the face (2.5× the detected box) rather than clipped tightly to the bounding box — a tight crop looks pixelated once it's blown up into a collage tile.

### 8. Collage rendering

The collage is drawn by hand on an Android `Canvas`, not stitched from static assets — a 1080×1920 (9:16, Story-shaped) canvas with the first identity getting a larger "hero" tile: a softly blurred, zoomed copy of their own photo as the tile background, with their normal portrait centered on top and a light vignette over the whole thing. Everyone else gets a clean, evenly-cropped tile below. The background blur is a small hand-rolled box blur (blur small, then upscale) rather than `RenderEffect`, since the canvas here is a plain software bitmap and `RenderEffect` silently no-ops off a hardware-accelerated surface.

### 9. Save, share, and revisit

The finished collage can be written straight to the gallery (`Pictures/iykyk`, via `MediaStore` on modern Android, with a legacy file-based fallback), or handed to the standard Android share sheet through a `FileProvider`. No storage permission prompt is needed for either the video picker (Android's built-in Photo Picker) or the gallery save (scoped storage). Collages you choose to keep also land in an in-app **Saved Collages** library, backed by Room, so you can come back and re-share or re-save one later without reprocessing the video.

---

## Screens

- **Home** — pick a video from the Photo Picker and kick off processing.
- **Processing** — a live, staged progress view walking through metadata → frames → detection → embeddings → clustering → counting → best shots → collage.
- **Result** — the finished collage plus a per-person card showing their appearance count, with Save-to-Gallery and Share actions.
- **Saved Collages** — a library of everything you've kept, stored locally.

---

## Face Embedding Model

**Model:** FaceNet-512

The face recognition/clustering stage uses **FaceNet-512**, a convolutional neural network that maps a 160×160 face crop to a 512-dimensional embedding vector, such that embeddings of the same person land close together (by cosine similarity) and embeddings of different people land far apart.

- **Original model:** FaceNet-512, from Sefik Ilkin Serengil's [`deepface`](https://github.com/serengil/deepface) library.
- **TFLite conversion source:** [`shubham0204/OnDevice-Face-Recognition-Android`](https://github.com/shubham0204/OnDevice-Face-Recognition-Android) (Shubham Panchal), commit [`fd0938d`](https://github.com/shubham0204/OnDevice-Face-Recognition-Android/commit/fd0938d) — `app/src/main/assets/facenet_512.tflite`. *(This file has since been removed from the repo's current `main` branch, as that project migrated to ExecuTorch; retrieved from the git history at the commit above.)*
- **Runtime:** Google's LiteRT (the successor to standalone TensorFlow Lite), running fully on-device via `com.google.ai.edge.litert`.
- **Input:** 160×160×3 RGB, normalized to **[-1, 1]** (`(pixel - 127.5) / 127.5`).
- **Output:** 512-dimensional float embedding.
- **Similarity metric:** cosine similarity between embeddings.

## Similarity Threshold

**Clustering match threshold: `0.72`** cosine similarity — a face joins an existing identity cluster only if it scores at least this high against that cluster's closest member; otherwise it seeds a new cluster.

A second, stricter pass helps recover clusters that got fragmented: a same-size or smaller cluster is folded into a larger one if enough of its members individually score above `0.72` against that larger cluster, with the group's median score also required to clear `0.72`. A leftover cluster of exactly one face is merged into a larger cluster if that single face scores **`0.80`** or higher against it *and* the two sightings are within **1 second** of each other in the video — a tighter bar, since a lone face has no other members to corroborate the match.

<details>
<summary><strong>How 0.72 was actually arrived at</strong></summary>

<br>

0.72 didn't come from a formula or a paper — it came from measuring how FaceNet-512 behaves on this app's own footage across several calibration passes, then designing for the worst case actually observed rather than the best-looking run.

**The method:** generate embeddings for detected faces, compute cosine similarity for known same-person pairs and known different-person pairs, and see where the two distributions actually separate.

**A false alarm, early on.** An initial exploratory run over 46 same-person and 11 different-person comparisons showed some same-person similarities dropping to near-zero — even negative. That looked like the embedding model was unreliable. It wasn't: a few ML Kit face boxes were badly malformed (one wider than the entire 1080px frame), so the embedder was being fed crops that were mostly *not* face. That's what motivated the quality-gating step described above (minimum face size, visibility ratio, sharpness floor, max face-area ratio) — it exists specifically to keep broken detections out of both production and the calibration data itself.

**Three clean calibration runs**, once quality filtering was in place:

| Run | Lowest clean same-person similarity | Highest different-person similarity |
|---|---:|---:|
| Run 1 | 0.790 | 0.685 |
| Run 2 | 0.739 | 0.688 |
| Run 3 | 0.767 | 0.660 |

Across these three runs: 89 same-person comparisons and 20 different-person comparisons (109 total), on top of earlier exploratory testing (57 comparisons, then 42 after an initial filtering pass).

**Reading the runs together, not in isolation:**
- Worst clean same-person similarity observed: **0.739** (Run 2)
- Worst (highest) different-person similarity observed: **0.688** (Run 2)

That's an observed separation of only about **0.05**. A threshold of `0.75` would have rejected the legitimate 0.739 same-person match from Run 2 — misclassifying one person as two. A threshold of `0.70` would sit only 0.012 above the observed 0.688 different-person ceiling, leaving almost no margin against noise. `0.72` lands just above the midpoint of that gap (≈0.714), erring slightly toward *not* merging two different people rather than risk-splitting one person into two.

**A handful of genuine outliers were deliberately not used to drag the threshold down.** Even after quality filtering, isolated same-person comparisons still occasionally scored as low as 0.21 or negative — clearly anomalous against their neighboring frames. Rather than lowering the threshold to accommodate rare bad frames, the clustering step compares a new face against *multiple* stored embeddings per identity rather than a single frame pair, so one outlier frame can't unilaterally split or merge an identity.

**What this number is not:** a universal FaceNet constant. It's a starting point calibrated to this app's specific model, preprocessing, face detector, and video type — changing any of those would call for recalibrating against fresh data rather than assuming 0.72 still holds.

</details>

Both thresholds were arrived at by testing against the sample clips and watching where identities either split apart or bled into each other, rather than being picked in the abstract — they're a reasonable starting point for similar portrait footage, not a universal constant.

---

## Tech Stack

| Area | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Architecture | MVVM + Repository + Processor layers |
| Dependency Injection | Hilt |
| Face Detection | Google ML Kit Face Detection |
| Face Recognition | FaceNet-512 on-device (LiteRT / TFLite) |
| Video Decoding | `MediaMetadataRetriever`, sequential frame extraction |
| Persistence | Room (Saved Collages library) |
| Concurrency | Kotlin Coroutines |
| Min SDK / Target SDK | 26 / 37 |

Nothing here talks to a backend. The entire pipeline — detection, embedding, clustering, rendering — runs on the device the video was picked from.

---

## Project Structure

```
app/src/main/java/com/amanansari/iykyk/
├── data/
│   ├── model/                    # DetectedFace, FaceCluster, PersonResult, UI state, etc.
│   ├── local/                    # Room: AppDatabase, SavedCollageDao, SavedCollageEntity
│   ├── processor/
│   │   ├── VideoMetadataExtractor.kt
│   │   ├── FrameExtractor.kt
│   │   ├── FaceDetector.kt
│   │   ├── FaceEmbedding.kt
│   │   ├── SimilarityCalculator.kt
│   │   ├── FaceClusterer.kt
│   │   ├── AppearanceCounter.kt
│   │   ├── BestShotSelector.kt
│   │   ├── CollageGenerator.kt
│   │   ├── ClusterImageSaver.kt
│   │   ├── CollageLibraryStorage.kt
│   │   └── MediaExporter.kt
│   └── repository/
│       ├── ProcessingRepository.kt
│       └── SavedCollageRepository.kt
├── di/                            # Hilt modules
├── navigation/                    # NavGraph, Routes
├── ui/
│   ├── screen/                    # Home, Processing, Result, SavedCollages
│   ├── component/
│   ├── theme/
│   └── viewmodel/
└── IykykApplication.kt
```

Hilt keeps every processing stage independently constructible and testable, instead of wiring dependencies by hand inside the UI layer.

---

## Sample Videos

A few sample portrait clips to try it against: **[Sample videos folder](https://drive.google.com/drive/folders/1SFeS08KWiH0DBl3ua57C68plEhBW1xP2)**

The app isn't tuned to these specific clips — it's meant to work on any similar portrait video, so treat these as a starting point rather than the only footage it supports.

---

## Build and Run

**Requirements**

- Android Studio (recent stable)
- Android SDK 26+ (compiles against 37)
- A physical device or emulator running Android 8.0+

**Steps**

```bash
git clone <your-repository-url>
cd iykyk
```

Open the project in Android Studio, let Gradle sync, select a device or emulator, and run the `app` configuration. The on-device FaceNet model (~45MB) ships inside `app/src/main/assets`, so the first sync/build can take a little longer than a typical Compose project.

No `.env` files, API keys, or backend setup required — it just runs.

---

## Release

<!-- Fill in once published -->

| Channel | Link |
|---|---|
| Google Play | `<ADD PLAY STORE LISTING LINK HERE>` |
| Direct APK download | `<ADD APK DOWNLOAD / RELEASES LINK HERE>` |

---

## Privacy

Video decoding, face detection, embedding generation, and clustering all happen locally on-device. Nothing about the video, the faces in it, or the generated collage is sent anywhere.

---

## Roadmap / Known Rough Edges

- Identity grouping can still fragment on tricky cases — extreme profile angles, heavy occlusion, or two people who look a lot alike.
- Representative-shot quality is good but not perfect on frames with fast motion between sampled timestamps.
- Collage layout currently has a dedicated design for up to 5 people per video; larger groups fall back to a simpler grid.
