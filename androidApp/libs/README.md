# Native LLM runtime

`llama-kotlin-0.2.2.aar` is [ThalzeronLabs/llama-kotlin](https://github.com/ThalzeronLabs/llama-kotlin) v0.2.2
(Apache-2.0), a coroutine Kotlin API over llama.cpp with JSON-schema grammars.

The AAR’s `aar-metadata.properties` was patched from `minCompileSdk=37` to `35`
so this project can stay on AGP 8.9 / compileSdk 35. Native ABIs: `arm64-v8a`, `x86_64`.

To refresh from upstream:

```bash
curl -L -o /tmp/llama-kotlin.aar \
  https://github.com/ThalzeronLabs/llama-kotlin/releases/download/v0.2.2/llama-kotlin-0.2.2.aar
rm -rf /tmp/llama-patch && mkdir /tmp/llama-patch && cd /tmp/llama-patch
unzip -q /tmp/llama-kotlin.aar
sed -i 's/minCompileSdk=37/minCompileSdk=35/' META-INF/com/android/build/gradle/aar-metadata.properties
zip -qr "$OLDPWD/llama-kotlin-0.2.2.aar" .
```
