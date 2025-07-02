# Music Cadence Generator

**Music Cadence Generator** is a creative tool for harmonic exploration, composition, and music theory analysis. It allows users to select a cadence (a harmonic progression), apply transformational operations to it, visualize the resulting changes, and render them audibly and visually via MIDI and MusicXML.

At its core, this software bridges musical intuition and computational structure — enabling musicians, students, and theorists to investigate how harmony can be reimagined through systematic transformation.

---

## 🎵 Motivation

In music theory, cadences represent structural endpoints or punctuation marks in a composition. By transforming a cadence, we’re not merely changing chords — we’re probing the underlying logic of tonal progression.

**Music Cadence Generator** was designed to answer questions like:

- What if I inverted the cadence harmonically?
- What would a retrograde or transposed version of this sound like?
- How do interval structures and voice-leading behave under transformation?

This tool becomes a laboratory for musical possibility — illuminating concepts from both classical harmony and algorithmic composition.

---

## 🛠️ Features

- 🎼 **Cadence Library**: Choose from a list of predefined cadences rooted in tonal tradition
- ⚙️ **Transformations**: Chain up to three sequential transformations like:
  - Retrograde
  - Inversion
  - Transposition
  - Cycle
  - Augmentation and more
- 🧊 **Matrix Visualization**: View interval and note matrices before and after each transformation
- 🔊 **Real-Time Playback**: Hear the result using Java’s MIDI synthesizer and a loaded SoundFont
- 📝 **MusicXML Export**: Save transformed cadences with accurate rhythm and voicing
- 🎛️ **Custom Instrument & Tempo**: Select your preferred timbre and BPM
- 🎨 **Elegant Interface**: Java Swing UI with custom icon, macOS Dock integration and About dialog

---

## 🧾 TODO

This section collects ideas, challenges, and next steps discovered during development. It includes features that were attempted but not yet successful, and possible future enhancements for the Music Cadence Generator:

### 🎹 MIDI & Instrument Handling

- [ ] Allow integration with external synthesizers (e.g., FluidSynth, VSTs).

### 🖥 UI & Usability

- [ ] Embed a preview of the generated score inside the interface (SVG or raster image from MusicXML).
- [ ] Let users tweak individual voices after transformation (e.g. in matrix or piano roll view).

### 🔧 Transformations & Music Logic

- [ ] Support voicing models like open, close, drop-2.
- [ ] Implement better voice leading — e.g. minimize leaps, prefer contrary motion.
- [ ] Allow selective transformation (e.g., apply inversion to soprano only).

### 🎼 Output Formats

- [ ] Add `.wav` or `.mp3` export (via Java Audio or external renderer).
- [ ] Export matrix and score visualizations as images (possibly via LilyPond or MuseScore CLI).
- [ ] Add `.mid` export with embedded instrument and tempo metadata.

---

This TODO list is your roadmap — or at least a well-marked detour sign — in case you pick the project up again later. 📍

---

## 🚀 Quick Start

To build and run:

```bash
make jar
java -jar music-cadence-generator.jar

