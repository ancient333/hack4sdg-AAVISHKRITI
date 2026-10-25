# hack4sdg-AAVISHKRITI

# CampusLens – An Integrated Accessibility System

**An assistive Android application for blind, deaf, and non-speaking individuals.**

<p align="center">
  <b>One App. Three Accessibility Modes. Greater Independence.</b>
</p>

## About the Project

CampusLens is an Android-based accessibility application designed to help people with visual, hearing, and speech impairments navigate their surroundings and communicate more independently.

The application brings multiple assistive features together in one platform, including camera-based object detection, distance estimation, audio guidance, speech-to-text, sign-language assistance, and text-to-speech communication.

CampusLens provides three dedicated modes through a simple menu. Blind Mode is enabled by default, while Deaf Mode and Non-Speaking Mode can be selected whenever required.

## Problem Statement

People with visual, hearing, and speech impairments face daily challenges in environmental awareness, safe navigation, and communication. Many existing assistive solutions address these needs separately, requiring users to depend on multiple applications or devices.

CampusLens aims to address this gap by developing a unified Android application that combines intelligent object detection, distance estimation, audio-based guidance, speech-to-text, sign-language recognition, and text-to-speech capabilities. By bringing these functions into one accessible interface, the system seeks to support more independent navigation and communication.

## Objectives

- Develop one application supporting three accessibility modes.
- Help blind users identify objects and estimate their distance.
- Provide spoken environmental information and obstacle alerts.
- Convert speech into readable text for deaf users.
- Explore camera-based sign-language recognition and translation.
- Enable non-speaking users to communicate through text-to-speech.
- Support integration with compatible smart glasses and audio devices.
- Provide offline functionality wherever supported by the selected models and tools.

## Key Features

### 1. Blind Mode

Blind Mode is the default mode when CampusLens opens.

- Camera-based object detection.
- Object distance estimation.
- Spoken object descriptions and alerts.
- Voice-command interaction.
- Environmental awareness assistance.
- Planned integration with compatible smart glasses.

### 2. Deaf Mode

Deaf Mode is accessible through the top-right menu in the application.

- Speech-to-text communication.
- Camera-based hand tracking.
- Sign-language recognition integration.
- Text-based communication assistance.
- Potential spoken output for recognized signs.

**Development note:** Hand tracking alone does not translate sign language. Reliable translation requires a compatible trained recognition model, and speech recognition requires the appropriate device or service configuration.

### 3. Non-Speaking Mode

- Text input for everyday communication.
- Text-to-speech output.
- Quick-access phrases for common situations.
- On-screen messages for face-to-face conversations.

### 4. Unified Accessibility Interface

- Blind Mode enabled by default.
- Menu-based switching between Blind, Deaf, and Non-Speaking modes.
- Large, clearly labelled controls.
- Accessibility-focused navigation.
- Automatic reconnection to previously paired hardware where Android and the hardware support it.

## Technology Stack

The exact stack depends on the implementation of each module.

| Component | Technology |
|---|---|
| Mobile application | Android |
| Android development | Java / Kotlin, as used by each module |
| Object detection | Compatible computer vision model |
| Hand tracking | MediaPipe |
| Distance estimation | Camera-based estimation techniques |
| Speech output | Android Text-to-Speech |
| Speech recognition | Android speech recognition or a compatible model |
| Deaf Mode interface | Android WebView and web technologies, where applicable |
| Sign-language translation | Trained machine-learning model |
| Version control | Git and GitHub |
| Development environment | Android Studio |

## System Workflow

1. The user opens CampusLens.
2. Blind Mode starts by default.
3. The app attempts to connect to compatible, previously paired smart glasses, if configured.
4. The user opens the menu to switch to Deaf Mode or Non-Speaking Mode.
5. The selected mode opens its corresponding interface.
6. The app provides the available audio, visual, or text-based assistance.
7. Features that require an external service or server operate when their dependencies are configured and available.

## System Requirements

- Windows, macOS, or Linux development computer.
- Android Studio.
- A JDK compatible with the project's Gradle version.
- Android SDK and platform tools.
- Git.
- Android phone or compatible emulator.
- Node.js if the Deaf Mode web interface requires an asset build.
- Appropriate camera, microphone, Bluetooth, and location permissions for the features being used.

## Installation and Setup

### Clone the Repository

```bash
git clone https://github.com/ancient333/hack4sdg-AAVISHKRITI.git
```

Open the cloned project in Android Studio and allow Gradle synchronization to finish.

### Build and Run

1. Install the required Android SDK components.
2. Configure a compatible Gradle JDK.
3. Install any dependencies required by the individual modules.
4. Generate required web assets if the Deaf Mode module uses a separate web build.
5. Connect an Android device or start an emulator.
6. Build and run the app through Android Studio.

Follow the instructions in the relevant module directories if additional model files, web assets, or services are required.

## Smart Glasses Integration

CampusLens is designed to begin with an Android phone camera and can be extended to compatible smart glasses.

Potential integration capabilities include:

- Camera input from a compatible device.
- Bluetooth connectivity where supported.
- Spoken feedback through a compatible audio output.
- Automatic reconnection to previously paired hardware where supported.
- Phone-based AI inference to reduce hardware requirements.

Actual functionality depends on the glasses hardware, available APIs, connection protocol, and Android permissions.

## Privacy and Offline Support

CampusLens aims to process data locally wherever feasible.

Offline availability depends on the feature and its implementation. Local object detection, hand tracking, or speech processing may work offline when compatible models are bundled. Cloud-based recognition, remote communication, and external APIs may require internet access.

Camera, microphone, Bluetooth, and location permissions should be requested only when needed. Sensitive user data should be handled securely.

## Current Development Status

CampusLens is an evolving assistive technology project.

The application is intended to provide a unified interface for all three accessibility modes. Feature completeness, model availability, and hardware compatibility must be validated through testing.

Some capabilities, particularly reliable sign-language translation, smart-glasses integration, and server-based messaging, require additional implementation and verification before they can be considered production-ready.

## Future Enhancements

- Improve object detection and distance estimation.
- Expand supported sign languages and vocabularies.
- Improve speech recognition in noisy environments.
- Add robust obstacle-awareness and navigation assistance.
- Improve Bluetooth reconnection and hardware compatibility.
- Expand offline AI capabilities.
- Conduct usability testing with people with disabilities.
- Improve privacy, performance, and battery efficiency.

## Repository

**GitHub:** [ancient333/hack4sdg-AAVISHKRITI](https://github.com/ancient333/hack4sdg-AAVISHKRITI)

## Disclaimer

CampusLens is an assistive technology project under development. It is not a replacement for a white cane, guide dog, hearing assistance, professional support, or established safety systems. Distance estimates and environmental alerts may be inaccurate; users should exercise caution and independently verify safety-critical information.
