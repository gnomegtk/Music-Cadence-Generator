package com.music.ui;

import com.music.arrange.Harmonizer;
import com.music.domain.Cadence;
import com.music.domain.Note;
import com.music.registry.CadenceRegistry;
import com.music.service.JavaxMidiPlayer;
import com.music.service.ScoreRenderer;
import com.music.transform.Transformer;
import com.music.transform.impl.*;
import com.music.util.KeySignatureHelper;

import javax.sound.midi.Instrument;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Patch;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;

import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Taskbar;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.IOException;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Main Swing application for generating, previewing,
 * playing, and exporting cadences.
 */
public class MainApp extends JFrame {

    private final Synthesizer synth;
    private final JComboBox<String> cbCadence, cbTonic, cbT1, cbT2, cbT3;
    private final JComboBox<Instrument> cbInstr;
    private final JComboBox<Integer> cbTempo;
    private final JButton btnApply, btnPlay, btnExport, btnExportMidi, btnReset;
    private final JCheckBox cbVoiceLeading;
    private final JCheckBox cbDodecafonize;
    private final JPanel[] numPanels = new JPanel[4], notePanels = new JPanel[4];
    private final JTextArea descArea;
    private final JEditorPane htmlPane;
    private Cadence lastCadence;
    private Cadence midiCad;

    private final Map<String, Transformer> transformers = new LinkedHashMap<>() {{
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
        put("Polynomial Derivative", new PolynomialDerivativeTransformer());
        put("Polynomial Integral",   new PolynomialIntegralTransformer());
        // VoiceLeadingOptimizer is NOT included here, applied separately
        put("Modal Interchange",        new ModalInterchangeTransformer());
        put("Chromatic Mediants",       new ChromaticMediantsTransformer());
        put("Secondary Dominants",      new SecondaryDominantsTransformer());
    }};

    public MainApp() throws Exception {
        super("Music Cadence Generator");

        // --- Icon & Taskbar ---
        Image icon = Toolkit.getDefaultToolkit()
                            .getImage(getClass().getResource("/icons/icon.png"));
        setIconImage(icon);
        if (Taskbar.isTaskbarSupported()) {
            try { Taskbar.getTaskbar().setIconImage(icon); }
            catch (UnsupportedOperationException ignore) {}
        }

        // --- Help menu ---
        JMenuBar menuBar = new JMenuBar();
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> showAboutDialog());
        helpMenu.add(aboutItem);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar);

        // --- Synth & SoundFont ---
        synth = MidiSystem.getSynthesizer();
        synth.open();
        boolean sfLoaded = false;
        try (InputStream sf =
                 getClass().getResourceAsStream("/soundfonts/FluidR3_GM.sf2")) {
            Soundbank sb = MidiSystem.getSoundbank(sf);
            synth.loadAllInstruments(sb);
            sfLoaded = true;
        } catch (Exception ignore) {}

        // --- Instrument combo ---
        Instrument[] allIns = sfLoaded
            ? synth.getLoadedInstruments()
            : synth.getDefaultSoundbank().getInstruments();
        List<Instrument> insts = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Instrument ins : allIns) {
            Patch p = ins.getPatch();
            String key = p.getBank() + ":" + p.getProgram();
            if (seen.add(key)) insts.add(ins);
        }
        cbInstr = new JComboBox<>(insts.toArray(new Instrument[0]));
        cbInstr.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l,
                                                          Object v,
                                                          int idx,
                                                          boolean sel,
                                                          boolean focus) {
                super.getListCellRendererComponent(l, v, idx, sel, focus);
                if (v instanceof Instrument) {
                    Instrument i = (Instrument) v;
                    Patch p = i.getPatch();
                    setText(String.format("Bank %03d / Program %03d: %s",
                        p.getBank(), p.getProgram(), i.getName()));
                }
                return this;
            }
        });

        // --- Cadence & tonic selectors ---
        cbCadence = new JComboBox<>(
          CadenceRegistry.getAvailableCadences().toArray(new String[0])
        );
        cbCadence.setSelectedIndex(0);

        cbTonic = new JComboBox<>(new String[]{
          "C","C#","D","Eb","E","F","F#","G","G#","A","Bb","B"
        });
        cbTonic.setSelectedIndex(0);

        // --- Transformer selectors ---
        cbT1 = new JComboBox<>(transformers.keySet().toArray(new String[0]));
        cbT2 = new JComboBox<>(transformers.keySet().toArray(new String[0]));
        cbT3 = new JComboBox<>(transformers.keySet().toArray(new String[0]));
        cbT1.setSelectedItem("Identity");
        cbT2.setSelectedItem("Identity");
        cbT3.setSelectedItem("Identity");

        cbVoiceLeading = new JCheckBox("Optimize Voice Leading");
        cbDodecafonize = new JCheckBox("Dodecafonize (12-tone row)");

        // --- Tempo selector ---
        cbTempo = new JComboBox<>();
        for (int i = 30; i <= 200; i++) cbTempo.addItem(i);
        cbTempo.setSelectedItem(60);

        // --- Buttons & Previews ---
        btnApply  = new JButton("Apply");
        btnPlay   = new JButton("Play MIDI");
        btnExport = new JButton("Export XML");
        btnExportMidi = new JButton("Export MIDI");
        btnReset  = new JButton("Reset");
        btnPlay.setEnabled(false);
        btnExport.setEnabled(false);
        btnExportMidi.setEnabled(false);

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

        // --- Panels for grids ---
        for (int i = 0; i < 4; i++) {
            numPanels[i]  = new JPanel();
            notePanels[i] = new JPanel();
        }

        Runnable disable = () -> {
            btnPlay .setEnabled(false);
            btnExport.setEnabled(false);
            btnExportMidi.setEnabled(false);
        };
        cbCadence.addActionListener(e -> disable.run());
        cbTonic  .addActionListener(e -> disable.run());
        cbT1     .addActionListener(e -> disable.run());
        cbT2     .addActionListener(e -> disable.run());
        cbT3     .addActionListener(e -> disable.run());
        cbInstr  .addActionListener(e -> disable.run());
        cbTempo  .addActionListener(e -> disable.run());

        // --- APPLY action ---
        btnApply.addActionListener(e -> {
            String cadName = (String) cbCadence.getSelectedItem();
            String tonic   = (String) cbTonic.getSelectedItem();

            // 1) Raw offsets
            Cadence raw = CadenceRegistry.getCadence(cadName);

            // 2) Semitone shift
            Cadence semis = new TransposeToTonicTransformer(tonic)
                                 .transform(raw);

            // 3) Chain other transforms
            Cadence c1 = transformers.get(cbT1.getSelectedItem())
                                     .transform(semis);
            Cadence c2 = transformers.get(cbT2.getSelectedItem())
                                     .transform(c1);
            Cadence c3 = transformers.get(cbT3.getSelectedItem())
                                     .transform(c2);

            if (cbVoiceLeading.isSelected()) {
               c3 = new VoiceLeadingOptimizerTransformer().transform(c3);
            }

            if (cbDodecafonize.isSelected()) {
               c3 = new DodecafonizeTransformer().transform(c3);
            }

            
            lastCadence = c3;

            // 5) Numeric grids
            showGrid(numPanels[0], semis.intervals());
            showGrid(numPanels[1], c1.intervals());
            showGrid(numPanels[2], c2.intervals());
            showGrid(numPanels[3], c3.intervals());

            // 6) Spelled-note grids for display
            Note[][] spelled0 = KeySignatureHelper.computeMatrix(
                semis.intervals(), tonic);
            Note[][] spelled1 = KeySignatureHelper.computeMatrix(
                c1.intervals(),    tonic);
            Note[][] spelled2 = KeySignatureHelper.computeMatrix(
                c2.intervals(),    tonic);
            Note[][] spelled3 = KeySignatureHelper.computeMatrix(
                c3.intervals(),    tonic);

            showGrid(notePanels[0], spelled0);
            showGrid(notePanels[1], spelled1);
            showGrid(notePanels[2], spelled2);
            showGrid(notePanels[3], spelled3);

            // 7) Descriptions
            descArea.setText(
              "1) " + cbT1.getSelectedItem() + ": " + c1.description() + "\n" +
              "2) " + cbT2.getSelectedItem() + ": " + c2.description() + "\n" +
              "3) " + cbT3.getSelectedItem() + ": " + c3.description() +
              (cbVoiceLeading.isSelected() ? "\n+ Voice Leading Optimization applied" : "")
            );

            // 8) HTML preview without octave
            htmlPane.setText(buildNoteTableHtml(spelled3));

            // 9) Prepare MIDI playback
            midiCad = new Harmonizer().transform(c3);
            btnPlay .setEnabled(true);
            btnExport.setEnabled(true);
            btnExportMidi.setEnabled(true);
        });

        // --- PLAY action ---
        btnPlay.addActionListener(e -> {
            Instrument ins = (Instrument) cbInstr.getSelectedItem();
            Patch p        = ins.getPatch();
            int bank       = p.getBank(), prog = p.getProgram();
            int bpm        = (Integer) cbTempo.getSelectedItem();

            new Thread(() -> {
                try {
                    JavaxMidiPlayer.play(midiCad, synth, bank, prog, bpm);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        });

        // --- EXPORT XML action ---
        btnExport.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Save MusicXML");
            String filename = lastCadence.type()
                                .replaceAll("[^A-Za-z0-9]", "_")
                                + ".musicxml";
            fc.setSelectedFile(new File(filename));

            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try (FileWriter w = new FileWriter(fc.getSelectedFile())) {
                    int bpm = (Integer) cbTempo.getSelectedItem();
                    System.out.println(">>> Exporting grid: " + Arrays.deepToString(midiCad.intervals()));
                    w.write( ScoreRenderer.toMusicXMLFromMidi(midiCad, bpm) );
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(
                        this,
                        "Error saving file:\n" + ex.getMessage(),
                        "Save Failed",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        });

        // --- EXPORT MIDI action ---
        btnExportMidi.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Save MIDI");
            String filename = lastCadence.type()
                                .replaceAll("[^A-Za-z0-9]", "_")
                                + ".mid";
            fc.setSelectedFile(new File(filename));

            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    int bpm = (Integer) cbTempo.getSelectedItem();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(
                        this,
                        "Error saving MIDI:\n" + ex.getMessage(),
                        "Save Failed",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        });

        // --- RESET action ---
        btnReset.addActionListener(e -> {
            cbCadence.setSelectedIndex(0);
            cbTonic  .setSelectedIndex(0);
            cbT1     .setSelectedItem("Identity");
            cbT2     .setSelectedItem("Identity");
            cbT3     .setSelectedItem("Identity");
            cbTempo  .setSelectedItem(60);
            cbVoiceLeading.setSelected(false);
            cbDodecafonize.setSelected(false);
            btnPlay .setEnabled(false);
            btnExport.setEnabled(false);
            btnExportMidi.setEnabled(false);
            descArea.setText("");
            htmlPane.setText("");
            for (JPanel p : numPanels)  clearGrid(p);
            for (JPanel p : notePanels) clearGrid(p);
        });

        // --- Layout setup ---
        JPanel controls1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        controls1.add(new JLabel("Cadence:"));    controls1.add(cbCadence);
        controls1.add(new JLabel("Tonic:"));      controls1.add(cbTonic);
        controls1.add(new JLabel("Instrument:")); controls1.add(cbInstr);
        controls1.add(new JLabel("Tempo:"));      controls1.add(cbTempo);

        JPanel controls2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        controls2.add(new JLabel("T1:")); controls2.add(cbT1);
        controls2.add(new JLabel("T2:")); controls2.add(cbT2);
        controls2.add(new JLabel("T3:")); controls2.add(cbT3);
        controls2.add(cbVoiceLeading);
        controls2.add(cbDodecafonize);
        controls2.add(btnApply); controls2.add(btnPlay);
        controls2.add(btnExport); controls2.add(btnExportMidi);
        controls2.add(btnReset);

        JPanel matrices = new JPanel();
        matrices.setLayout(new BoxLayout(matrices, BoxLayout.X_AXIS));
        matrices.setBorder(new EmptyBorder(10, 10, 10, 10));
        for (int i = 0; i < 4; i++) {
            JPanel col = new JPanel();
            col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
            col.add(new JLabel(i == 0 ? "Num0" : "Num→T" + i));

            JScrollPane numScroll = new JScrollPane(numPanels[i]);
            numScroll.setPreferredSize(new Dimension(200, 100));
            col.add(numScroll);

            col.add(Box.createVerticalStrut(5));
            col.add(new JLabel(i == 0 ? "Note0" : "Note→T" + i));

            JScrollPane noteScroll = new JScrollPane(notePanels[i]);
            noteScroll.setPreferredSize(new Dimension(200, 100));
            col.add(noteScroll);

            col.setBorder(new EmptyBorder(0, 5, 0, 5));
            matrices.add(col);
        }

        JScrollPane descScroll = new JScrollPane(descArea);
        JScrollPane htmlScroll = new JScrollPane(htmlPane);

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        root.add(controls1);
        root.add(controls2);
        root.add(matrices);
        root.add(descScroll);
        root.add(htmlScroll);

        setContentPane(root);
        setSize(1400, 850);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    /**
     * Display an int[][] grid in the given panel.
     */
    private void showGrid(JPanel panel, int[][] mat) {
        panel.removeAll();
        if (mat.length == 0 || mat[0].length == 0) {
            panel.revalidate();
            panel.repaint();
            return;
        }

        panel.setLayout(new GridLayout(mat.length, mat[0].length, 4, 4));
        for (int[] row : mat) {
            for (int x : row) {
                JLabel lbl = new JLabel(String.valueOf(x), SwingConstants.CENTER);
                lbl.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                panel.add(lbl);
            }
        }

        panel.revalidate();
        panel.repaint();
    }

    /**
     * Display a Note[][] grid in the given panel (step + accidental only).
     */
    private void showGrid(JPanel panel, Note[][] mat) {
        panel.removeAll();
        if (mat.length == 0 || mat[0].length == 0) {
            panel.revalidate();
            panel.repaint();
            return;
        }

        panel.setLayout(new GridLayout(mat.length, mat[0].length, 4, 4));
        for (Note[] row : mat) {
            for (Note n : row) {
                String step = n.step();
                String accidental = n.alter() == 1 ? "♯" : n.alter() == -1 ? "♭" : "";
                JLabel lbl = new JLabel(step + accidental, SwingConstants.CENTER);
                lbl.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                panel.add(lbl);
            }
        }

        panel.revalidate();
        panel.repaint();
    }

    /**
     * Build an HTML table (step + accidental only) for a Note grid.
     */
    private String buildNoteTableHtml(Note[][] mat) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body>\n")
            .append("<table border='1' cellpadding='4' cellspacing='0'>\n");
        for (Note[] row : mat) {
            html.append("  <tr>");
            for (Note n : row) {
                // use getters step() and alter(), omit octave
                String step       = n.step();
                String accidental = n.alter() ==  1 ? "♯"
                                  : n.alter() == -1 ? "♭"
                                  : "";
                html.append("<td>")
                    .append(step)
                    .append(accidental)
                    .append("</td>");
            }
            html.append("</tr>\n");
        }
        html.append("</table>\n")
            .append("</body></html>");
        return html.toString();
    }

    /**
     * Clear a grid panel.
     */
    private void clearGrid(JPanel p) {
        p.removeAll();
        p.revalidate();
        p.repaint();
    }

    /**
     * Show About dialog with author information.
     */
    private void showAboutDialog() {
        String html = "<html><b>Music Cadence Generator</b><br/>" +
                      "Evandro Veloso Gomes<br/>" +
                      "<a href=\"mailto:gnome_gtk2000@yahoo.com.br\">" +
                      "gnome_gtk2000@yahoo.com.br</a></html>";
        JEditorPane pane = new JEditorPane("text/html", html);
        pane.setEditable(false);
        pane.setOpaque(false);
        JOptionPane.showMessageDialog(this, pane,
            "About Music Cadence Generator", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Application entry point.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { new MainApp().setVisible(true); }
            catch (Exception e) { e.printStackTrace(); }
        });
    }
}