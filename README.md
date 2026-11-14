# Voice Keyboard

A keyboard for slow typers: speak in any language and it types for you, then rewrites unclear speech (stutters, repeats, fillers, misheard words) into clear text.

## Files

- `index.html` — the whole page (markup, styles and script in one file).

## Where it runs

The page is built as a Claude artifact. Listening and the "Fix my words" step use the artifact's `dictation` and `sample` capabilities, so they only work when the page is opened as a published artifact on claude.ai (Chrome or the Claude desktop app). Opened anywhere else, such as GitHub Pages or a local file, the on-screen keys, Copy, Clear and Undo still work, but the microphone and fixing buttons stay hidden.
