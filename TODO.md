# TODO

## Bugs

- [ ] Make sure Undo (Ctrl+Z) doesn't go back to prior opened files (partially fixed, still goes back one file sometimes due to react CodeMirror component not always being re-created when switching files)

## Future updates

- [ ] Show picture instead of opening it in editor
- [ ] Show video instead of opening it in editor
- [ ] Add a fork button so you can fork read-only archives to edit the files
- [ ] Add diff to see what changed in files, either before publishing or with a new diff button
- [ ] Add revert button to discard all non-published changes
- [ ] Add dark theme
- [ ] Add font size setting
- [ ] Retain changes to files when opening other files - To do this, app-state would get a new key :file-changes which is a map with a filename as key and the current value of the file's contents as the value. This would be used instead of the :selected-file-edited key in the app-state and would be checked when opening a file and if the key exists, used instead of reading the file from the DatArchive. File's that are in the map could then be displayed with either a little icon on the right or maybe blue text for the filename to indicate there are unsaved changes. There should also be a button that lets the user revert changes, which would just load the file state from the DatArchive and ignore the :file-changes map.
