# TODO

## Bugs

- [ ] Make sure Undo (Ctrl+Z) doesn't go back to prior opened files (partially fixed, still goes back one file sometimes due to react CodeMirror component not always being re-created when switching files)
- [ ] If opening the drawer results in the editor needing a horizontal scrollbar, it's not shown until the editor component is redrawn (for example by toggling wrap lines)
- [ ] Expander arrows on file-list ListItems don't update color when switching between light and dark theme
- [ ] Trying to change dat.json file results in ProtectedFileNotWritableError
- [ ] When wrap lines is active and the settings drawer is opened, the CodeMirror component doesn't update correctly, have to try if force updating it helps

## Future updates

- [ ] Show picture instead of opening it in editor
- [ ] Show video instead of opening it in editor
- [ ] Add a fork button so you can fork read-only archives to edit the files
- [ ] Add diff to see what changed in files, either before publishing or with a new diff button
- [ ] Add revert button to discard all non-published changes
- [ ] Add button that lets the user revert unsaved changes, which would just load the file state from the DatArchive.
- [ ] Add button to save all unsaved files
- [ ] Find out if there is a better replacement for react-codemirror2, had to use very ugly workarounds to make opening multiple files and font-size setting work
- [ ] Could add a file watcher so that the app notices edits made with other tools, not really necessary but if it's easy to add it's nice to have
- [ ] Display name of opened Dat archive and selected file somewhere where it isn't annoying, maybe at the bottom with a setting to hide it
- [ ] Replace read-only text in toolbar with icon to use up less space
- [ ] Add ability to rename files/folders
- [ ] Add delete file/folder action
- [ ] Refresh file sizes after saving a file (could just do .readdir again)
