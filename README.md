# NFC Tag Emulator

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Android application for emulating NFC tags. Scan, save, and emulate NFC tags with custom content.

## Features

- 📱 **Scan NFC tags** - Read physical NFC tags
- 💾 **Save tags** - Store tags with custom names
- ✨ **Create tags** - Generate virtual tags (URL, Text, Phone, Email, Contact)
- 🔄 **Emulate tags** - Act as an NFC tag for other devices
- 🎨 **Modern UI** - Neon-themed Material 3 design

## How it works

1. **Phone A** scans a physical NFC tag or creates a virtual one
2. **Phone A** selects a tag to emulate
3. **Phone B** (with NFC enabled) taps Phone A
4. **Phone B** receives the tag data

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose
- **NFC**: Android NFC/HCE APIs
- **Architecture**: MVVM (Model-View-ViewModel)
- **Storage**: SharedPreferences

## Requirements

- Android 7.0 (API 24) or higher
- Device with NFC hardware

## Security Notes
⚠️ Important: This app requires NFC permissions. All tag data is stored locally on the device.

## Contributing
Contributions are welcome! Please feel free to submit a Pull Request.

## License
This project is licensed under the MIT License - see the LICENSE file for details.

## Author
Vlad-Akimov