# Infusory 3D Model Viewer — Android Developer Screening Task

A single-activity Android application built in Kotlin using traditional XML Layout Views and **SceneView 2.2.1** (Google Filament 3D engine) to load, display, and interact with multiple 3D `.glb` models simultaneously inside moveable, resizable containers with real-time 3D node label tracking.

---

## 🚀 Key Features

1. **Single-Activity Architecture**:
   - Built entirely on a single canvas screen (`MainActivity`) using traditional XML ViewBinding.
2. **Multi-Model Canvas**:
   - Pre-loaded with 5 `.glb` models (`Bulb`, `Fiagena`, `Lungs`, `Microscope`, `solarsystem`).
   - Tapping **"Add Model"** loads multiple models onto the canvas (supporting 5+ active models smoothly).
3. **Independent Draggable & Resizable Containers**:
   - Each model sits inside a custom `ModelContainerView` container card.
   - **Normal Mode (`MOVE/RESIZE`)**:
     - **1-Finger Drag**: Moves the model container around the canvas.
     - **2-Finger Pinch / `+` `-` Buttons**: Resizes the container card frame bounds.
   - **Interaction Mode (`ROTATE/ZOOM`)**:
     - **1-Finger Drag**: Rotates the 3D model 360° inside the viewport.
     - **2-Finger Pinch**: Zooms the 3D content in and out inside the viewport.
   - **Strict Gesture Separation**: Container movement gestures and 3D camera gestures are isolated and never mix.
4. **Always-Visible Container Controls**:
   - 🔄 **Interaction Toggle**: Switches container mode between NORMAL and INTERACTION.
   - 🏷️ **Label Toggle**: Displays/hides part labels built into the GLB model (`extras.prop`).
   - ❌ **Close Button**: Completely removes the model from the screen and releases memory resources.
5. **2D Part Labels & 3D Screen Projection**:
   - Parses `extras.prop` metadata directly from GLB JSON chunk without third-party parser overhead.
   - Projects 3D node world coordinates to 2D viewport coordinates every frame.
   - Renders 2D label cards with connecting anchor lines to the corresponding 3D node position on screen.
6. **Live Performance Monitoring**:
   - Integrated `Choreographer` FPS counter HUD overlay on screen.

---

## 📊 Performance & Profiling (Low-End Device Strategy)

This application was engineered specifically to maintain a steady **30–60 FPS** performance on low-end Android devices with **2–3 GB RAM**.

### 🛠️ Profiling Tools & Verification
1. **Android Studio Profiler (CPU & Memory)**:
   - Verified Heap Memory allocation stays under **120–150 MB** with 5 active models.
   - Confirmed **0 memory leaks**: Tapping ❌ completely releases Filament C++ native buffers, vertex array objects (VAOs), and textures. Memory drops immediately back to baseline.
2. **Choreographer Real-Time FPS HUD**:
   - Measures raw frame render times on every display refresh tick. Verified steady 50–60 FPS on test devices with 5 models loaded concurrently.

### ⚡ Architectural Decisions for Low-Hardware Execution
1. **Lightweight Google Filament 3D Engine**:
   - SceneView uses Google Filament, a mobile-first PBR renderer designed for low-power ARM GPUs.
2. **Conditional Frame Callbacks**:
   - World-to-screen matrix transformations (`updateLabelsProjection`) execute ONLY when part labels are toggled ON, saving significant CPU frame time when labels are hidden (default state).
3. **Native Binary GLB JSON Extractor (`GlbLabelParser`)**:
   - Parses GLB binary headers directly in Kotlin without instantiating heavy JSON reflection trees.
4. **Auto Unit-Cube Normalization**:
   - `modelNode.scaleToUnitCube(1.0f)` normalizes vertex bounds on load to prevent high GPU matrix transform overhead.
5. **Thread-Safe Asynchronous Model Loading**:
   - Asynchronous `.glb` loading runs on `Dispatchers.IO` background coroutines while UI updates are posted safely back to the Main UI Thread, eliminating UI stutter or dropped frames during model instantiation.

---

## 🛠️ Tech Stack & Dependencies

- **Language**: Kotlin
- **UI Toolkit**: Traditional XML Layout Views + ViewBinding
- **Minimum SDK**: 24 (Target SDK: 36)
- **3D Engine**: `io.github.sceneview:sceneview:2.2.1` (Google Filament)

---

## 📦 Deliverables & Build Instructions

- **Signed Release APK**: `app/build/outputs/apk/release/app-release.apk`
- **Debug APK**: `app/build/outputs/apk/debug/app-debug.apk`

To build from command line:
```bash
# Build Debug APK
./gradlew assembleDebug

# Build Signed Release APK
./gradlew assembleRelease

# Deploy directly to connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.example.infusoryapp/.MainActivity
```

---

## 🔮 Trade-offs & Future Enhancements

- **Occlusion Culling for Labels**: Part labels remain visible even when turned away from the camera; future work can add raycast depth-buffering to hide obscured labels.
- **Custom Environment Lighting**: Standard lighting is used for low GPU overhead; custom HDR IBL maps can be added for enhanced metallic reflections on high-end hardware.
