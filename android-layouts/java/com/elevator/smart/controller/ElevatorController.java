package com.elevator.smart.controller;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.elevator.smart.model.Direction;
import com.elevator.smart.model.Elevator;
import com.elevator.smart.model.ElevatorState;
import com.elevator.smart.model.FloorCall;

import java.util.ArrayList;
import java.util.List;

/**
 * Smart Elevator Controller that manages two elevators with intelligent scheduling.
 * 
 * Key Features:
 * - Prevents "ghost trips" by assigning only ONE lift per call
 * - Calculates optimal lift based on distance and availability
 * - Handles both hall calls (outside) and car calls (inside)
 * - Real-time floor-by-floor movement simulation
 */
public class ElevatorController {
    private static final String TAG = "ElevatorController";
    
    private final Elevator liftA;
    private final Elevator liftB;
    private final Handler handler;
    private final List<FloorCall> pendingCalls;
    private final ElevatorEventListener listener;
    
    private boolean isLiftAMoving = false;
    private boolean isLiftBMoving = false;

    /**
     * Interface for UI updates and events.
     */
    public interface ElevatorEventListener {
        void onFloorChanged(String liftId, int floor);
        void onStateChanged(String liftId, ElevatorState state);
        void onDirectionChanged(String liftId, Direction direction);
        void onDestinationQueueChanged(String liftId, List<Integer> queue);
        void onCallAssigned(FloorCall call, String liftId);
        void onCallServed(FloorCall call, String liftId);
        void onDoorsOpening(String liftId, int floor);
        void onDoorsClosing(String liftId, int floor);
        void onLog(String message);
    }

    public ElevatorController(ElevatorEventListener listener) {
        this.liftA = new Elevator("A", 1); // Lift A starts at floor 1
        this.liftB = new Elevator("B", 6); // Lift B starts at floor 6
        this.handler = new Handler(Looper.getMainLooper());
        this.pendingCalls = new ArrayList<>();
        this.listener = listener;
        
        log("Elevator Controller initialized. Lift A at F1, Lift B at F6");
    }

    // ==================== PUBLIC API ====================

    /**
     * Handle a hall call (someone pressing up/down button on a floor).
     * This is the main entry point for external calls.
     */
    public synchronized void handleHallCall(int floor, Direction direction) {
        log("Hall call received: Floor " + floor + " " + direction);
        
        FloorCall call = new FloorCall(floor, direction);
        
        // Check if this call already exists
        if (isCallAlreadyPending(call)) {
            log("Call already pending, ignoring duplicate");
            return;
        }
        
        // Find the best lift for this call
        String assignedLift = selectBestLift(floor, direction);
        
        if (assignedLift != null) {
            FloorCall assignedCall = call.withAssignedLift(assignedLift);
            
            if (assignedLift.equals("A")) {
                liftA.assignCall(assignedCall);
                listener.onCallAssigned(assignedCall, "A");
                log("Call assigned to Lift A");
                startLiftMovement("A");
            } else {
                liftB.assignCall(assignedCall);
                listener.onCallAssigned(assignedCall, "B");
                log("Call assigned to Lift B");
                startLiftMovement("B");
            }
        } else {
            // Both lifts busy, add to pending queue
            pendingCalls.add(call);
            log("Both lifts busy, call added to pending queue");
        }
    }

    /**
     * Handle a SYNCED hall call - buttons are linked.
     * The button pressed indicates the shaft side, but the SYSTEM decides which lift responds.
     * 
     * Key Logic:
     * - Both A and B buttons on the same floor/direction are treated as ONE call
     * - System picks the closest available lift using smart algorithm
     * - Returns which lift is actually responding (so UI can highlight correctly)
     * 
     * @return The lift ID that will respond ("A" or "B"), or null if call queued
     */
    public synchronized String handleSyncedHallCall(int floor, Direction direction) {
        log("Synced hall call: Floor " + floor + " " + direction);
        
        FloorCall call = new FloorCall(floor, direction);
        
        // Check if this call already exists (someone already pressed for this floor/direction)
        String existingLift = getAssignedLiftForCall(floor, direction);
        if (existingLift != null) {
            log("Call already assigned to Lift " + existingLift + ", returning existing assignment");
            return existingLift;
        }
        
        // Check pending queue too
        for (FloorCall pending : pendingCalls) {
            if (pending.getFloor() == floor && pending.getDirection() == direction) {
                log("Call already in pending queue");
                return null; // Call is pending, no specific lift yet
            }
        }
        
        // Use smart selection algorithm to pick the BEST lift
        String assignedLift = selectBestLift(floor, direction);
        
        if (assignedLift != null) {
            FloorCall assignedCall = call.withAssignedLift(assignedLift);
            
            if (assignedLift.equals("A")) {
                liftA.assignCall(assignedCall);
                log("SMART DECISION: Lift A selected (closer/available)");
            } else {
                liftB.assignCall(assignedCall);
                log("SMART DECISION: Lift B selected (closer/available)");
            }
            
            listener.onCallAssigned(assignedCall, assignedLift);
            startLiftMovement(assignedLift);
            return assignedLift;
        } else {
            // Both lifts busy, add to pending queue
            pendingCalls.add(call);
            log("Both lifts busy, call added to pending queue");
            return null;
        }
    }
    
    /**
     * Get which lift is assigned to handle a specific floor/direction call.
     * Returns null if no lift is assigned.
     */
    public String getAssignedLiftForCall(int floor, Direction direction) {
        for (FloorCall call : liftA.getAssignedCalls()) {
            if (call.getFloor() == floor && call.getDirection() == direction) {
                return "A";
            }
        }
        for (FloorCall call : liftB.getAssignedCalls()) {
            if (call.getFloor() == floor && call.getDirection() == direction) {
                return "B";
            }
        }
        return null;
    }

    /**
     * Handle a car call (someone inside the lift pressing a floor button).
     */
    public synchronized void handleCarCall(String liftId, int destinationFloor) {
        log("Car call: Lift " + liftId + " to Floor " + destinationFloor);
        
        Elevator lift = liftId.equals("A") ? liftA : liftB;
        
        if (lift.getCurrentFloor() == destinationFloor) {
            log("Already at destination floor");
            return;
        }
        
        lift.addDestination(destinationFloor);
        listener.onDestinationQueueChanged(liftId, lift.getDestinationQueue());
        startLiftMovement(liftId);
    }

    /**
     * Get current floor of a lift.
     */
    public int getCurrentFloor(String liftId) {
        return liftId.equals("A") ? liftA.getCurrentFloor() : liftB.getCurrentFloor();
    }

    /**
     * Get current state of a lift.
     */
    public ElevatorState getState(String liftId) {
        return liftId.equals("A") ? liftA.getState() : liftB.getState();
    }

    /**
     * Get destination queue of a lift.
     */
    public List<Integer> getDestinationQueue(String liftId) {
        return liftId.equals("A") ? liftA.getDestinationQueue() : liftB.getDestinationQueue();
    }

    /**
     * Reset both lifts to initial state.
     */
    public synchronized void reset() {
        handler.removeCallbacksAndMessages(null);
        isLiftAMoving = false;
        isLiftBMoving = false;
        
        liftA.reset();
        liftA.setCurrentFloor(1);
        
        liftB.reset();
        liftB.setCurrentFloor(6);
        
        pendingCalls.clear();
        
        listener.onFloorChanged("A", 1);
        listener.onFloorChanged("B", 6);
        listener.onStateChanged("A", ElevatorState.IDLE);
        listener.onStateChanged("B", ElevatorState.IDLE);
        listener.onDestinationQueueChanged("A", new ArrayList<>());
        listener.onDestinationQueueChanged("B", new ArrayList<>());
        
        log("System reset. Lift A at F1, Lift B at F6");
    }

    // ==================== SMART SCHEDULING LOGIC ====================

    /**
     * Select the best lift for a hall call.
     * This is the core algorithm that prevents ghost trips.
     * 
     * Selection criteria:
     * 1. If one lift is IDLE and other is busy, pick IDLE
     * 2. If both IDLE, pick closest
     * 3. If both MOVING, pick the one going towards the call floor in correct direction
     * 4. If neither suitable, return null (add to pending)
     */
    private String selectBestLift(int floor, Direction direction) {
        boolean aAvailable = liftA.isAvailable();
        boolean bAvailable = liftB.isAvailable();
        
        int distanceA = liftA.distanceTo(floor);
        int distanceB = liftB.distanceTo(floor);
        
        boolean aIdle = liftA.getState() == ElevatorState.IDLE;
        boolean bIdle = liftB.getState() == ElevatorState.IDLE;
        
        log("Selecting lift: A(dist=" + distanceA + ", idle=" + aIdle + ", avail=" + aAvailable + 
            ") B(dist=" + distanceB + ", idle=" + bIdle + ", avail=" + bAvailable + ")");

        // Case 1: One idle, one busy - pick idle
        if (aIdle && !bIdle) {
            return "A";
        }
        if (bIdle && !aIdle) {
            return "B";
        }
        
        // Case 2: Both idle - pick closest
        if (aIdle && bIdle) {
            return distanceA <= distanceB ? "A" : "B";
        }
        
        // Case 3: Both moving - check direction compatibility
        if (aAvailable && bAvailable) {
            boolean aTowards = isMovingTowards(liftA, floor, direction);
            boolean bTowards = isMovingTowards(liftB, floor, direction);
            
            if (aTowards && !bTowards) return "A";
            if (bTowards && !aTowards) return "B";
            if (aTowards && bTowards) {
                return distanceA <= distanceB ? "A" : "B";
            }
        }
        
        // Case 4: Pick any available, prefer closer
        if (aAvailable && !bAvailable) return "A";
        if (bAvailable && !aAvailable) return "B";
        if (aAvailable && bAvailable) {
            return distanceA <= distanceB ? "A" : "B";
        }
        
        // Both busy
        return null;
    }

    /**
     * Check if a lift is moving towards a floor in the requested direction.
     */
    private boolean isMovingTowards(Elevator lift, int targetFloor, Direction requestedDir) {
        Direction liftDir = lift.getDirection();
        int currentFloor = lift.getCurrentFloor();
        
        if (liftDir == Direction.NONE) return true;
        
        if (liftDir == Direction.UP && requestedDir == Direction.UP) {
            return targetFloor >= currentFloor;
        }
        if (liftDir == Direction.DOWN && requestedDir == Direction.DOWN) {
            return targetFloor <= currentFloor;
        }
        
        return false;
    }

    /**
     * Check if a call is already pending or assigned.
     */
    private boolean isCallAlreadyPending(FloorCall call) {
        // Check pending queue
        for (FloorCall pending : pendingCalls) {
            if (pending.getFloor() == call.getFloor() && 
                pending.getDirection() == call.getDirection()) {
                return true;
            }
        }
        
        // Check assigned calls
        for (FloorCall assigned : liftA.getAssignedCalls()) {
            if (assigned.getFloor() == call.getFloor() && 
                assigned.getDirection() == call.getDirection()) {
                return true;
            }
        }
        for (FloorCall assigned : liftB.getAssignedCalls()) {
            if (assigned.getFloor() == call.getFloor() && 
                assigned.getDirection() == call.getDirection()) {
                return true;
            }
        }
        
        return false;
    }

    // ==================== MOVEMENT LOGIC ====================

    /**
     * Start the movement loop for a lift if not already moving.
     */
    private void startLiftMovement(String liftId) {
        if (liftId.equals("A") && !isLiftAMoving) {
            isLiftAMoving = true;
            processLiftMovement(liftA);
        } else if (liftId.equals("B") && !isLiftBMoving) {
            isLiftBMoving = true;
            processLiftMovement(liftB);
        }
    }

    /**
     * Process movement for a lift - moves floor by floor.
     */
    private void processLiftMovement(Elevator lift) {
        String liftId = lift.getId();
        
        // Get next stop
        Integer nextStop = lift.getNextStop();
        
        if (nextStop == null) {
            // No more destinations
            lift.setState(ElevatorState.IDLE);
            lift.setDirection(Direction.NONE);
            listener.onStateChanged(liftId, ElevatorState.IDLE);
            listener.onDirectionChanged(liftId, Direction.NONE);
            
            if (liftId.equals("A")) isLiftAMoving = false;
            else isLiftBMoving = false;
            
            log("Lift " + liftId + " is now IDLE at Floor " + lift.getCurrentFloor());
            
            // Check for pending calls
            processPendingCalls();
            return;
        }
        
        int currentFloor = lift.getCurrentFloor();
        
        if (currentFloor == nextStop) {
            // Arrived at destination - serve it
            serveFloor(lift, currentFloor);
        } else {
            // Need to move
            Direction moveDir = nextStop > currentFloor ? Direction.UP : Direction.DOWN;
            lift.setDirection(moveDir);
            lift.setState(ElevatorState.MOVING);
            
            listener.onStateChanged(liftId, ElevatorState.MOVING);
            listener.onDirectionChanged(liftId, moveDir);
            
            // Move one floor after delay
            handler.postDelayed(() -> {
                int newFloor = currentFloor + (moveDir == Direction.UP ? 1 : -1);
                lift.setCurrentFloor(newFloor);
                listener.onFloorChanged(liftId, newFloor);
                
                log("Lift " + liftId + " moved to Floor " + newFloor);
                
                // Continue processing
                processLiftMovement(lift);
            }, Elevator.FLOOR_TRAVEL_TIME_MS);
        }
    }

    /**
     * Serve a floor - open doors, wait, close doors.
     */
    private void serveFloor(Elevator lift, int floor) {
        String liftId = lift.getId();
        
        lift.setState(ElevatorState.SERVING);
        listener.onStateChanged(liftId, ElevatorState.SERVING);
        listener.onDoorsOpening(liftId, floor);
        log("Lift " + liftId + " doors opening at Floor " + floor);
        
        // Remove this floor from destinations
        lift.removeDestination(floor);
        listener.onDestinationQueueChanged(liftId, lift.getDestinationQueue());
        
        // Remove any hall calls for this floor
        List<FloorCall> toRemove = new ArrayList<>();
        for (FloorCall call : lift.getAssignedCalls()) {
            if (call.getFloor() == floor) {
                toRemove.add(call);
                listener.onCallServed(call, liftId);
            }
        }
        for (FloorCall call : toRemove) {
            lift.removeAssignedCall(call);
        }
        
        // Wait for passengers, then close doors
        handler.postDelayed(() -> {
            listener.onDoorsClosing(liftId, floor);
            log("Lift " + liftId + " doors closing at Floor " + floor);
            
            // Continue processing after doors close
            handler.postDelayed(() -> {
                processLiftMovement(lift);
            }, 500);
            
        }, Elevator.DOOR_OPEN_TIME_MS);
    }

    /**
     * Process any pending calls when a lift becomes available.
     */
    private void processPendingCalls() {
        if (pendingCalls.isEmpty()) return;
        
        List<FloorCall> toProcess = new ArrayList<>(pendingCalls);
        pendingCalls.clear();
        
        for (FloorCall call : toProcess) {
            handleHallCall(call.getFloor(), call.getDirection());
        }
    }

    // ==================== UTILITY ====================

    private void log(String message) {
        Log.d(TAG, message);
        if (listener != null) {
            listener.onLog(message);
        }
    }
}
