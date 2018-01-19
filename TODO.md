# TODO

## Before releasing the first version

- [x] Implement publishing changed files (the button doesn't do anything yet)
- [x] Disable Save button after saving
- [x] Show folders as expandable list items (right now the list just displays all files and folders without any tree structure)
- [x] Handle subdirectories in file list
- [ ] Configure CodeMirror syntax highlighting based on the selected file's extension (right now JavaScript is hardcoded so it's used for every opened file)
- [ ] The app opens a test Dat archive by default, could instead show some info about the app and only show a Dat archive once the user has opened one with the "Open Dat archive" button
- [ ] Make sure only text files are opened in the editor, not binary files
- [x] Make sure things like save button is disabled for read-only archives and show somewhere in the UI if an archive is writable or read-only
- [x] Scroll editor to top when switching files (retains old scroll position right now)
- [x] Scroll file list to top when switching Dat archives
- [ ] Make sure back button doesn't go back to prior opened files (partially fixed, still goes back one file sometimes due to react CodeMirror component not always being re-created when switching files)

## Future updates

- [ ] For pictures and videos, show them instead of the editor when opened
- [ ] Add a fork button so you can fork read-only archives to edit the files
