# Mozc notices

This app bundles Mozc source-derived native code and dictionary data from Google Mozc commit `d918f30063999d4ca250f76ced8b92169a0e0b25`.

The complete upstream [Mozc LICENSE](THIRD_PARTY_NOTICES/MOZC-LICENSE) and [official credits](THIRD_PARTY_NOTICES/MOZC-CREDITS.html) are bundled with this project. The credits include the dictionary-data notices for IPAdic, Japanese Usage Dictionary, Okinawa dictionary, Tamachi Phonetic Kanji Alphabet, and Japan Post zipcode data.

The native library statically includes [Abseil C++](THIRD_PARTY_NOTICES/ABSEIL-LICENSE), [Protocol Buffers](THIRD_PARTY_NOTICES/PROTOBUF-LICENSE), and [zlib](THIRD_PARTY_NOTICES/ZLIB-LICENSE). `mozc-proto-lite.jar` uses Protocol Buffers generated code and the app's `protobuf-javalite` runtime. Copies of all notices are also packaged in the APK at `assets/licenses/`.
