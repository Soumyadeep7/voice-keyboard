# Voice Keyboard

A keyboard for slow typers. You speak, it types. When you stop, it rewrites unclear speech (stutters, repeats, fillers, misheard words) into clear text.

There are three versions in this repo.

## Android keyboard app (`android/`)

A real phone keyboard that types into any app (WhatsApp, Chrome, Messages).

**Install**

1. On your phone, open the [latest release](../../releases/latest) and download `VoiceKeyboard.apk`.
2. Open the file and allow installing from this source when Android asks.
3. Open the **Voice Keyboard** app and do the three setup steps: allow the microphone, turn the keyboard on in phone settings, switch to it.

**Use**

- Tap in any text box, tap the red **Start listening** key and speak. Your words appear underlined as you talk.
- Tap **Stop listening**. The text is cleaned up and typed in.
- **Fix** repairs the text already in the box, before the cursor.
- **Other keyboard** switches back to your normal keyboard.

Listening uses your phone's own speech recognition (the Google app on most phones). Pick the language you speak in the app's options.

The APK is built by GitHub Actions (`.github/workflows/android.yml`) on every push that touches `android/`. To build it yourself, open the `android` folder in Android Studio.

## Installable web app (`docs/`)

Runs in Chrome or Edge on a phone or PC, and can be installed to the home screen. It types into its own box; you copy the text and paste it where you need it.

Hosted with GitHub Pages from the `docs` folder: `https://soumyadeep7.github.io/voice-keyboard/`

## Claude artifact (`claude-artifact/`)

The original page. It only works when opened as a published artifact on claude.ai.

## Fixing unclear speech

- **Without a key:** a basic cleanup that removes fillers ("um", "uh") and repeated words.
- **With an Anthropic API key:** unclear speech is rewritten properly in any language, and can be translated into the language chosen under "Write in". Add the key in Settings (web app) or in the Voice Keyboard app (Android). The key is stored only on your device and is sent only to `api.anthropic.com`. Never commit a key to this repo.
