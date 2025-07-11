package com.music.ui;

import com.music.arrange.Harmonizer;
import com.music.domain.Cadence;
import com.music.domain.Note;
import com.music.registry.CadenceRegistry;
import com.music.service.JavaxMidiPlayer;
import com.music.service.ScoreRenderer;
import com.music.transform.Transformer;
import com.music.transform.impl.TransposeToTonicTransformer;
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
import java.util.List;            // <— import java.util.List explicitly
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
    private final JButton btnApply, btnPlay, btnExport, btnReset;
    private final JPanel[] numPanels = new JPanel[4], notePanels = new JPanel[4];
    private final JTextArea descArea;
    private final JEditorPane htmlPane;
    private Cadence lastCadence;
    private Thread playThread;

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
    }};

    public MainApp() throws Exception {
        super("Music Cadence Generator");

        // --- Icon, Taskbar ---
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

        // --- Tempo selector ---
        cbTempo = new JComboBox<>();
        for (int i = 30; i <= 200; i++) cbTempo.addItem(i);
        cbTempo.setSelectedItem(60);

        // --- Buttons & Previews ---
        btnApply  = new JButton("Apply");
        btnPlay   = new JButton("Play MIDI");
        btnExport = new JButton("Export XML");
        btnReset  = new JButton("Reset");
        btnPlay.setEnabled(false);
        btnExport.setEnabled(false);

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
            System.out.println("MAINAPP ► raw offsets = " +
                Arrays.deepToString(raw.intervals()));

            // 2) Semitone shift
            Cadence semis = new TransposeToTonicTransformer(tonic)
                                 .transform(raw);
            System.out.println("MAINAPP ► semitone grid = " +
                Arrays.deepToString(semis.intervals()));

            // 3) MIDI anchoring
            Cadence midiCad = new Harmonizer().transform(semis);
            System.out.println("MAINAPP ► MIDI grid = " +
                Arrays.deepToString(midiCad.intervals()));

            // 4) Chain outros transforms
            Cadence c1 = transformers.get(cbT1.getSelectedItem())
                                     .transform(midiCad);
            Cadence c2 = transformers.get(cbT2.getSelectedItem())
                                     .transform(c1);
            Cadence c3 = transformers.get(cbT3.getSelectedItem())
                                     .transform(c2);
            lastCadence = c3;

            System.out.println("MAINAPP ► after custom transforms:");
            System.out.println("   c1 = " + Arrays.deepToString(c1.intervals()));
            System.out.println("   c2 = " + Arrays.deepToString(c2.intervals()));
            System.out.println("   c3 = " + Arrays.deepToString(c3.intervals()));

            // 5) Spelled notes via computeMatrix
            Note[][] spelled = KeySignatureHelper.computeMatrix(
                midiCad.intervals(), tonic);
            System.out.println("MAINAPP ► spelled (midiCad):");
            for (int i = 0; i < spelled.length; i++) {
                System.out.println("   chord " + (i+1) + " = " +
                    Arrays.toString(spelled[i]));
            }

            showGrid(notePanels[0],
                KeySignatureHelper.computeMatrix(midiCad.intervals(), tonic));
            showGrid(notePanels[1],
                KeySignatureHelper.computeMatrix(c1.intervals(),    tonic));
            showGrid(notePanels[2],
                KeySignatureHelper.computeMatrix(c2.intervals(),    tonic));
            showGrid(notePanels[3],
                KeySignatureHelper.computeMatrix(c3.intervals(),    tonic));

            descArea.setText(
              "1) " + cbT1.getSelectedItem() + ": " + c1.description() + "\n" +
              "2) " + cbT2.getSelectedItem() + ": " + c2.description() + "\n" +
              "3) " + cbT3.getSelectedItem() + ": " + c3.description()
            );

            htmlPane.setText(ScoreRenderer.render(c3, tonic));
            btnPlay .setEnabled(true);
            btnExport.setEnabled(true);
        });

        btnPlay.addActionListener(e -> {
            Instrument ins = (Instrument) cbInstr.getSelectedItem();
            Patch p        = ins.getPatch();
            int bank       = p.getBank(), prog = p.getProgram();
            int bpm        = (Integer) cbTempo.getSelectedItem();

            new Thread(() -> {
                try {
                    // play absolute MIDI grid only
                    JavaxMidiPlayer.play(lastCadence, synth, bank, prog, bpm);
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
                    String tonic = (String) cbTonic.getSelectedItem();
                    int bpm      = (Integer) cbTempo.getSelectedItem();
                    w.write(ScoreRenderer.toMusicXML(lastCadence, tonic, bpm));
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this,
                        "Error saving file:\n" + ex.getMessage(),
                        "Save Failed",
                        JOptionPane.ERROR_MESSAGE);
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
            btnPlay .setEnabled(false);
            btnExport.setEnabled(false);
            descArea.setText("");
            htmlPane.setText("");
            for (JPanel p : numPanels)  clearGrid(p);
            for (JPanel p : notePanels) clearGrid(p);
        });

        // --- Layout panels ---
        JPanel controls1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        controls1.add(new JLabel("Cadence:"));    controls1.add(cbCadence);
        controls1.add(new JLabel("Tonic:"));      controls1.add(cbTonic);
        controls1.add(new JLabel("Instrument:")); controls1.add(cbInstr);
        controls1.add(new JLabel("Tempo:"));      controls1.add(cbTempo);

        JPanel controls2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        controls2.add(new JLabel("T1:")); controls2.add(cbT1);
        controls2.add(new JLabel("T2:")); controls2.add(cbT2);
        controls2.add(new JLabel("T3:")); controls2.add(cbT3);
        controls2.add(btnApply); controls2.add(btnPlay);
        controls2.add(btnExport);controls2.add(btnReset);

        JPanel matrices = new JPanel();
        matrices.setLayout(new BoxLayout(matrices, BoxLayout.X_AXIS));
        matrices.setBorder(new EmptyBorder(10, 10, 10, 10));
        for (int i = 0; i < 4; i++) {
            JPanel col = new JPanel();
            col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
            col.add(new JLabel(i == 0 ? "Num0" : "Num→T" + i));
            col.add(numPanels[i]);
            col.add(Box.createVerticalStrut(5));
            col.add(new JLabel(i == 0 ? "Note0" : "Note→T" + i));
            col.add(notePanels[i]);
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

        setContentPane(root); // ⬅️ This line is CRITICAL to display the GUI
        setSize(1400, 850);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    private void showGrid(JPanel panel, int[][] mat) {
        panel.removeAll();
        panel.setLayout(new GridLayout(mat.length, mat[0].length, 4, 4));
        for (int[] row : mat) for (int x : row) {
            JLabel lbl = new JLabel(String.valueOf(x),
                                    SwingConstants.CENTER);
            lbl.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            panel.add(lbl);
        }
        panel.revalidate(); panel.repaint();
    }

    private void showGrid(JPanel panel, Note[][] mat) {
        panel.removeAll();
        panel.setLayout(new GridLayout(mat.length, mat[0].length, 4, 4));
        for (Note[] row : mat) for (Note n : row) {
            JLabel lbl = new JLabel(n.toString(),
                                    SwingConstants.CENTER);
            lbl.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            panel.add(lbl);
        }
        panel.revalidate(); panel.repaint();
    }

    private void clearGrid(JPanel p) {
        p.removeAll();
        p.revalidate();
        p.repaint();
    }

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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { new MainApp().setVisible(true); }
            catch (Exception e) { e.printStackTrace(); }
        });
    }
}
