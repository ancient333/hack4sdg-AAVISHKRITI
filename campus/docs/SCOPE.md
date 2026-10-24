# Phase 3.1 boundary

No route planning, destination search, traffic-crossing decision, distance estimate, avoidance instruction, Bluetooth streaming, Wi-Fi fallback, or cloud inference is implemented. These are separately approved later work. The system is an environmental description/OCR prototype; object detections can be wrong or incomplete.

A laptop is used only to compile, install, and inspect tests. A connected laptop is not needed by the installed phone application. A laptop webcam implementation was intentionally not added: CameraX on-device testing is the primary input, and optional emulator camera configuration remains a development facility outside the release code.

Future expansion must preserve the `FrameSource`, `Detector`, `TextReader`, `VoiceCommands`, and `SpeechOutput` boundaries. Hardware-specific preview transforms can change without rewriting core policies.

Phase 3.1 adds bounded scene descriptions and one description command only. The accepted manual-restart limitation after interruptions remains unchanged. Phase 3.2 and Phase 3.3 have not begun.
