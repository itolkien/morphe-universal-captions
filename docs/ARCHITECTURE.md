# Native YouTube integration boundary

The Java language resolver is unit-testable and independent of Android. It **does not unlock missing translation targets**.

To deliver a working Morphe add-on, we must validate one of these host-specific integration routes:

1. A YouTube caption target is present in the internal auto-translation track list but hidden in the UI. Then selecting that **existing** track is safe.
2. The Android YouTube caption endpoint accepts a target absent from its list. Then inject the verified target through the same authenticated, native caption pipeline while preserving all caption state, and test against an actual APK.
3. Neither holds. In that case native unlock is unsupported for the video; fall back without fabricating a track.

Reference: https://github.com/MorpheApp/morphe-patches/pull/3566

Do **not** reuse arbitrary obfuscated class names across versions, fabricate vssId values, claim that desktop availability proves Android availability, or release an untested MPP. YouTube account tokens and private requests must never be collected.
