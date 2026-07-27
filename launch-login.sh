#!/bin/bash
set -e

# launch-login.sh — Compile and run BuildPay Login with optimized settings
export OPENJFX_LIB="/Users/mac/Downloads/javafx-sdk-26.0.1/lib"
cd "$(dirname "$0")"

echo "📦 Compiling login module..."
mkdir -p out

javac --module-path "$OPENJFX_LIB" --add-modules javafx.fxml,javafx.controls \
  -cp "src:libs/sqlite-jdbc-3.41.2.1.jar" -d out \
  src/models/User.java \
  src/repositories/UserRepository.java \
  src/services/AuthenticationService.java \
  src/ui/login/LoginLauncher.java \
  src/com/payroll/controllers/LoginController.java

echo "🚀 Launching BuildPay Login UI..."
java --enable-native-access=javafx.graphics \
  --module-path "$OPENJFX_LIB" \
  --add-modules javafx.controls,javafx.fxml \
  -cp "out:libs/sqlite-jdbc-3.41.2.1.jar:src" \
  ui.login.LoginLauncher 2>/dev/null
