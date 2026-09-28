package com.elevator.smart.model;

/**
 * Model representing a floor call request (hall call).
 * This is when someone presses the up/down button on a floor.
 */
public class FloorCall {
    private final int floor;
    private final Direction direction;
    private final String assignedLift; // "A" or "B" or null if not assigned
    private final long timestamp;
    private boolean served;

    public FloorCall(int floor, Direction direction) {
        this.floor = floor;
        this.direction = direction;
        this.assignedLift = null;
        this.timestamp = System.currentTimeMillis();
        this.served = false;
    }

    public FloorCall(int floor, Direction direction, String assignedLift) {
        this.floor = floor;
        this.direction = direction;
        this.assignedLift = assignedLift;
        this.timestamp = System.currentTimeMillis();
        this.served = false;
    }

    public int getFloor() {
        return floor;
    }

    public Direction getDirection() {
        return direction;
    }

    public String getAssignedLift() {
        return assignedLift;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isServed() {
        return served;
    }

    public void setServed(boolean served) {
        this.served = served;
    }

    public FloorCall withAssignedLift(String liftId) {
        FloorCall newCall = new FloorCall(this.floor, this.direction, liftId);
        return newCall;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        FloorCall floorCall = (FloorCall) obj;
        return floor == floorCall.floor && direction == floorCall.direction;
    }

    @Override
    public int hashCode() {
        return 31 * floor + direction.hashCode();
    }

    @Override
    public String toString() {
        return "FloorCall{floor=" + floor + ", direction=" + direction + 
               ", assignedLift=" + assignedLift + ", served=" + served + "}";
    }
}
