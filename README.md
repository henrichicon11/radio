# World Radio (Android)

Listen to radio stations from every country in the world, sorted by country.

## Features
- **Countries tab**: every country with its flag and station count, plus a country search
- **Country screen**: all stations in that country, most popular first, loaded as you scroll, with a name filter
- **Search tab**: search stations by name across the whole world
- **Favorites**: tap ☆ to save a station (stored on the device)
- **Background playback**: media notification, lock-screen and headset controls, pauses when headphones are unplugged
- **Now-playing bar**: station logo, live song title (when the stream sends it), play/pause/stop, favorite
- Plays MP3, AAC, OGG and HLS (m3u8) streams, over both HTTP and HTTPS

Station data comes from the free community [Radio Browser](https://www.radio-browser.info) directory (over 58,000 stations in 240+ countries). The app switches mirrors automatically if one server is down.

## Build
Requires JDK 17+ and the Android SDK (platform 34, build-tools 34).
```
echo "sdk.dir=/path/to/android-sdk" > local.properties
keytool -genkeypair -keystore worldradio.keystore -alias worldradio -keyalg RSA -validity 10000 -storepass worldradio -keypass worldradio -dname "CN=World Radio"
gradle assembleRelease
```
Output: `app/build/outputs/apk/release/app-release.apk` (minSdk 21 / Android 5.0+).
