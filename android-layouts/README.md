# Android Elevator UI Layouts

This folder contains all the XML layout files for the Smart Dual Elevator System Android app.

## Structure

```
android-layouts/
└── res/
    ├── drawable/              # Background shapes and selectors
    │   ├── bg_card.xml
    │   ├── bg_floor_row.xml
    │   ├── bg_floor_number.xml
    │   ├── bg_lift_label_a.xml
    │   ├── bg_lift_label_b.xml
    │   ├── bg_status_idle.xml
    │   ├── bg_status_moving.xml
    │   ├── bg_status_serving.xml
    │   ├── bg_call_button.xml
    │   ├── bg_call_button_a.xml
    │   ├── bg_call_button_b.xml
    │   ├── bg_lift_shaft.xml
    │   ├── bg_lift_cabin_a.xml
    │   ├── bg_lift_cabin_b.xml
    │   ├── bg_inside_panel.xml
    │   ├── bg_floor_select_button.xml
    │   ├── ic_arrow_up.xml
    │   └── ic_arrow_down.xml
    ├── layout/                # Layout files
    │   ├── activity_main.xml           # Main activity layout
    │   ├── component_lift_shaft_a.xml  # Lift A shaft component
    │   ├── component_lift_shaft_b.xml  # Lift B shaft component
    │   ├── component_status_panel.xml  # Status panel component
    │   ├── dialog_inside_lift.xml      # Inside lift bottom sheet
    │   ├── item_floor_row_1.xml        # Floor 1 (Ground)
    │   ├── item_floor_row_2.xml        # Floor 2
    │   ├── item_floor_row_3.xml        # Floor 3
    │   ├── item_floor_row_4.xml        # Floor 4
    │   ├── item_floor_row_5.xml        # Floor 5
    │   └── item_floor_row_6.xml        # Floor 6
    └── values/                # Resource values
        ├── colors.xml         # Color definitions
        ├── dimens.xml         # Dimension values
        └── styles.xml         # Theme and styles
```

## Key View IDs for Java Backend Integration

### Lift Cabins
- `ll_cabin_a` - Lift A cabin container (animate Y position)
- `ll_cabin_b` - Lift B cabin container (animate Y position)
- `tv_cabin_a_floor` - Lift A current floor text
- `tv_cabin_b_floor` - Lift B current floor text
- `tv_cabin_a_badge` - Lift A selected floors badge
- `tv_cabin_b_badge` - Lift B selected floors badge

### Status
- `tv_lift_a_status` - Lift A status badge (IDLE/MOVING/SERVING)
- `tv_lift_b_status` - Lift B status badge
- `tv_status_a` - Lift A status message
- `tv_status_b` - Lift B status message

### Call Buttons (Pattern: btn_floor{N}_{lift}_{direction})
Examples:
- `btn_floor3_a_up` - Floor 3, Lift A, Up button
- `btn_floor5_b_down` - Floor 5, Lift B, Down button

### Inside Lift Panel
- `tv_inside_lift_label` - "Inside Lift A/B" header
- `tv_inside_current_floor` - Current floor display
- `btn_select_floor_{1-6}` - Floor selection buttons
- `btn_exit_lift` - Exit button

## Animation Tips

To animate the lift cabin position:
```java
// Calculate Y position based on floor (1-6)
float shaftHeight = flShaftA.getHeight();
float floorHeight = shaftHeight / 6f;
float targetY = shaftHeight - (floor * floorHeight);

llCabinA.animate()
    .translationY(targetY)
    .setDuration(500)
    .start();
```

## Status Badge Styles
Apply these styles dynamically based on lift state:
- `@style/StatusBadge.Idle` - Green, for IDLE state
- `@style/StatusBadge.Moving` - Amber, for MOVING state
- `@style/StatusBadge.Serving` - Blue, for SERVING state
