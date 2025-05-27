# Button Instructions for Warehouse Camera App

## Main Interface Buttons

### 1. Navigation Buttons

- **Back Button (←)**: 
  - Located in the top-left corner of each screen
  - Returns to the previous screen
  - On the main screen, displays a confirmation dialog before exiting

- **Home Button (🏠)**: 
  - Located in the top-right corner of most screens
  - Returns directly to the main screen
  - Displays a confirmation dialog if there is unsaved data

### 2. Camera Control Buttons

- **Capture Button (📸)**: 
  - Large circular button at the bottom center of the camera screen
  - When pressed:
    - Triggers a flash animation for immediate visual feedback
    - Captures the photo (processing happens in background)
    - Displays "📸 Snapshot taken! Processing..." while the photo is being processed
    - Changes to "✅ Photo ready!" when processing is complete
  - Temporarily disabled during photo processing to prevent multiple captures

- **Flash Toggle (⚡️)**: 
  - Located on the camera screen toolbar
  - Cycles through flash modes: Auto → On → Off
  - Current mode is indicated by icon change

- **Camera Switch (🔄)**: 
  - Switches between front and rear cameras
  - Shows a rotation animation during transition

### 3. Marker Buttons

- **Green Marker (🟢)**: 
  - Indicates lowest importance level (1)
  - Adds a green circle marker to photos

- **Yellow Marker (🟡)**: 
  - Indicates medium importance level (2)
  - Adds a yellow circle marker to photos

- **Red Marker (🔴)**: 
  - Indicates highest importance level (3)
  - Adds a red circle marker to photos

### 4. Form Control Buttons

- **Save Button (💾)**: 
  - Located at the bottom of form screens
  - Saves the current data
  - Shows an animation and confirmation message when pressed
  - Validates input before saving
  - Disabled until all required fields are filled

- **Add Item Button (+)**: 
  - Adds a new item to the current list
  - Located next to lists of items
  - Shows a subtle animation when pressed

- **Delete Button (🗑️)**: 
  - Appears in list items and in the file manager
  - Deletes the selected item after confirmation
  - Shows a slide-out animation when pressed

### 5. File Manager Buttons

- **Open Folder Button (📁)**: 
  - Opens the selected folder
  - Double-tap on folder has the same effect

- **View Image Button (🖼️)**: 
  - Opens the selected image in the viewer
  - Double-tap on image has the same effect

- **Up Directory Button (⬆️)**: 
  - Navigates to the parent directory
  - Located at the top of the file manager screen

## Button Interactions

All buttons in the app follow these interaction principles:

1. **Visual Feedback**: 
   - Buttons darken slightly when pressed
   - Interactive elements have subtle animation effects
   - Success/failure actions are accompanied by appropriate animations

2. **Haptic Feedback**:
   - Buttons provide haptic feedback when pressed (if device supports it)
   - Different types of feedback for different actions (success, error, etc.)

3. **Disabled States**:
   - Disabled buttons are grayed out
   - Tooltip appears explaining why a button is disabled when tapped

4. **Long Press Actions**:
   - Some buttons have additional functionality when long-pressed
   - For example, long-pressing a photo in the gallery shows delete/share options
   - Long-pressing the save button shows additional save options

## Optimized Camera Button Implementation

The camera capture button has been optimized in version 1.1.0 to provide a better user experience:

### Previous Implementation Issues:
- Pressing the capture button resulted in a several-second delay
- UI would freeze during processing
- No immediate feedback to the user
- Multiple button presses could cause errors

### New Implementation Features:
1. **Immediate Visual Feedback**:
   - Flash animation appears instantly when button is pressed
   - Status message updates immediately
   
2. **Background Processing**:
   - Photo processing (EXIF rotation, marking, saving) happens in background threads
   - UI remains responsive during processing
   
3. **Button State Management**:
   - Button is temporarily disabled during processing
   - Visual indicator shows processing state
   - Re-enables automatically when processing completes
   
4. **Implementation Details**:
   ```kotlin
   // Coroutine scope for background processing
   private val processingScope = CoroutineScope(Dispatchers.IO)
   
   // Button click handler
   captureButton.setOnClickListener {
       if (!isProcessingPhoto) {
           isProcessingPhoto = true
           showCaptureAnimation()
           takePhoto()
       }
   }
   
   // Immediate feedback function
   private fun showCaptureAnimation() {
       flashOverlay.alpha = 0.7f
       flashOverlay.animate().alpha(0f).setDuration(500).start()
       statusText.text = "📸 Snapshot taken! Processing..."
       captureButton.isEnabled = false
   }
   
   // Background processing
   private fun processPhotoAsync(imageFile: File) {
       processingScope.launch {
           // Process image in background
           // 1. Fix orientation using EXIF
           // 2. Create marked version
           // 3. Save both versions
           
           // Update UI on main thread when done
           withContext(Dispatchers.Main) {
               statusText.text = "✅ Photo ready!"
               captureButton.isEnabled = true
               isProcessingPhoto = false
           }
       }
   }
   ```

## Accessibility Considerations

All buttons in the app are designed with accessibility in mind:

1. **Size and Touch Targets**:
   - All buttons have a minimum touch target of 48x48dp
   - Adequate spacing between touchable elements (minimum 8dp)

2. **Color Contrast**:
   - Buttons maintain at least 4.5:1 contrast ratio with backgrounds
   - Critical buttons have higher contrast for better visibility

3. **Screen Readers**:
   - All buttons have contentDescription attributes for TalkBack
   - Custom actions are properly labeled for screen reader users

4. **Button Labels**:
   - Text buttons have clear, concise labels
   - Icon-only buttons always have text labels or tooltips

## Custom Button Styling

The app's minimalist Apple-inspired design extends to all buttons:

1. **Shape and Style**:
   - Rounded corners (12dp radius) on all buttons
   - Subtle drop shadows (elevation: 2dp)
   - Semi-transparent backgrounds (alpha: 0.9)

2. **Animation Effects**:
   - Scale-down effect when pressed (scaleX/Y: 0.98)
   - Ripple effects contained within button bounds
   - Smooth transitions between states (150ms duration)

3. **State Indicators**:
   - Subtle color shifts between normal/pressed/disabled states
   - Special states for critical actions (delete, save, etc.)
   - Loading indicators for operations in progress
