package com.elevator.smart.model;

/**
 * Enum representing the current state of an elevator.
 */
public enum ElevatorState {
    IDLE,       // Elevator is stationary, not serving any request
    MOVING,     // Elevator is moving between floors
    SERVING     // Elevator has arrived and is serving passengers (doors open)
}
