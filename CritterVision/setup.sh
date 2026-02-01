#!/bin/bash

# Exit immediately if a command exits with a non-zero status.
set -e

# --- Environment Variables ---
export ANDROID_HOME=$HOME/android-sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

# --- Android SDK Installation ---
echo "Installing Android SDK..."
mkdir -p $ANDROID_HOME/cmdline-tools
cd $ANDROID_HOME/cmdline-tools
wget https://dl.google.com/android/repository/commandlinetools-linux-10406996_latest.zip -O cmdline-tools.zip
unzip -q cmdline-tools.zip
mv cmdline-tools latest
rm cmdline-tools.zip
cd -

# --- Accept SDK Licenses ---
echo "Accepting Android SDK licenses..."
yes | sdkmanager --licenses > /dev/null

# --- Install Required SDK Packages ---
echo "Installing Android SDK platforms and build-tools..."
# Add the specific platform and build-tools versions your project requires.
# For example: "platforms;android-34" and "build-tools;34.0.0"
sdkmanager "platforms;android-34" "build-tools;34.0.0" "platform-tools"

# --- Verify Installation ---
echo "Verifying Android SDK installation..."
sdkmanager --list_installed

echo "Android development environment setup complete."

# --- (Optional) Grant Execute Permissions to Gradle Wrapper ---
if [ -f "./gradlew" ]; then
    echo "Granting execute permissions to Gradle wrapper."
    chmod +x ./gradlew
fi

echo "Jules is now ready to work on your Android project."