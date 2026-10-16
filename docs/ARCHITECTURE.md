> Historical stable2.0.0 architecture. Stable **2.1.0** adds current-audio source fallback and reads the native target catalog; see [release evidence](RELEASE_2.1.0.md) and [fallback architecture](EMPTY_TRANSLATION_EXPERIMENT.md).

# Menu-only architecture

The independent patch discovers the native caption track and translated-track list using semantic string anchors, then resolves the builder through copy-constructor data flow. Resolution completes before modifying host bytecode. Both direct-field and abstract-getter layouts are supported, with ambiguous identities rejected.

Generated accessors live in the owning native class so private field access remains valid. Every helper uses the `universalCaptions_` prefix to avoid colliding with Morphe’s `patch_` helpers.

Only translated-list returns are intercepted. `CaptionTranslationList` retains native entries, clones a valid native translated prototype for additional targets, preserves its source and signed URL parameters, and sorts ordinary language entries using a locale collator. Special entries retain their slots. The runtime never reads or writes caption preferences, source-selection decisions or visibility policies.

The bridge reads Morphe’s app locale when the Requester API is available and otherwise uses the system locale. No app context or persistent state is required. Runtime augmentation failures return the original list.

The original v1.4 implementation and its selection-policy documentation remain available in the v1.4.0 Git tag.

## Native Morphe coexistence

The integration from Morphe PR #3754 now has its own menu setting and an injected `AutoTranslateCaptionsPatch$CaptionTrackInterface`. The addon resolves that interface without initializing Morphe settings and checks actual returned tracks. If a track implements the interface, the addon returns the exact input list. This leaves both native setting states and either list-hook order under Morphe control. Checking only for an extension class would be insufficient: shared extension classes may exist even when Captions was not selected.

Independent host helpers retain their distinct namespace. The addon keeps structural ABI guards and exact URL-parameter replacement because it runs independently of Morphe’s fingerprints and declared version range. It does not duplicate the native settings UI or broaden supported APK versions based solely on upstream fixes. Source: https://github.com/MorpheApp/morphe-patches/pull/3754/commits/078692dfb82dcfce55a529b43c5c4e7c585507cd
