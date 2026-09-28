package com.elevator.smart.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Model representing a single elevator with its current state and destination queue.
 */
public class Elevator {
    private final String id; // "A" or "B"
    private int currentFloor;
    private ElevatorState state;
    private Direction direction;
    private final List<Integer> destinationQueue; // Car calls (inside lift selections)
    private final List<FloorCall> assignedCalls;  // Hall calls assigned to this lift
    
    public static final int MIN_FLOOR = 1;
    public static final int MAX_FLOOR = 6;
    public static final long FLOOR_TRAVEL_TIME_MS = 1000; // 1 second per floor
    public static final long DOOR_OPEN_TIME_MS = 2000;    // 2 seconds doors open

    public Elevator(String id, int startFloor) {
        this.id = id;
        this.currentFloor = startFloor;
        this.state = ElevatorState.IDLE;
        this.direction = Direction.NONE;
        this.destinationQueue = new ArrayList<>();
        this.assignedCalls = new ArrayList<>();
    }

    // Getters
    public String getId() {
        return id;
    }

    public int getCurrentFloor() {
        return currentFloor;
    }

    public ElevatorState getState() {
        return state;
    }

    public Direction getDirection() {
        return direction;
    }

    public List<Integer> getDestinationQueue() {
        return Collections.unmodifiableList(destinationQueue);
    }

    public List<FloorCall> getAssignedCalls() {
        return Collections.unmodifiableList(assignedCalls);
    }

    // Setters
    public void setCurrentFloor(int floor) {
        if (floor >= MIN_FLOOR && floor <= MAX_FLOOR) {
            this.currentFloor = floor;
        }
    }

    public void setState(ElevatorState state) {
        this.state = state;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    /**
     * Add a destination floor (car call - from inside the lift).
     */
    public void addDestination(int floor) {
        if (floor >= MIN_FLOOR && floor <= MAX_FLOOR && !destinationQueue.contains(floor)) {
            destinationQueue.add(floor);
            optimizeQueue();
        }
    }

    /**
     * Remove a destination floor after it's been served.
     */
    public void removeDestination(int floor) {
        destinationQueue.remove(Integer.valueOf(floor));
    }

    /**
     * Assign a hall call to this elevator.
     */
    public void assignCall(FloorCall call) {
        if (!assignedCalls.contains(call)) {
            assignedCalls.add(call.withAssignedLift(this.id));
        }
    }

    /**
     * Remove a hall call after it's been served.
     */
    public void removeAssignedCall(FloorCall call) {
        assignedCalls.removeIf(c -> c.getFloor() == call.getFloor() && 
                                    c.getDirection() == call.getDirection());
    }

    /**
     * Check if this elevator has any pending work.
     */
    public boolean hasPendingWork() {
        return !destinationQueue.isEmpty() || !assignedCalls.isEmpty();
    }

    /**
     * Check if this elevator is available (IDLE or can take more requests).
     */
    public boolean isAvailable() {
        return state == ElevatorState.IDLE || 
               (state != ElevatorState.SERVING && destinationQueue.size() < 3);
    }

    /**
     * Calculate distance to a target floor.
     */
    public int distanceTo(int targetFloor) {
        return Math.abs(currentFloor - targetFloor);
    }

    /**
     * Get the next floor to visit based on current direction and queue.
     */
    public Integer getNextStop() {
        if (destinationQueue.isEmpty() && assignedCalls.isEmpty()) {
            return null;
        }

        // Combine all target floors
        List<Integer> allTargets = new ArrayList<>(destinationQueue);
        for (FloorCall call : assignedCalls) {
            if (!allTargets.contains(call.getFloor())) {
                allTargets.add(call.getFloor());
            }
        }

        if (allTargets.isEmpty()) {
            return null;
        }

        // Find closest in current direction, or closest overall
        Integer nextStop = null;
        int minDistance = Integer.MAX_VALUE;

        for (int target : allTargets) {
            int distance = distanceTo(target);
            boolean correctDirection = (direction == Direction.UP && target > currentFloor) ||
                                       (direction == Direction.DOWN && target < currentFloor) ||
                                       (direction == Direction.NONE);

            if (correctDirection && distance < minDistance) {
                minDistance = distance;
                nextStop = target;
            }
        }

        // If no stop in current direction, pick closest
        if (nextStop == null) {
            for (int target : allTargets) {
                int distance = distanceTo(target);
                if (distance < minDistance) {
                    minDistance = distance;
                    nextStop = target;
                }
            }
        }

        return nextStop;
    }

    /**
     * Optimize the destination queue based on current direction.
     */
    private void optimizeQueue() {
        if (destinationQueue.size() <= 1) return;

        if (direction == Direction.UP) {
            Collections.sort(destinationQueue);
        } else if (direction == Direction.DOWN) {
            Collections.sort(destinationQueue, Collections.reverseOrder());
        }
    }

    /**
     * Clear all destinations and calls (reset).
     */
    public void reset() {
        destinationQueue.clear();
        assignedCalls.clear();
        state = ElevatorState.IDLE;
        direction = Direction.NONE;
    }

    @Override
    public String toString() {
        return "Elevator " + id + " [Floor=" + currentFloor + ", State=" + state + 
               ", Dir=" + direction + ", Queue=" + destinationQueue + "]";
    }
}
