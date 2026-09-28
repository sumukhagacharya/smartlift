package com.elevator.smart;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.elevator.smart.controller.ElevatorController;
import com.elevator.smart.model.Direction;
import com.elevator.smart.model.ElevatorState;
import com.elevator.smart.model.FloorCall;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main Activity for the Smart Dual Elevator System.
 * Connects the XML layouts to the Java backend logic.
 */
public class MainActivity extends AppCompatActivity implements ElevatorController.ElevatorEventListener {

    // Controller
    private ElevatorController controller;
    
    // Lift Cabin Views
    private LinearLayout llCabinA, llCabinB;
    private TextView tvCabinAFloor, tvCabinBFloor;
    private TextView tvCabinABadge, tvCabinBBadge;
    
    // Status Panel Views
    private TextView tvLiftAStatus, tvLiftBStatus;
    private TextView tvStatusA, tvStatusB;
    
    // Lift Shaft containers
    private View flShaftA, flShaftB;
    
    // Floor Call Buttons - stored in maps for easy access
    private Map<String, Button> floorButtons = new HashMap<>();
    
    // Active button states
    private Map<String, Boolean> activeButtons = new HashMap<>();
    
    // Inside Lift Panel
    private BottomSheetDialog insideLiftDialog;
    private String currentInsideLift = null;
    
    // Constants
    private static final int TOTAL_FLOORS = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        initController();
        setupClickListeners();
        updateUI();
    }

    // ==================== INITIALIZATION ====================

    private void initViews() {
        // Lift A Cabin
        llCabinA = findViewById(R.id.ll_cabin_a);
        tvCabinAFloor = findViewById(R.id.tv_cabin_a_floor);
        tvCabinABadge = findViewById(R.id.tv_cabin_a_badge);
        
        // Lift B Cabin
        llCabinB = findViewById(R.id.ll_cabin_b);
        tvCabinBFloor = findViewById(R.id.tv_cabin_b_floor);
        tvCabinBBadge = findViewById(R.id.tv_cabin_b_badge);
        
        // Status Panel
        tvLiftAStatus = findViewById(R.id.tv_lift_a_status);
        tvLiftBStatus = findViewById(R.id.tv_lift_b_status);
        tvStatusA = findViewById(R.id.tv_status_a);
        tvStatusB = findViewById(R.id.tv_status_b);
        
        // Lift Shafts
        flShaftA = findViewById(R.id.fl_shaft_a);
        flShaftB = findViewById(R.id.fl_shaft_b);
        
        // Initialize floor buttons from each floor row
        initFloorButtons();
    }
    
    private void initFloorButtons() {
        // Floor button IDs follow pattern: btn_floor{N}_{lift}_{direction}
        // Example: btn_floor3_a_up, btn_floor5_b_down
        
        for (int floor = 1; floor <= TOTAL_FLOORS; floor++) {
            // Lift A buttons
            if (floor < TOTAL_FLOORS) { // Up button (not on top floor)
                String upId = "btn_floor" + floor + "_a_up";
                int resId = getResources().getIdentifier(upId, "id", getPackageName());
                if (resId != 0) {
                    Button btn = findViewById(resId);
                    if (btn != null) {
                        floorButtons.put(upId, btn);
                        activeButtons.put(upId, false);
                    }
                }
            }
            
            if (floor > 1) { // Down button (not on ground floor)
                String downId = "btn_floor" + floor + "_a_down";
                int resId = getResources().getIdentifier(downId, "id", getPackageName());
                if (resId != 0) {
                    Button btn = findViewById(resId);
                    if (btn != null) {
                        floorButtons.put(downId, btn);
                        activeButtons.put(downId, false);
                    }
                }
            }
            
            // Lift B buttons
            if (floor < TOTAL_FLOORS) {
                String upId = "btn_floor" + floor + "_b_up";
                int resId = getResources().getIdentifier(upId, "id", getPackageName());
                if (resId != 0) {
                    Button btn = findViewById(resId);
                    if (btn != null) {
                        floorButtons.put(upId, btn);
                        activeButtons.put(upId, false);
                    }
                }
            }
            
            if (floor > 1) {
                String downId = "btn_floor" + floor + "_b_down";
                int resId = getResources().getIdentifier(downId, "id", getPackageName());
                if (resId != 0) {
                    Button btn = findViewById(resId);
                    if (btn != null) {
                        floorButtons.put(downId, btn);
                        activeButtons.put(downId, false);
                    }
                }
            }
        }
    }

    private void initController() {
        controller = new ElevatorController(this);
    }

    private void setupClickListeners() {
        // Floor call buttons
        for (Map.Entry<String, Button> entry : floorButtons.entrySet()) {
            String key = entry.getKey();
            Button button = entry.getValue();
            
            button.setOnClickListener(v -> handleFloorButtonClick(key));
        }
        
        // Cabin click listeners (to enter lift)
        llCabinA.setOnClickListener(v -> showInsideLiftPanel("A"));
        llCabinB.setOnClickListener(v -> showInsideLiftPanel("B"));
    }
    
    // ==================== BUTTON HANDLERS ====================

    /**
     * Handle floor button click with SYNCED logic.
     * 
     * Key behavior:
     * - Pressing Lift A or Lift B button for same floor/direction = same call
     * - System decides which lift responds based on proximity
     * - BOTH buttons for that floor/direction get highlighted
     * - Color indicates which lift is actually responding
     */
    private void handleFloorButtonClick(String buttonId) {
        // Parse button ID: btn_floor{N}_{lift}_{direction}
        // Example: btn_floor3_a_up
        
        String[] parts = buttonId.replace("btn_floor", "").split("_");
        if (parts.length < 3) return;
        
        int floor = Integer.parseInt(parts[0]);
        String dirStr = parts[2]; // "up" or "down"
        Direction direction = dirStr.equals("up") ? Direction.UP : Direction.DOWN;
        
        // Check if buttons for this floor/direction are already active
        String buttonA = "btn_floor" + floor + "_a_" + dirStr;
        String buttonB = "btn_floor" + floor + "_b_" + dirStr;
        
        boolean alreadyActive = activeButtons.getOrDefault(buttonA, false) || 
                                activeButtons.getOrDefault(buttonB, false);
        
        if (alreadyActive) {
            // Call already registered for this floor/direction
            return;
        }
        
        // Call synced method - SYSTEM decides which lift responds
        String respondingLift = controller.handleSyncedHallCall(floor, direction);
        
        if (respondingLift != null) {
            // Highlight BOTH A and B buttons for this floor/direction
            // to show the call is registered, with the responding lift's color
            activeButtons.put(buttonA, true);
            activeButtons.put(buttonB, true);
            updateButtonStyle(buttonA, true, respondingLift);
            updateButtonStyle(buttonB, true, respondingLift);
        } else {
            // Call is pending (both lifts busy) - highlight with neutral color
            activeButtons.put(buttonA, true);
            activeButtons.put(buttonB, true);
            updateButtonStyle(buttonA, true, "A"); // Will use first available
            updateButtonStyle(buttonB, true, "A");
        }
    }
    
    private void updateButtonStyle(String buttonId, boolean active, String respondingLift) {
        Button button = floorButtons.get(buttonId);
        if (button == null) return;
        
        if (active) {
            // Set active background based on which lift is RESPONDING (not which button was pressed)
            int bgRes = respondingLift.equals("A") ? R.drawable.bg_call_button_a : R.drawable.bg_call_button_b;
            button.setBackgroundResource(bgRes);
        } else {
            button.setBackgroundResource(R.drawable.bg_call_button);
        }
    }

    // ==================== INSIDE LIFT PANEL ====================

    private void showInsideLiftPanel(String liftId) {
        currentInsideLift = liftId;
        
        insideLiftDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_inside_lift, null);
        insideLiftDialog.setContentView(sheetView);
        
        // Set title
        TextView tvLabel = sheetView.findViewById(R.id.tv_inside_lift_label);
        tvLabel.setText("Inside Lift " + liftId);
        
        // Set current floor
        TextView tvCurrentFloor = sheetView.findViewById(R.id.tv_inside_current_floor);
        int currentFloor = controller.getCurrentFloor(liftId);
        tvCurrentFloor.setText("Floor " + currentFloor);
        
        // Setup floor selection buttons
        for (int floor = 1; floor <= TOTAL_FLOORS; floor++) {
            String btnId = "btn_select_floor_" + floor;
            int resId = getResources().getIdentifier(btnId, "id", getPackageName());
            if (resId != 0) {
                Button btn = sheetView.findViewById(resId);
                if (btn != null) {
                    final int targetFloor = floor;
                    
                    // Highlight if already selected
                    List<Integer> queue = controller.getDestinationQueue(liftId);
                    if (queue.contains(floor)) {
                        btn.setBackgroundResource(liftId.equals("A") ? 
                            R.drawable.bg_call_button_a : R.drawable.bg_call_button_b);
                    }
                    
                    btn.setOnClickListener(v -> {
                        controller.handleCarCall(currentInsideLift, targetFloor);
                        btn.setBackgroundResource(currentInsideLift.equals("A") ? 
                            R.drawable.bg_call_button_a : R.drawable.bg_call_button_b);
                    });
                }
            }
        }
        
        // Exit button
        Button btnExit = sheetView.findViewById(R.id.btn_exit_lift);
        btnExit.setOnClickListener(v -> insideLiftDialog.dismiss());
        
        insideLiftDialog.show();
    }

    // ==================== UI UPDATES ====================

    private void updateUI() {
        // Update cabin floor displays
        tvCabinAFloor.setText(String.valueOf(controller.getCurrentFloor("A")));
        tvCabinBFloor.setText(String.valueOf(controller.getCurrentFloor("B")));
        
        // Update status badges
        updateStatusBadge("A", controller.getState("A"));
        updateStatusBadge("B", controller.getState("B"));
        
        // Update destination badges
        updateDestinationBadge("A", controller.getDestinationQueue("A"));
        updateDestinationBadge("B", controller.getDestinationQueue("B"));
        
        // Position cabins
        positionCabin("A", controller.getCurrentFloor("A"));
        positionCabin("B", controller.getCurrentFloor("B"));
    }
    
    private void updateStatusBadge(String liftId, ElevatorState state) {
        TextView badge = liftId.equals("A") ? tvLiftAStatus : tvLiftBStatus;
        TextView statusText = liftId.equals("A") ? tvStatusA : tvStatusB;
        
        switch (state) {
            case IDLE:
                badge.setText("IDLE");
                badge.setBackgroundResource(R.drawable.bg_status_idle);
                statusText.setText("Waiting");
                break;
            case MOVING:
                badge.setText("MOVING");
                badge.setBackgroundResource(R.drawable.bg_status_moving);
                statusText.setText("In transit");
                break;
            case SERVING:
                badge.setText("SERVING");
                badge.setBackgroundResource(R.drawable.bg_status_serving);
                statusText.setText("Doors open");
                break;
        }
    }
    
    private void updateDestinationBadge(String liftId, List<Integer> queue) {
        TextView badge = liftId.equals("A") ? tvCabinABadge : tvCabinBBadge;
        
        if (queue.isEmpty()) {
            badge.setVisibility(View.GONE);
        } else {
            badge.setVisibility(View.VISIBLE);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < queue.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(queue.get(i));
            }
            badge.setText(sb.toString());
        }
    }
    
    private void positionCabin(String liftId, int floor) {
        LinearLayout cabin = liftId.equals("A") ? llCabinA : llCabinB;
        View shaft = liftId.equals("A") ? flShaftA : flShaftB;
        
        // Calculate position (floor 1 at bottom, floor 6 at top)
        shaft.post(() -> {
            float shaftHeight = shaft.getHeight();
            float cabinHeight = cabin.getHeight();
            float floorHeight = (shaftHeight - cabinHeight) / (TOTAL_FLOORS - 1);
            float targetY = (TOTAL_FLOORS - floor) * floorHeight;
            
            cabin.animate()
                .translationY(targetY)
                .setDuration(0) // Instant for initial positioning
                .start();
        });
    }
    
    private void animateCabinToFloor(String liftId, int floor) {
        LinearLayout cabin = liftId.equals("A") ? llCabinA : llCabinB;
        View shaft = liftId.equals("A") ? flShaftA : flShaftB;
        
        float shaftHeight = shaft.getHeight();
        float cabinHeight = cabin.getHeight();
        float floorHeight = (shaftHeight - cabinHeight) / (TOTAL_FLOORS - 1);
        float targetY = (TOTAL_FLOORS - floor) * floorHeight;
        
        cabin.animate()
            .translationY(targetY)
            .setDuration(500) // Smooth animation
            .start();
    }

    // ==================== ELEVATOR EVENT CALLBACKS ====================

    @Override
    public void onFloorChanged(String liftId, int floor) {
        runOnUiThread(() -> {
            // Update floor display
            if (liftId.equals("A")) {
                tvCabinAFloor.setText(String.valueOf(floor));
            } else {
                tvCabinBFloor.setText(String.valueOf(floor));
            }
            
            // Animate cabin
            animateCabinToFloor(liftId, floor);
            
            // Update inside panel if open
            if (insideLiftDialog != null && insideLiftDialog.isShowing() && 
                liftId.equals(currentInsideLift)) {
                TextView tvCurrentFloor = insideLiftDialog.findViewById(R.id.tv_inside_current_floor);
                if (tvCurrentFloor != null) {
                    tvCurrentFloor.setText("Floor " + floor);
                }
            }
        });
    }

    @Override
    public void onStateChanged(String liftId, ElevatorState state) {
        runOnUiThread(() -> updateStatusBadge(liftId, state));
    }

    @Override
    public void onDirectionChanged(String liftId, Direction direction) {
        // Could add direction indicator UI here
    }

    @Override
    public void onDestinationQueueChanged(String liftId, List<Integer> queue) {
        runOnUiThread(() -> updateDestinationBadge(liftId, queue));
    }

    @Override
    public void onCallAssigned(FloorCall call, String liftId) {
        // Button is already highlighted in handleFloorButtonClick
    }

    @Override
    public void onCallServed(FloorCall call, String liftId) {
        runOnUiThread(() -> {
            // Deactivate BOTH buttons for this floor/direction (synced behavior)
            String direction = call.getDirection() == Direction.UP ? "up" : "down";
            String buttonA = "btn_floor" + call.getFloor() + "_a_" + direction;
            String buttonB = "btn_floor" + call.getFloor() + "_b_" + direction;
            
            activeButtons.put(buttonA, false);
            activeButtons.put(buttonB, false);
            updateButtonStyle(buttonA, false, "A");
            updateButtonStyle(buttonB, false, "B");
        });
    }

    @Override
    public void onDoorsOpening(String liftId, int floor) {
        // Could add door animation here
    }

    @Override
    public void onDoorsClosing(String liftId, int floor) {
        // Could add door animation here
    }

    @Override
    public void onLog(String message) {
        // Could display logs in a debug panel
        android.util.Log.d("ElevatorSystem", message);
    }
}
