# Changelog

All notable changes to this project will be documented in this file.
## [1.0.69] 08-08-2025
### Added
* [VST-1068](https://exotel.atlassian.net/browse/VST-1068): upgrade the cloudonix sdk from .34 to .35 version because of Boot-Start broadcast listener crash.

## [1.0.68] 01-08-2025
### Added
* [VST-1041](https://exotel.atlassian.net/browse/VST-1041): Added the hangup event matrix and syncing with the backend for SDK stability

## [1.0.67] 04-07-2025
### Added
* [VST-1010](https://exotel.atlassian.net/browse/VST-1010): Added the latency matrix syncing with the backend for SDK stability

## [1.0.66] 19-03-2025
### Added
* [VST-963](https://exotel.atlassian.net/browse/VST-963): adding opus codec support based on tenant settings from profile api and MAX_RATE set to 8000

## [1.0.65] 11-02-2025
### Added
* [VST-942](https://exotel.atlassian.net/browse/VST-942): adding check for LogLevel NONE

## [1.0.64] 07-02-2025
### Added
* [VST-942](https://exotel.atlassian.net/browse/VST-942): adding check for LogLevel NONE

## [1.0.63] 25-11-2024
### Added
* [VST-859](https://exotel.atlassian.net/browse/VST-882): Updating the Foreground Service for Microphone Permission at the Runtime According to Android 14 Guidelines


## [1.0.62] 16-10-2024
### Added
* [VST-859](https://exotel.atlassian.net/browse/VST-859): In the stop function, the onDestroyMediaSession is handled, and stop can only be initiated in the Idle state.

## [1.0.61] 08-10-2024
### Added
* [VST-847](https://exotel.atlassian.net/browse/VST-847) : Fix the Foreground service crash for Android 14

## [1.0.60] 30-09-2024
* [VST-848] Volume level receiver crash fix

## [1.0.57] 09-09-2024

### Added
* [VST-837](https://exotel.atlassian.net/browse/VST-837) : updated version of cloudonix from 5.2.53.27 to 5.2.53.28

## [1.0.56] 12-08-2024

### Added
* [VST-805](https://exotel.atlassian.net/browse/VST-805) : fixed network switch crash issue

## [1.0.55] 01-08-2024

### Added
* [VST-809](https://exotel.atlassian.net/browse/VST-809) : Added listener on onDestroyMediaSession()

## [1.0.54] 30-07-2024

### Added
* [VST-804](https://exotel.atlassian.net/browse/VST-804) : configured send metrics

## [1.0.53] 17-07-2024

### Added
* [VST-787]: Added try-catch to fix crash during speaker switch in adjust audio route in jniSIPSdkClient.cpp- wingbank

## [1.0.52] 17-07-2024

### Added
* [VST-787] Added try-catch to fix crash during speaker switch in adjust audio route - wingbank

## [1.0.51] 16-07-2024

### Changed
* [VST-786] fixed race condition between set codec and invite during SDK initialization

## [1.0.50] 10-07-2024

### Changed
* updated SDK version to 1.0.50

## [1.0.49] 19-06-2024

### Changed
* exotel sdk upgraded to version 1.0.49. refer [changelog](https://bitbucket.org/Exotel/exotel_voice_android/src/master/jetix/Changelog.md)
  * fix call initiated flow and hangup code

## [1.0.48] 13-06-2024

### Changed
* exotel sdk upgraded to version 1.0.48. refer [changelog](https://bitbucket.org/Exotel/exotel_voice_android/src/master/jetix/Changelog.md)

## [1.0.47] 04-06-2024

### Changed
* use new APIs introduced in latest version of sdk
  * stop()
  * onDeinitialized()
* exotel sdk upgraded to version 1.0.47. refer [changelog](https://bitbucket.org/Exotel/exotel_voice_android/src/master/jetix/Changelog.md)
* exotel sdk upgraded to version 1.0.44. refer [changelog](https://bitbucket.org/Exotel/exotel_voice_android/src/master/jetix/Changelog.md)
* exotel sdk upgraded to version 1.0.43. refer [changelog](https://bitbucket.org/Exotel/exotel_voice_android/src/master/jetix/Changelog.md)
* exotel sdk upgraded to version 1.0.42. refer [changelog](https://bitbucket.org/Exotel/exotel_voice_android/src/master/jetix/Changelog.md)

### Added
* [AP2AP-243](https://exotel.atlassian.net/browse/AP2AP-243) : android 13 onward, declared foreground service types and related changes
* [AP2AP-235](https://exotel.atlassian.net/browse/AP2AP-235) : bluetooth switching feature added

## [1.0.40] 28-08-2023

### Added
* [AP2AP-196](https://exotel.atlassian.net/browse/AP2AP-196) : added search contact support 

## [1.0.39] 25-08-2023

### Added
* [AP2AP-187](https://exotel.atlassian.net/browse/AP2AP-187) : added whatsapp calling support for exotel group

### Changed
* [AP2AP-191](https://exotel.atlassian.net/browse/AP2AP-191) : removed call activity from foreground when there is no active call.
* [AP2AP-192](https://exotel.atlassian.net/browse/AP2AP-192) : VoiceAppLogger code refactor to make output stream writer reusable
* exotel sdk version upgrade from 1.0.38 to 1.0.39

## [1.0.38] 11-08-2023

### Added
* [AP2AP-184](https://exotel.atlassian.net/browse/AP2AP-184) : added null pointer check while appending log

### Changed
* [AP2AP-180](https://exotel.atlassian.net/browse/AP2AP-180) : VoiceAppLogger code refactor to avoid unnecessary Date object creation.
* [AP2AP-182](https://exotel.atlassian.net/browse/AP2AP-182) : binding service onResume of activity if not bound



## [1.0.37] 08-04-2023

### Added
* [AP2AP-175](https://exotel.atlassian.net/browse/AP2AP-175) : hide multi call
* [AP2AP-179](https://exotel.atlassian.net/browse/AP2AP-179) : VoiceAppLogger code refactor to avoid unnecessary object creation.
