# ADK Assistant

A minimal Android sample that runs an AI **agent** inside the app using Google's
[Agent Development Kit (ADK) for Android](https://developer.android.com/ai/adk).

The agent is a cloud **Gemini** model wired up with two local, on-device tools:

- `getCurrentTime(timeZoneId)` — reads the device clock for any IANA time zone
- `getDeviceInfo()` — returns manufacturer / model / Android version

Ask it *"what time is it in Tokyo?"* or *"what phone is this?"* and the model
decides which tool to call, calls it, and answers from the result.

## How it works

| Piece | File |
|---|---|
| Tools the agent may call (`@Tool` / `@Param`) | [`agent/DeviceTools.kt`](app/src/main/java/com/example/adkassistant/agent/DeviceTools.kt) |
| The agent (`LlmAgent` + `Gemini`) | [`agent/AssistantAgent.kt`](app/src/main/java/com/example/adkassistant/agent/AssistantAgent.kt) |
| Conversation loop (`InMemoryRunner`) | [`ui/main/MainScreenViewModel.kt`](app/src/main/java/com/example/adkassistant/ui/main/MainScreenViewModel.kt) |
| Compose chat UI | [`ui/main/MainScreen.kt`](app/src/main/java/com/example/adkassistant/ui/main/MainScreen.kt) |

The KSP processor reads the `@Tool` annotations at compile time and generates
`generatedTools()`, which the agent uses to expose the functions to the model.

## Running it

1. Get a Gemini API key from [Google AI Studio](https://aistudio.google.com/apikey).
2. Put it in `local.properties` (git-ignored):
   ```properties
   GEMINI_API_KEY=your_key_here
   ```
3. `./gradlew :app:assembleDebug` (or run from Android Studio).

> The key is read into `BuildConfig` for convenience. That ships the key inside
> the APK — fine for a local sample, **not** for production. For a real app,
> route calls through a backend or Firebase AI Logic
> (`com.google.adk:google-adk-kotlin-firebase-android`).

## Requirements

- **minSdk 26** — the ADK AAR declares 26 even though the docs say 24.
- ADK `0.1.0`, KSP `2.3.9`, Kotlin `2.3.20`, AGP `9.0.1`.

## On-device (Gemini Nano)

This sample uses cloud Gemini. ADK also supports running on-device via Gemini
Nano (ML Kit GenAI APIs) by swapping the `Gemini(...)` model for `GenaiPrompt`,
on an AICore-capable device. Same agent, same tools — just a different model.
