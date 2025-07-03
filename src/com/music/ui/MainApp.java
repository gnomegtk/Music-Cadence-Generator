package com.music.ui;

import com.music.domain.Cadence;
import com.music.domain.Note;
import com.music.registry.CadenceRegistry;
import com.music.service.JavaxMidiPlayer;
import com.music.service.ScoreRenderer;
import com.music.transform.Transformer;
import com.music.transform.impl.*;

import javax.sound.midi.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MainApp extends JFrame {
    private final Synthesizer synth;
    private final JComboBox<String> cbCadence, cbTonic, cbT1, cbT2, cbT3;
    private final JComboBox<Instrument> cbInstr;
    private final JComboBox<Integer> cbTempo;
    private final JButton btnApply, btnPlay, btnExport, btnReset;
    private final JPanel[] numPanels = new JPanel[4], notePanels = new JPanel[4];
    private final JTextArea descArea;
    private final JEditorPane htmlPane;
    private Cadence lastCadence;
    private Thread playThread;

    private final java.util.Map<String, Transformer> transformers = new java.util.LinkedHashMap<>() {{
        put("Identity",            new IdentityTransformer());
        put("Add Ninth to …",      new AddNinthTransformer());
        put("Add Seventh to …",    new AddSeventhTransformer());
        put("Augmentation ×2",     new AugmentationTransformer());
        put("Diminution ÷2",       new DiminutionTransformer());
        put("Cycle",               new CycleTransformer());
        put("Duplicate Chords",    new DuplicateChordTransformer());
        put("Extend Progression",  new ExtendProgressionTransformer());
        put("Inversion",           new InverterTransformer());
        put("Major→Minor",         new MajorToMinorTransformer());
        put("Negation",            new NegationTransformer());
        put("Reciprocal (12–x)",   new ReciprocalTransformer());
        put("Retrograde",          new RetrogradeTransformer());
        put("Transpose +2",        new TransposeTransformer());
        put("Transpose Rows⇄Cols", new TransposeMatrixTransformer());
    }};

    private static Note[][] computeNoteMatrix(Cadence c) {
        int[][] iv = c.intervals();
        int tonic = c.getTonicMidi();
        Note[][] out = new Note[iv.length][];
        for (int i = 0; i < iv.length; i++) {
            out[i] = new Note[iv[i].length];
            for (int j = 0; j < iv[i].length; j++) {
                out[i][j] = midiToNote(tonic + iv[i][j]);
            }
        }
        return out;
    }

    private static Note midiToNote(int midi) {
        int pc = midi % 12;
        int oct = midi / 12 - 1;
        switch (pc) {
            case 0:  return new Note("C",  0, oct);
            case 1:  return new Note("C",  1, oct);
            case 2:  return new Note("D",  0, oct);
            case 3:  return new Note("D",  1, oct);
            case 4:  return new Note("E",  0, oct);
            case 5:  return new Note("F",  0, oct);
            case 6:  return new Note("F",  1, oct);
            case 7:  return new Note("G",  0, oct);
            case 8:  return new Note("G",  1, oct);
            case 9:  return new Note("A",  0, oct);
            case 10: return new Note("A",  1, oct);
            case 11: return new Note("B",  0, oct);
            default: return new Note("C",  0, oct);
        }
    }

    public MainApp() throws Exception {
        super("Music Cadence Generator");

        // Load icon
        Image iconImage = Toolkit.getDefaultToolkit()
                                 .getImage(getClass().getResource("/icons/icon.png"));
        setIconImage(iconImage);
        if (Taskbar.isTaskbarSupported()) {
            try { Taskbar.getTaskbar().setIconImage(iconImage); }
            catch (UnsupportedOperationException ignored) {}
        }

        // Window
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1400, 850);
        setLocationRelativeTo(null);

        // Help menu
        JMenuBar menuBar = new JMenuBar();
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> showAboutDialog(iconImage));
        helpMenu.add(aboutItem);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar);

        // Synth & SoundFont
        synth = MidiSystem.getSynthesizer();
        synth.open();
        boolean sfLoaded = false;
        try (InputStream sf = getClass().getResourceAsStream(
                 "/soundfonts/FluidR3_GM.sf2")) {
            Soundbank sb = MidiSystem.getSoundbank(sf);
            synth.loadAllInstruments(sb);
            sfLoaded = true;
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
              "Warning: Could not load SoundFont. Using default synth.",
              "SoundFont Error", JOptionPane.WARNING_MESSAGE);
        }

        // Instrument combo (bank+program, no duplicates)
        Instrument[] allIns = sfLoaded
            ? synth.getLoadedInstruments()
            : synth.getDefaultSoundbank().getInstruments();

        Set<String> seen = new HashSet<>();
        java.util.List<Instrument> filtered = new ArrayList<>();
        for (Instrument ins : allIns) {
            Patch p = ins.getPatch();
            String key = p.getBank()+":"+p.getProgram();
            if (seen.add(key)) filtered.add(ins);
        }

        cbInstr = new JComboBox<>(filtered.toArray(new Instrument[0]));
        cbInstr.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list,
                                                          Object value,
                                                          int index,
                                                          boolean isSelected,
                                                          boolean cellHasFocus) {
                super.getListCellRendererComponent(
                  list, value, index, isSelected, cellHasFocus);
                if (value instanceof Instrument) {
                    Instrument ins = (Instrument) value;
                    Patch p = ins.getPatch();
                    setText(String.format(
                      "Bank %03d / Program %03d: %s",
                      p.getBank(), p.getProgram(), ins.getName()));
                }
                return this;
            }
        });

        // Cadence & transformer combos
        cbCadence = new JComboBox<>(
          CadenceRegistry.getAvailableCadences().toArray(new String[0]));
        cbCadence.setSelectedIndex(0);

        cbTonic = new JComboBox<>(new String[]{
            "C","C#","D","Eb","E","F","F#","G","G#","A","Bb","B"
        });
        cbTonic.setSelectedIndex(0);

        cbT1 = new JComboBox<>(
          transformers.keySet().toArray(new String[0]));
        cbT2 = new JComboBox<>(
          transformers.keySet().toArray(new String[0]));
        cbT3 = new JComboBox<>(
          transformers.keySet().toArray(new String[0]));
        cbT1.setSelectedItem("Identity");
        cbT2.setSelectedItem("Identity");
        cbT3.setSelectedItem("Identity");

        // Tempo 30–200 BPM step=1
        java.util.List<Integer> bpmList = new ArrayList<>();
        for (int i = 30; i <= 200; i++) bpmList.add(i);
        cbTempo = new JComboBox<>(bpmList.toArray(new Integer[0]));
        cbTempo.setSelectedItem(60);

        // Buttons
        btnApply  = new JButton("Apply");
        btnPlay   = new JButton("Play MIDI");
        btnExport = new JButton("Export XML");
        btnReset  = new JButton("Reset");
        btnPlay.setEnabled(false);
        btnExport.setEnabled(false);

        // Grids & preview
        descArea = new JTextArea(6, 80);
        descArea.setEditable(false);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);

        htmlPane = new JEditorPane("text/html", "");
        htmlPane.setEditable(false);
        htmlPane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                try { Desktop.getDesktop().browse(e.getURL().toURI()); }
                catch (Exception ex) { ex.printStackTrace(); }
            }
        });

        for (int i = 0; i < 4; i++) {
            numPanels[i]  = new JPanel();
            notePanels[i] = new JPanel();
        }

        // Disable play/export on any change
        Runnable disableButtons = () -> {
            btnPlay.setEnabled(false);
            btnExport.setEnabled(false);
        };
        cbCadence.addActionListener(e -> disableButtons.run());
        cbTonic  .addActionListener(e -> disableButtons.run());
        cbT1     .addActionListener(e -> disableButtons.run());
        cbT2     .addActionListener(e -> disableButtons.run());
        cbT3     .addActionListener(e -> disableButtons.run());
        cbInstr  .addActionListener(e -> disableButtons.run());
        cbTempo  .addActionListener(e -> disableButtons.run());

        // Apply button
        btnApply.addActionListener(e -> {
            String cadName = (String) cbCadence.getSelectedItem();
            String tonic   = (String) cbTonic.getSelectedItem();

            Cadence raw     = CadenceRegistry.getCadence(cadName, "C");
            Cadence aligned = new TransposeToTonicTransformer().transform(raw, tonic);

            Cadence c1 = transformers.get(cbT1.getSelectedItem()).transform(aligned, tonic);
            Cadence c2 = transformers.get(cbT2.getSelectedItem()).transform(c1, tonic);
            Cadence c3 = transformers.get(cbT3.getSelectedItem()).transform(c2, tonic);

            lastCadence = c3;

            showGrid(numPanels[0], aligned.intervals());
            showGrid(numPanels[1], c1.intervals());
            showGrid(numPanels[2], c2.intervals());
            showGrid(numPanels[3], c3.intervals());

            showGrid(notePanels[0], computeNoteMatrix(aligned));
            showGrid(notePanels[1], computeNoteMatrix(c1));
            showGrid(notePanels[2], computeNoteMatrix(c2));
            showGrid(notePanels[3], computeNoteMatrix(c3));

            descArea.setText(
              "1) " + cbT1.getSelectedItem() + ": " + c1.description() + "\n" +
              "2) " + cbT2.getSelectedItem() + ": " + c2.description() + "\n" +
              "3) " + cbT3.getSelectedItem() + ": " + c3.description()
            );
            htmlPane.setText(ScoreRenderer.render(c3));

            btnPlay.setEnabled(true);
            btnExport.setEnabled(true);
        });

        // Play MIDI button
        btnPlay.addActionListener(e -> {
            if (lastCadence == null) return;
            if (playThread != null && playThread.isAlive())
                playThread.interrupt();

            Instrument ins = (Instrument) cbInstr.getSelectedItem();
            Patch patch    = ins.getPatch();
            int bank       = patch.getBank();
            int prog       = patch.getProgram();
            int bpm        = (Integer) cbTempo.getSelectedItem();

            playThread = new Thread(() ->
                JavaxMidiPlayer.play(lastCadence, synth, bank, prog, bpm)
            );
            playThread.start();
        });

        // Export XML button
        btnExport.addActionListener(e -> {
            if (lastCadence == null) return;
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Save MusicXML");
            fc.setSelectedFile(new File(
              lastCadence.type().replaceAll("[^A-Za-z0-9]", "_")
              + ".musicxml"
            ));
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try (FileWriter w = new FileWriter(fc.getSelectedFile())) {
                    int bpm = (Integer) cbTempo.getSelectedItem();
                    w.write(ScoreRenderer.toMusicXML(lastCadence, bpm));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this,
                      ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Reset button
        btnReset.addActionListener(e -> {
            cbCadence.setSelectedIndex(0);
            cbTonic  .setSelectedIndex(0);
            cbT1     .setSelectedItem("Identity");
            cbT2     .setSelectedItem("Identity");
            cbT3     .setSelectedItem("Identity");
            cbInstr  .setSelectedIndex(0);
            cbTempo  .setSelectedItem(60);
            btnPlay  .setEnabled(false);
            btnExport.setEnabled(false);
            descArea .setText("");
            htmlPane .setText("");
            for (JPanel p : numPanels)  clearGrid(p);
            for (JPanel p : notePanels) clearGrid(p);
        });

        // Layout...
        JPanel controls1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        controls1.add(new JLabel("Cadence:"));     controls1.add(cbCadence);
        controls1.add(new JLabel("Tonic:"));       controls1.add(cbTonic);
        controls1.add(new JLabel("Instrument:"));  controls1.add(cbInstr);
        controls1.add(new JLabel("Tempo (BPM):")); controls1.add(cbTempo);

        JPanel controls2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        controls2.add(new JLabel("T1:")); controls2.add(cbT1);
        controls2.add(new JLabel("T2:")); controls2.add(cbT2);
        controls2.add(new JLabel("T3:")); controls2.add(cbT3);
        controls2.add(btnApply); controls2.add(btnPlay);
        controls2.add(btnExport);controls2.add(btnReset);

        JPanel matrices = new JPanel();
        matrices.setLayout(new BoxLayout(matrices, BoxLayout.X_AXIS));
        matrices.setBorder(new EmptyBorder(10,10,10,10));
        for (int i = 0; i < 4; i++) {
            JPanel col = new JPanel();
            col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
            String numTitle  = (i == 0 ? "Num0" : "Num→T"+i);
            String noteTitle = (i == 0 ? "Note0": "Note→T"+i);
            col.add(new JLabel(numTitle)); col.add(numPanels[i]);
            col.add(Box.createVerticalStrut(5));
            col.add(new JLabel(noteTitle));col.add(notePanels[i]);
            col.setBorder(new EmptyBorder(0,5,0,5));
            matrices.add(col);
        }

        JScrollPane descScroll = new JScrollPane(descArea);
        JScrollPane htmlScroll = new JScrollPane(htmlPane);

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(new EmptyBorder(10,10,10,10));
        root.add(controls1);
        root.add(controls2);
        root.add(matrices);
        root.add(descScroll);
        root.add(htmlScroll);

        setContentPane(root);
    }

    private void showAboutDialog(Image icon) {
        String html = "<html><b>Music Cadence Generator</b><br/>" +
                      "Evandro Veloso Gomes<br/>" +
                      "<a href=\"mailto:gnome_gtk2000@yahoo.com.br\">" +
                      "gnome_gtk2000@yahoo.com.br</a></html>";
        JEditorPane p = new JEditorPane("text/html", html);
        p.setEditable(false);
        p.setOpaque(false);
        Image scaled = icon.getScaledInstance(48,48,Image.SCALE_SMOOTH);
        ImageIcon aboutIcon = new ImageIcon(scaled);
        JOptionPane.showMessageDialog(this, p,
            "About Music Cadence Generator",
            JOptionPane.INFORMATION_MESSAGE, aboutIcon);
    }

    private void showGrid(JPanel panel, int[][] mat) {
        panel.removeAll();
        panel.setLayout(new GridLayout(mat.length, mat[0].length, 4, 4));
        for (int[] row : mat) {
            for (int x : row) {
                JLabel lbl = new JLabel(String.valueOf(x), SwingConstants.CENTER);
                lbl.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                panel.add(lbl);
            }
        }
        panel.revalidate(); panel.repaint();
    }

    private void showGrid(JPanel panel, Note[][] mat) {
        panel.removeAll();
        panel.setLayout(new GridLayout(mat.length, mat[0].length, 4, 4));
        for (Note[] row : mat) {
            for (Note n : row) {
                JLabel lbl = new JLabel(n.toString(), SwingConstants.CENTER);
                lbl.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                panel.add(lbl);
            }
        }
        panel.revalidate(); panel.repaint();
    }

    private void clearGrid(JPanel p) {
        p.removeAll(); p.revalidate(); p.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new MainApp().setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
