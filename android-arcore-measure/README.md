# ARCore Android Tape Measure (Real-Time)

This is an Android starter app to replicate tape-like measurement using ARCore.

## What this app does

- Opens camera in AR view
- Shows a blue center reticle
- `Start` locks first 3D point at reticle
- Moving phone updates second point in real time
- `Stop` finalizes measurement
- Shows distance in `cm`, `m`, `in`

This is the right foundation for your idea: measuring room edges, sofa length, tyre diameter approximation, etc.

## Project Location

`C:\Users\lenovo\OneDrive\Documents\Playground\android-arcore-measure`

## Step-by-Step Run (Android Studio)

1. Open Android Studio.
2. Click `Open` and choose:
   `C:\Users\lenovo\OneDrive\Documents\Playground\android-arcore-measure`
3. Let Gradle sync complete.
4. Connect an ARCore-supported Android phone.
5. Enable Developer options + USB debugging on phone.
6. Run app (`Shift + F10`).
7. In app:
   - Move camera so ARCore detects planes.
   - Aim blue reticle at start point.
   - Tap `Start`.
   - Move to end point.
   - Tap `Stop`.
   - Read distance.

## Important Notes

- This app requires ARCore-supported device.
- Accuracy depends on lighting, surface texture, and tracking quality.
- For curved objects (tyre), measure diameter by selecting opposite edge points.
- For best room/object measurement, move slowly and keep object in view.

## Main Files

- `app/src/main/java/com/lenovo/artapemeasure/MainActivity.kt`
- `app/src/main/res/layout/activity_main.xml`
- `app/src/main/AndroidManifest.xml`
- `app/build.gradle.kts`

## Next Upgrades (I can build next)

1. Continuous polyline path while button held (true drag line).
2. Multi-segment measurements and area/perimeter.
3. Save screenshot + measurement history.
4. Voice output for accessibility.
5. Add YOLO object tagging on top of AR measurement.
