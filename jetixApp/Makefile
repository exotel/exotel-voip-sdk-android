JETIX=../jetix generate-sdk
PWD=$(shell pwd)
EXOTEL_SDK_PATH := $(PWD)/exotel-voice-sdk
get_latest_version_cmd := curl --silent https://github.com/exotel/exotel-voip-sdk-android/releases | grep -o '\/tag\/.*' | awk -F'[<>]' '{print $2}' | head -n 1
EXOTEL_SDK_VERSION := $(shell $(get_latest_version_cmd))
GIT_URL := https://github.com/exotel/exotel-voip-sdk-android/releases/download/$(EXOTEL_SDK_VERSION)/exotel-voice-$(EXOTEL_SDK_VERSION)-release.aar
EXOTEL_SDK_FILE := exotel-voice-$(EXOTEL_SDK_VERSION)-release.aar

deps:
	rm -rf $(EXOTEL_SDK_PATH)/exotel-voice-release.aar
	curl -L $(GIT_URL) -o $(EXOTEL_SDK_PATH)/exotel-voice-release.aar

release-apk:
	echo "Generating release apk file..."
	$(MAKE) -C $(JETIX)
	./gradlew clean
	./gradlew assembleRelease
	echo "Output file generated in this location jetixApp/app/build/outputs/apk/release"
	
debug-apk:
	echo "Generating debug apk file..."
	$(MAKE) -C $(JETIX)
	./gradlew clean
	./gradlew assembleDebug
	echo "Output file generated in this location jetixApp/app/build/outputs/apk/debug"

apk: release-apk

clean:
	echo "Cleaning.."
	./gradlew clean
	rm -rf $(EXOTEL_SDK_PATH)/exotel-voice-release.aar
	rm -rf $(EXOTEL_SDK_PATH)/build
