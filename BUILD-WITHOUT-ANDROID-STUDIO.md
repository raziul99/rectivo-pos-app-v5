# Building the APK without Android Studio (GitHub Actions)

You get a real installable APK, built in the cloud, no local tools.

## One-time setup
1. Create a **private** GitHub repo (e.g. rectivo-pos-app).
2. Put your real Firebase file at `app/google-services.json`
   (from Firebase console → your Android app → download). It is safe to commit
   this to a PRIVATE repo — it only contains the FCM sender id/app id that ship
   inside every APK anyway. Do NOT commit the server service-account.json.
3. Push the **contents of the RectivoPosApp folder** as the repo ROOT — i.e.
   `settings.gradle` and `build.gradle` must sit at the top level of the repo,
   and `.github/workflows/build.yml` is already included.

   From a machine with git (your PC or the VPS):
   ```
   cd RectivoPosApp
   git init
   git add .
   git commit -m "Rectivo POS app"
   git branch -M main
   git remote add origin https://github.com/<you>/rectivo-pos-app.git
   git push -u origin main
   ```

## Get the APK
- The push triggers the build automatically. Open the repo → **Actions** tab →
  the latest run → wait ~3–5 min → download the **rectivo-pos-debug-apk**
  artifact (a zip containing `app-debug.apk`).
- You can also start a build manually: Actions → Build Rectivo POS APK → Run
  workflow.

## Install on a phone
- Copy `app-debug.apk` to the phone (or open the download link on it).
- Allow "Install unknown apps" for your browser/file manager when prompted.
- Install, open, log in. Done — this debug APK receives FCM push exactly like a
  Play Store build. Fine for your own staff phones.

## Notes
- No keystore needed: a debug APK is auto-signed and installable. You only need
  a release keystore + Play Console if you later want to publish on Google Play.
- If a build fails, open the failed step in the Actions log and send me the red
  error lines — usually a missing `app/google-services.json`.
