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
import java.util.*;
import java.util.List;

/**
 * MainApp (Swing UI) for Music Cadence Generator.
 * Handles cadence selection, transformations, preview, MIDI playback, and MusicXML export.
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
        put("Add Ninth to …",      new AddNinthTransformer());
        put("Add Seventh to …",    new AddSeventhTransformer());
        put("Augmentation ×2",     new AugmentationTransformer());
        put("Cycle",               new CycleTransformer());
        put("Diminution ÷2",       new DiminutionTransformer());
        put("Duplicate Chords",    new DuplicateChordTransformer());
        put("Extend Progression",  new ExtendProgressionTransformer());
        put("Identity",            new IdentityTransformer());
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

        // Load and set application icon
        Image iconImage = Toolkit.getDefaultToolkit()
                                 .getImage(getClass().getResource("/icons/icon.png"));
        setIconImage(iconImage);
        if (Taskbar.isTaskbarSupported()) {
            try { Taskbar.getTaskbar().setIconImage(iconImage); }
            catch (UnsupportedOperationException ignored) {}
        }

        // Window setup
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1400, 850);
        setLocationRelativeTo(null);

        // Build menu with About
        JMenuBar menuBar = new JMenuBar();
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> showAboutDialog(iconImage));
        helpMenu.add(aboutItem);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar);

        // MIDI setup with GM SoundFont
        synth = MidiSystem.getSynthesizer();
        synth.open();
        boolean sfLoaded = false;
        try (InputStream sf = getClass().getResourceAsStream("/soundfonts/FluidR3_GM.sf2")) {
            Soundbank sb = MidiSystem.getSoundbank(sf);
            synth.loadAllInstruments(sb);
            sfLoaded = true;
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Could not load SoundFont:\n" + ex.getMessage() + "\nUsing default synth.",
                "Warning", JOptionPane.WARNING_MESSAGE);
        }

        // Instrument list
	Instrument[] allIns = synth.getAvailableInstruments();
        Set<String> seen = new HashSet<>();
        List<Instrument> filtered = new ArrayList<>();
        for (Instrument ins : allIns) {
            Patch patch = ins.getPatch();
            String key = patch.getBank() + ":" + patch.getProgram();
            if (seen.add(key)) {
                filtered.add(ins);
            }
        }

        cbInstr = new JComboBox<>(filtered.toArray(new Instrument[0]));
        cbInstr.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Instrument) {
                    Instrument ins = (Instrument) value;
                    Patch patch = ins.getPatch();
                    setText(String.format("Bank %03d / Program %03d: %s",
                        patch.getBank(), patch.getProgram(), ins.getName()));
                }
                return this;
            }
        });

        // UI controls
        cbCadence = new JComboBox<>(CadenceRegistry.getAvailableCadences().toArray(new String[0]));
        cbCadence.setSelectedIndex(0);

        cbTonic = new JComboBox<>(new String[]{
            "C","C#","D","D#","E","F","F#","G","G#","A","A#","B"
        });
        cbTonic.setSelectedIndex(0);

        cbT1 = new JComboBox<>(transformers.keySet().toArray(new String[0]));
        cbT2 = new JComboBox<>(transformers.keySet().toArray(new String[0]));
        cbT3 = new JComboBox<>(transformers.keySet().toArray(new String[0]));
        cbT1.setSelectedItem("Identity");
        cbT2.setSelectedItem("Identity");
        cbT3.setSelectedItem("Identity");
        cbInstr.setSelectedIndex(0);

        List<Integer> bpmList = new ArrayList<>();
        for (int i = 30; i <= 200; i++) bpmList.add(i);
        cbTempo = new JComboBox<>(bpmList.toArray(new Integer[0]));
        cbTempo.setSelectedItem(60);

        btnApply  = new JButton("Apply");
        btnPlay   = new JButton("Play MIDI");  btnPlay.setEnabled(false);
        btnExport = new JButton("Export XML"); btnExport.setEnabled(false);
        btnReset  = new JButton("Reset");

        // Grid panels
        for (int i = 0; i < 4; i++) {
            numPanels[i]  = new JPanel();
            notePanels[i] = new JPanel();
        }

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

        // Disable Play/Export on changes except tempo
        Runnable disable = () -> {
            btnPlay.setEnabled(false);
            btnExport.setEnabled(false);
        };
        cbCadence.addActionListener(e -> disable.run());
        cbTonic  .addActionListener(e -> disable.run());
        cbT1     .addActionListener(e -> disable.run());
        cbT2     .addActionListener(e -> disable.run());
        cbT3     .addActionListener(e -> disable.run());

        // — Apply button —
        btnApply.addActionListener(e -> {
            String cadName = (String) cbCadence.getSelectedItem();
            String tonic   = (String) cbTonic.getSelectedItem();
            Cadence s0 = CadenceRegistry.getCadence(cadName, tonic);
            Cadence s1 = transformers.get(cbT1.getSelectedItem()).transform(s0, tonic);
            Cadence s2 = transformers.get(cbT2.getSelectedItem()).transform(s1, tonic);
            Cadence s3 = transformers.get(cbT3.getSelectedItem()).transform(s2, tonic);
            lastCadence = s3;

            showGrid(numPanels[0], s0.intervals());
            showGrid(numPanels[1], s1.intervals());
            showGrid(numPanels[2], s2.intervals());
            showGrid(numPanels[3], s3.intervals());

            showGrid(notePanels[0], s0.matrix());
            showGrid(notePanels[1], s1.matrix());
            showGrid(notePanels[2], s2.matrix());
            showGrid(notePanels[3], s3.matrix());

            descArea.setText(
                "1) " + cbT1.getSelectedItem() + ": " + s1.description() + "\n" +
                "2) " + cbT2.getSelectedItem() + ": " + s2.description() + "\n" +
                "3) " + cbT3.getSelectedItem() + ": " + s3.description()
            );

            htmlPane.setText(ScoreRenderer.render(s3));
            btnPlay.setEnabled(true);
            btnExport.setEnabled(true);
        });

        // — Play button —
        btnPlay.addActionListener(e -> {
            if (lastCadence == null) return;
            if (playThread != null && playThread.isAlive()) playThread.interrupt();

            Instrument ins = (Instrument) cbInstr.getSelectedItem();
            Patch patch = ins.getPatch();
            int bank = patch.getBank();
            int prog = patch.getProgram();
            int bpm  = (Integer) cbTempo.getSelectedItem();

            playThread = new Thread(() -> {
                try {
                    // Define canal 0 com banco e programa corretos
                    MidiChannel[] channels = synth.getChannels();
                    channels[0].programChange(bank, prog);

                    JavaxMidiPlayer.play(lastCadence, synth, prog, bpm);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            playThread.start();
        });

        // — Export button — (modified to pass BPM)
        btnExport.addActionListener(e -> {
            if (lastCadence == null) return;
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Save MusicXML");
            fc.setSelectedFile(new File(
                lastCadence.type().replaceAll("[^A-Za-z0-9]", "_") + ".musicxml"
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

        // — Reset button —
        btnReset.addActionListener(e -> {
            cbCadence.setSelectedIndex(0);
            cbTonic.setSelectedIndex(0);
            cbT1.setSelectedItem("Identity");
            cbT2.setSelectedItem("Identity");
            cbT3.setSelectedItem("Identity");
            cbInstr.setSelectedIndex(0);
            cbTempo.setSelectedItem(60);
            btnPlay.setEnabled(false);
            btnExport.setEnabled(false);
            descArea.setText("");
            htmlPane.setText("");
            for (JPanel p : numPanels) clearGrid(p);
            for (JPanel p : notePanels) clearGrid(p);
        });

        // Layout assembly
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
        controls2.add(btnExport); controls2.add(btnReset);

        JPanel matrices = new JPanel();
        matrices.setLayout(new BoxLayout(matrices, BoxLayout.X_AXIS));
        matrices.setBorder(new EmptyBorder(10, 10, 10, 10));
        for (int i = 0; i < 4; i++) {
            JPanel col = new JPanel();
            col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
            String numTitle  = (i == 0 ? "Num0"     : "Num→T"  + i);
            String noteTitle = (i == 0 ? "Note0"    : "Note→T" + i);
            col.add(new JLabel(numTitle));
            col.add(numPanels[i]);
            col.add(Box.createVerticalStrut(5));
            col.add(new JLabel(noteTitle));
            notePanels[i].setMaximumSize(new Dimension(150, Integer.MAX_VALUE));
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

        setContentPane(root);
    }

    /**
     * Show About dialog with clickable email and a properly sized icon.
     */
    private void showAboutDialog(Image iconImage) {
        String html = "<html><b>Music Cadence Generator</b><br/>" +
                      "Evandro Veloso Gomes<br/>" +
                      "<a href=\"mailto:gnome_gtk2000@yahoo.com.br\">" +
                      "gnome_gtk2000@yahoo.com.br</a></html>";
        JEditorPane pane = new JEditorPane("text/html", html);
        pane.setEditable(false);
        pane.setOpaque(false);

        // Scale the icon to 48×48 for the dialog
        Image scaled = iconImage.getScaledInstance(48, 48, Image.SCALE_SMOOTH);
        ImageIcon aboutIcon = new ImageIcon(scaled);

        JOptionPane.showMessageDialog(
            this,
            pane,
            "About Music Cadence Generator",
            JOptionPane.INFORMATION_MESSAGE,
            aboutIcon
        );
    }

    private void showGrid(JPanel panel, int[][] matrix) {
        panel.removeAll();
        panel.setLayout(new GridLayout(matrix.length, matrix[0].length, 4, 4));
        for (int[] row : matrix) {
            for (int cell : row) {
                JLabel lbl = new JLabel(String.valueOf(cell), SwingConstants.CENTER);
                lbl.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                panel.add(lbl);
            }
        }
        panel.revalidate();
        panel.repaint();
    }

    private void showGrid(JPanel panel, Note[][] matrix) {
        panel.removeAll();
        panel.setLayout(new GridLayout(matrix.length, matrix[0].length, 4, 4));
        for (Note[] row : matrix) {
            for (Note note : row) {
                JLabel lbl = new JLabel(note.toString(), SwingConstants.CENTER);
                lbl.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                panel.add(lbl);
            }
        }
        panel.revalidate();
        panel.repaint();
    }

    private void clearGrid(JPanel panel) {
        panel.removeAll();
        panel.revalidate();
        panel.repaint();
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
