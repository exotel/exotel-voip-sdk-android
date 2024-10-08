# Changelog

All notable changes to this project will be documented in this file.

## [1.0.58] 08-10-2024

### Added
* [VST-847](https://exotel.atlassian.net/browse/VST-847) : updated version of cloudonix from 5.2.53.29 to 5.2.53.30



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
