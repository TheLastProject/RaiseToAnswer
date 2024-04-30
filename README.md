# Raise To Answer (Android)

![Raise To Answer logo](app/src/main/res/mipmap-hdpi/ic_launcher.png)

*Logo by [Rose (TangentFoxy)](http://github.com/TangentFoxy)*

Simply hold your phone to your ear to answer an incoming call. When the app detects the phone is near your ear during an incoming call, it will beep 5 times and then answer the call.

No ads, no unnecessary permissions and no unnecessary battery drain. Easy to enable and disable. Doesn't replace your incoming call screen, so you don't need to learn anything new.

[<img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png"
    alt="Get it on IzzyOnDroid"
    height="80">](https://apt.izzysoft.de/fdroid/index/apk/me.hackerchick.raisetoanswer)
[<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png"
     alt="Get it on Google Play"
     height="80">](https://play.google.com/store/apps/details?id=me.hackerchick.raisetoanswer)

![Screenshot](fastlane/metadata/android/en-US/images/phoneScreenshots/1_en-US.png)

## How to build

Building can either be done through Android Studio (not reproducible!) or the build.sh script in this repository (reproducible with OpenJDK 17). This script can also sign the build.

Build without signing:
```
./build.sh
```

Build with signing:
```
KEYSTORE=/path/to/keystore KEYSTORE_ALIAS=raisetoanswer ./build.sh
```

### Translations

<a href="https://hosted.weblate.org/engage/raisetoanswer/">
<img src="https://hosted.weblate.org/widgets/raisetoanswer/-/open-graph.png" alt="Translation status" />
</a>

## License

MIT
