#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
export CMAKE_BUILD_PARALLEL_LEVEL="${CMAKE_BUILD_PARALLEL_LEVEL:-1}"
exec ./gradlew \
  :app:testDebugUnitTest \
  :app:assembleDebug \
  --no-daemon \
  --max-workers=1 \
  -Dorg.gradle.jvmargs='-Xmx768m -XX:MaxMetaspaceSize=448m -XX:ActiveProcessorCount=1 -Dfile.encoding=UTF-8' \
  -Dorg.gradle.parallel=false \
  -Pkotlin.compiler.execution.strategy=in-process
