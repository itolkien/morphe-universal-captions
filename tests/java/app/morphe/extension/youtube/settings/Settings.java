// Test-only platform stub.
package app.morphe.extension.youtube.settings;
public final class Settings {
 public static final Setting<String> PREFERRED_CAPTION_LANGUAGE = new Setting<>("off");
 public static final Setting<Boolean> AUTO_TRANSLATE_ADDITIONAL_LANGUAGES = new Setting<>(false);
 public static final class Setting<T> { public T value; public Setting(T value) { this.value=value; } public T get() { return value; } }
}
