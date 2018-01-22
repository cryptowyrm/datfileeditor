# TODO

## Bugs

- [ ] Make sure Undo (Ctrl+Z) doesn't go back to prior opened files (partially fixed, still goes back one file sometimes due to react CodeMirror component not always being re-created when switching files)
- [ ] If opening the drawer results in the editor needing a horizontal scrollbar, it's not shown until the editor component is redrawn (for example by toggling wrap lines)
- [ ] Expander arrows on file-list ListItems don't update color when switching between light and dark theme

## Future updates

- [ ] Show picture instead of opening it in editor
- [ ] Show video instead of opening it in editor
- [ ] Add a fork button so you can fork read-only archives to edit the files
- [ ] Add diff to see what changed in files, either before publishing or with a new diff button
- [ ] Add revert button to discard all non-published changes
- [ ] Add button that lets the user revert unsaved changes, which would just load the file state from the DatArchive.
- [ ] Add button to save all unsaved files
- [ ] Find out if there is a better replacement for react-codemirror2, had to use very ugly workarounds to make opening multiple files and font-size setting work
