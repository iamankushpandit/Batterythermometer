# Battery Thermometer for Wear OS

<div align="center">

<h1>🌡️</h1>

**Professional Battery Temperature Monitoring for Samsung Galaxy Watch 5**

[![Wear OS](https://img.shields.io/badge/Wear%20OS-4.0+-green.svg)](https://wearos.google.com/)
[![API Level](https://img.shields.io/badge/API-30+-blue.svg)](https://developer.android.com/guide/topics/manifest/uses-sdk-element#ApiLevels)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.8+-purple.svg)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-1.5+-orange.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-Proprietary-red.svg)](#license)

</div>

## 🌡️ Overview

Battery Thermometer is a sophisticated Wear OS application designed specifically for the Samsung Galaxy Watch 5, providing real-time battery temperature monitoring with advanced data visualization and long-term trend analysis.

### ⚠️ Medical Disclaimer

**This app is for informational purposes only and measures BATTERY temperature exclusively. It is NOT a medical device and should NOT be used for health monitoring, medical diagnosis, or measuring body, room, or environmental temperature. Consult healthcare professionals for medical advice.**

## ✨ Features

### 🔴 Live Mode
- **Real-time monitoring**: Updates every second with precise temperature readings
- **Rolling window**: 60-second continuous data visualization
- **Color-coded bars**: Temperature-based color gradients for instant visual feedback
- **Multiple units**: Fahrenheit, Celsius, and Kelvin support with tap-to-cycle functionality

### 📊 Snapshot Mode
- **Long-term analysis**: Data collection every minute for up to 3 days
- **Trend visualization**: Line charts with gradient fills for historical data
- **Battery correlation**: Dual-axis display showing temperature vs battery percentage
- **Persistent storage**: Data survives app restarts and navigation

### 🎨 User Interface
- **Modern Wear OS design**: Built with Jetpack Compose for Wear OS
- **Intuitive navigation**: Double-tap to switch modes, tap to cycle units
- **Battery-aware**: Optimized for watch battery life
- **Accessibility**: Full screen reader support and content descriptions

### 🔧 Technical Features
- **Always-on capability**: Optional screen wake lock for continuous monitoring
- **Background operation**: Continues data collection when app is not visible
- **Memory efficient**: Optimized data structures with automatic cleanup
- **Crash resilient**: Robust error handling and state management

## 📱 Screenshots

| Live Mode | Snapshot Mode | About Screen |
|-----------|---------------|--------------|
| Real-time bars | Historical trends | App information |

## 🛠️ Technical Specifications

### Architecture
- **MVVM Pattern**: Clean architecture with Compose state management
- **Reactive UI**: State-driven updates with Compose runtime
- **Coroutines**: Asynchronous data collection and processing
- **Material Design**: Wear OS Material components and theming

### Dependencies
```kotlin
// Core Wear OS
implementation "androidx.wear.compose:compose-material:1.2.1"
implementation "androidx.wear.compose:compose-foundation:1.2.1"

// Charts and Visualization
implementation "com.github.PhilJay:MPAndroidChart:v3.1.0"

// Compose and UI
implementation "androidx.compose.ui:ui:1.5.4"
implementation "androidx.activity:activity-compose:1.8.1"

// Lifecycle and Navigation
implementation "androidx.lifecycle:lifecycle-runtime-ktx:2.7.0"
implementation "androidx.core:core-splashscreen:1.0.1"
```

### System Requirements
- **Device**: Samsung Galaxy Watch 5 (optimized)
- **OS**: Wear OS 4.0+ (API Level 30+)
- **RAM**: Minimum 1GB recommended
- **Storage**: 10MB application size
- **Sensors**: Battery temperature sensor access

## 🚀 Installation

### From Source
```bash
# Clone the repository
git clone https://github.com/goodtimemicro/battery-thermometer-wearos.git

# Open in Android Studio
cd battery-thermometer-wearos

# Build and install
./gradlew installDebug
```

### ADB Installation
```bash
# Enable Developer Options on watch
# Settings > System > About > Build number (tap 7 times)

# Enable ADB Debugging
# Settings > Developer options > ADB debugging (ON)
# Settings > Developer options > Debug over WiFi (ON)

# Connect via WiFi
adb connect [WATCH_IP]:5555

# Install APK
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 📊 Data Collection

### Live Mode Sampling
- **Frequency**: 1 sample per second
- **Window**: Rolling 60-second buffer
- **Memory**: ~240 data points maximum
- **Storage**: Volatile (resets on app restart)

### Snapshot Mode Sampling
- **Frequency**: 1 sample per minute
- **Capacity**: 4,320 data points (3 days)
- **Memory**: ~52KB estimated usage
- **Storage**: Persistent during app session

### Data Structure
```kotlin
data class SnapshotPoint(
    val secondsSinceStart: Float,    // Timeline position
    val tempCelsius: Float,          // Temperature in Celsius
    val batteryPercent: Float        // Battery level correlation
)
```

## 🎯 Usage Guide

### Basic Operation
1. **Launch app** - Temperature display appears immediately
2. **View live data** - Real-time bars update every second
3. **Switch modes** - Double-tap graph to toggle live/snapshot
4. **Change units** - Tap temperature value to cycle °F → °C → K
5. **Access info** - Tap info icon (ⓘ) in battery bar

### Advanced Features
- **Always-on mode**: Keep screen active for continuous monitoring
- **Background collection**: Data continues when app is minimized
- **Navigation persistence**: Data survives screen changes
- **Battery optimization**: Efficient algorithms minimize power usage

## 🔧 Configuration

### Temperature Units
- **Fahrenheit**: Default unit (32-212°F range)
- **Celsius**: Metric standard (0-100°C range)
- **Kelvin**: Scientific scale (273-373K range)

### Color Coding (Fahrenheit)
- 🔵 **Blue**: < 50°F (Cold)
- 🔷 **Cyan**: 50-70°F (Cool)
- 🟢 **Green**: 70-80°F (Normal)
- 🟡 **Yellow**: 80-90°F (Warm)
- 🟠 **Orange**: 90-100°F (Hot)
- 🔴 **Red**: > 100°F (Critical)

## 🐛 Troubleshooting

### Common Issues

**App not updating**
- Ensure battery data permissions are granted
- Check if device supports temperature sensors
- Restart app if data appears frozen

**Connection problems**
- Verify ADB debugging is enabled
- Check WiFi connection stability
- Try USB connection as alternative

**Performance issues**
- Close unnecessary apps to free memory
- Disable always-on mode if battery drains quickly
- Clear app data if behavior becomes erratic

### Debug Information
```bash
# View app logs
adb logcat | grep "Thermometer"

# Check battery stats
adb shell dumpsys battery

# Monitor memory usage
adb shell dumpsys meminfo com.goodtime.thermometer
```

## 🤝 Contributing

This is a proprietary application developed by GoodTime Micro®. The source code, design, and features are protected by copyright law.

### Reporting Issues
- Use GitHub Issues for bug reports
- Include device model and Wear OS version
- Provide steps to reproduce problems
- Attach relevant logs when possible

## 📄 License

```
Battery Thermometer for Wear OS
Copyright © 2024 GoodTime Micro®. All rights reserved.

This software is proprietary and confidential. The design, user experience, 
features, and source code of this application are protected by copyright law 
and may not be copied, redistributed, reverse-engineered, or modified without 
written permission from GoodTime Micro®.

All trademarks, including GoodTime Micro®, are the property of their 
respective owners.

MEDICAL DISCLAIMER: This application is for informational purposes only and 
measures battery temperature exclusively. It is not a medical device and 
should not be used for health monitoring, medical diagnosis, or measuring 
body, room, or environmental temperature.
```

## 📞 Support

### Contact Information
- **Developer**: GoodTime Micro®

### Professional Services
- Custom Wear OS development
- Enterprise licensing available
- Technical consulting and support
- White-label solutions

---

<div align="center">

**Built with ❤️ for Samsung Galaxy Watch 5**

*GoodTime Micro® - Professional Wear OS Solutions*

</div>