#!/usr/bin/env bash
# Host smoke test for Sticky Language on-device GGUFs.
# No Android emulator needed: uses llama.cpp on this computer with the same
# prompts + JSON schemas the app uses.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
SMOKE="$(cd "$(dirname "$0")" && pwd)"
CLI="${LOCAL_LLM_CLI:-$SMOKE/llama.cpp/build/bin/llama-completion}"
# Default to the app's recommended model (1.5B). Override with LOCAL_LLM_MODEL or MODEL_ID=qwen25-05b.
MODEL_ID="${MODEL_ID:-qwen25-15b}"
case "$MODEL_ID" in
  qwen25-05b)
    DEFAULT_MODEL="$SMOKE/models/qwen2.5-0.5b-instruct-q4_k_m.gguf"
    MODEL_URL="https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf"
    MODEL_BYTES=491400032
    ;;
  qwen25-15b|*)
    DEFAULT_MODEL="$SMOKE/models/qwen2.5-1.5b-instruct-q4_k_m.gguf"
    MODEL_URL="https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf"
    MODEL_BYTES=1117320736
    ;;
esac
MODEL="${LOCAL_LLM_MODEL:-$DEFAULT_MODEL}"

echo "== Sticky Language local LLM host smoke =="
echo "No emulator needed — Android AAR natives cannot load in a desktop JVM."
echo "This runs llama.cpp on the host with the app's prompts + JSON schemas."
echo "root:  $ROOT"
echo "cli:   $CLI"
echo "model: $MODEL"

if [[ ! -x "$CLI" ]]; then
  echo "Building llama-completion…"
  if [[ ! -d "$SMOKE/llama.cpp/.git" ]]; then
    git clone --depth 1 https://github.com/ggml-org/llama.cpp.git "$SMOKE/llama.cpp"
  fi
  cmake -S "$SMOKE/llama.cpp" -B "$SMOKE/llama.cpp/build" \
    -DGGML_NATIVE=ON -DLLAMA_BUILD_TESTS=OFF -DLLAMA_CURL=OFF
  cmake --build "$SMOKE/llama.cpp/build" -j"$(nproc)" --target llama-completion
  CLI="$SMOKE/llama.cpp/build/bin/llama-completion"
fi

mkdir -p "$(dirname "$MODEL")"
if [[ ! -f "$MODEL" ]] || [[ "$(stat -c%s "$MODEL" 2>/dev/null || echo 0)" -ne "$MODEL_BYTES" ]]; then
  echo "Downloading model ($(numfmt --to=iec "$MODEL_BYTES" 2>/dev/null || echo "$MODEL_BYTES bytes"))…"
  curl -L --continue-at - -o "$MODEL" "$MODEL_URL"
fi

export LOCAL_LLM_SMOKE=1
export LOCAL_LLM_CLI="$CLI"
export LOCAL_LLM_MODEL="$MODEL"
export LOCAL_LLM_LIB="$(dirname "$CLI")"
export LD_LIBRARY_PATH="${LOCAL_LLM_LIB}:${LD_LIBRARY_PATH:-}"

cd "$ROOT"
./gradlew :shared:test --tests 'com.stickylanguage.shared.llm.LocalLlmHostSmokeTest' --info | tee "$SMOKE/last-run.log" | grep -E 'translate OK|breakdown OK|FAILED|PASSED|LocalLlmHostSmokeTest|AssertionError|BUILD|Snippet'
