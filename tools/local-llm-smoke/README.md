# Local LLM host smoke test

Validates Sticky Language’s on-device prompts + JSON schemas against a real GGUF
**on this computer**, without an Android emulator.

## Why not the app AAR / emulator?

`llama-kotlin` ships Android-only native libraries (`arm64-v8a` / Android `x86_64`).
Those `.so` files do not load in a desktop JVM. An emulator would only re-test the
JNI wrapper; the model weights, chat template, and JSON-schema constraints are the
risky parts, and llama.cpp on the host covers them.

You *do* have a physical phone on `adb` if you later want an instrumented Android test.

## Run

```bash
# Downloads Qwen2.5-1.5B (~1.1 GB) on first run, builds llama-completion if needed
./tools/local-llm-smoke/run.sh

# Or the smaller / weaker 0.5B preset:
MODEL_ID=qwen25-05b ./tools/local-llm-smoke/run.sh
```

Equivalent Gradle invocation:

```bash
LOCAL_LLM_SMOKE=1 \
LOCAL_LLM_CLI=$PWD/tools/local-llm-smoke/llama.cpp/build/bin/llama-completion \
LOCAL_LLM_MODEL=$PWD/tools/local-llm-smoke/models/qwen2.5-1.5b-instruct-q4_k_m.gguf \
./gradlew :shared:test --tests '*.LocalLlmHostSmokeTest'
```

Without `LOCAL_LLM_SMOKE=1`, the tests are skipped so normal CI stays fast.

## What we learned

| Model | translate schema | breakdown schema |
|---|---|---|
| Qwen2.5 0.5B Q4_K_M | Often fails (repetition / truncated mid-JSON) | Usually OK |
| Qwen2.5 1.5B Q4_K_M | OK | OK |

Prefer **1.5B+** on device. 0.5B is too weak for the full `TranslationResult` schema.
