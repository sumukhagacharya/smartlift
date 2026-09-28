# Smart Dual Elevator System - Java Backend

## Overview

This is the complete Java backend for the Smart Dual Elevator System Android app. The system intelligently manages two elevators to prevent "ghost trips" - where both lifts respond to the same call unnecessarily.

## File Structure

```
java/com/elevator/smart/
├── MainActivity.java              # Main Activity - connects UI to backend
├── controller/
│   └── ElevatorController.java    # Smart scheduling logic
└── model/
    ├── Direction.java             # UP, DOWN, NONE enum
    ├── ElevatorState.java         # IDLE, MOVING, SERVING enum
    ├── Elevator.java              # Elevator model with state & queue
    └── FloorCall.java             # Hall call request model
```

## Key Features

### 1. Ghost Trip Prevention
The `ElevatorController.selectBestLift()` method ensures only ONE lift responds to each call:

```java
// Selection priority:
// 1. If one IDLE, one busy → pick IDLE
// 2. If both IDLE → pick closest
// 3. If both MOVING → pick one going towards call in correct direction
// 4. If neither suitable → queue the call
```

### 2. Smart Scheduling Algorithm

```java
private String selectBestLift(int floor, Direction direction) {
    // Check availability and distance
    boolean aAvailable = liftA.isAvailable();
    boolean bAvailable = liftB.isAvailable();
    int distanceA = liftA.distanceTo(floor);
    int distanceB = liftB.distanceTo(floor);
    
    // Apply selection rules...
}
```

### 3. Real-Time Floor-by-Floor Movement

```java
// Each floor takes 1 second to traverse
public static final long FLOOR_TRAVEL_TIME_MS = 1000;

// Doors stay open for 2 seconds
public static final long DOOR_OPEN_TIME_MS = 2000;
```

### 4. Two Types of Calls

**Hall Calls (Outside):**
```java
// User presses up/down button on a floor
controller.handleSpecificLiftCall(floor, direction, "A"); // For Lift A
controller.handleSpecificLiftCall(floor, direction, "B"); // For Lift B
```

**Car Calls (Inside):**
```java
// User inside lift selects destination floor
controller.handleCarCall("A", destinationFloor);
```

## Integration Guide

### Step 1: Add Files to Android Project

Copy the `java/` folder contents to your Android project's `app/src/main/java/` directory.

### Step 2: Update Package Name

If your package name differs from `com.elevator.smart`, update:
- All `package` declarations
- All `import` statements
- `MainActivity` in `AndroidManifest.xml`

### Step 3: Add to AndroidManifest.xml

```xml
<activity
    android:name=".MainActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

### Step 4: Add Material Design Dependency

In `build.gradle` (app level):
```gradle
dependencies {
    implementation 'com.google.android.material:material:1.9.0'
}
```

## API Reference

### ElevatorController

| Method | Description |
|--------|-------------|
| `handleSpecificLiftCall(floor, direction, liftId)` | Call a specific lift to a floor |
| `handleCarCall(liftId, floor)` | Select destination from inside lift |
| `getCurrentFloor(liftId)` | Get current floor of a lift |
| `getState(liftId)` | Get current state (IDLE/MOVING/SERVING) |
| `getDestinationQueue(liftId)` | Get list of pending destinations |
| `reset()` | Reset both lifts to initial positions |

### ElevatorEventListener Callbacks

| Callback | Description |
|----------|-------------|
| `onFloorChanged(liftId, floor)` | Lift arrived at new floor |
| `onStateChanged(liftId, state)` | Lift state changed |
| `onDirectionChanged(liftId, direction)` | Lift direction changed |
| `onDestinationQueueChanged(liftId, queue)` | Destination queue updated |
| `onCallAssigned(call, liftId)` | Hall call assigned to lift |
| `onCallServed(call, liftId)` | Hall call served |
| `onDoorsOpening(liftId, floor)` | Doors opening |
| `onDoorsClosing(liftId, floor)` | Doors closing |
| `onLog(message)` | Debug log message |

## Elevator States

| State | Description |
|-------|-------------|
| `IDLE` | Stationary, no pending requests |
| `MOVING` | Traveling between floors |
| `SERVING` | Arrived at floor, doors open |

## Example Scenarios

### Scenario 1: Single Call (No Ghost Trip)
```
Initial: Lift A at F3, Lift B at F6
Call: Floor 4 UP

Result: Only Lift A moves (F3 → F4)
        Lift B stays at F6 ✓
```

### Scenario 2: Closest Lift Selection
```
Initial: Lift A at F1, Lift B at F5
Call: Floor 4 UP

Result: Lift B selected (distance 1)
        Lift A stays (distance 3) ✓
```

### Scenario 3: Queue When Both Busy
```
Initial: Both lifts moving
Call: Floor 3 DOWN

Result: Call added to pending queue
        Processed when a lift becomes IDLE ✓
```

## Testing

To test the system:

1. Run the app on emulator or device
2. Press floor call buttons (A or B, Up or Down)
3. Observe only one lift responding
4. Tap on a cabin to enter inside panel
5. Select destination floors
6. Watch real-time floor-by-floor movement

## Performance Notes

- Uses `Handler` for UI thread timing
- Animations run on UI thread via `runOnUiThread()`
- Thread-safe with `synchronized` methods in controller
- Efficient queue management with ArrayList
