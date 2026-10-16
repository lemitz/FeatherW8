# R8 rules for the release build.
#
# The default Android rules already keep every class that declares native methods under its original
# name, which is how JNI finds Java_com_featherw8_player_audio_NativeDsdDecoder_*. Say it explicitly so
# the dependency is visible and survives a change of the default rules.
-keep class com.featherw8.player.audio.NativeDsdDecoder {
    native <methods>;
}

# Media3, Compose and kotlinx.coroutines ship their own consumer rules.
