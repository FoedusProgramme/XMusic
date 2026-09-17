# Fix RichTooltip Placement and Anchor Logic

The user reported that `RichTooltip` doesn't correctly show from the top when the anchor is in the bottom half of the screen. Based on the analysis, the main issues are:
1. **Full-screen Anchor**: The `miniPlayer` view used as an anchor is `match_parent` in height, so its center is always in the middle of the screen. This causes the "bottom half" logic to be inconsistent.
2. **Caret Orientation**: The caret points the wrong way if the tooltip is placed below a view that is actually at the bottom of the screen but considered "middle" by the logic.
3. **Edge Case Handling**: The tooltip doesn't account for screen boundaries when calculating Y position.

## Proposed Changes

### 1. [RichTooltip.java](file:///P:/XMusic/app/src/main/java/com/xapps/media/xmusic/widget/RichTooltip.java)

- Improve the `showAbove` logic to check if there is enough space above the anchor before deciding placement.
- Fix `SmoothCaretEdgeTreatment` to handle `interpolation` for smoother animations.
- Add screen boundary constraints for Y-axis to prevent tooltips from being cut off.
- Refactor `SmoothCaretEdgeTreatment` to simplify coordinate calculations using the provided `length` parameter.

### 2. [LogicManager.java](file:///P:/XMusic/app/src/main/java/com/xapps/media/xmusic/activity/manager/LogicManager.java)

- Update the tooltip anchor from `binding.miniPlayer` to `binding.collapsedPlayer.root` to ensure it correctly identifies its position at the bottom of the screen.

## User Review Required

> [!IMPORTANT]
> The `ExpressiveSliderLayout` (miniPlayer) is a full-screen view. Anchoring to it will always center the tooltip on the screen. Switching to `collapsedPlayer.root` will make the tooltip point specifically to the playback control bar at the bottom.

## Verification Plan

### Manual Verification
- Deploy to device/emulator.
- Trigger the "Did you know?" tooltip (shows after 5th app opening when mini player is collapsed).
- Verify the tooltip appears ABOVE the mini player bar with the caret pointing DOWN to it.
- Test with different anchor positions (e.g. settings items) to ensure the logic still works for top-half views.
