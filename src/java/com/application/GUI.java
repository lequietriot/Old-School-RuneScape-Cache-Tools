package com.application;

import com.displee.cache.CacheLibrary;
import com.displee.cache.index.Index;
import com.displee.cache.index.archive.Archive;
import com.formdev.flatlaf.FlatDarculaLaf;
import com.sun.media.sound.SF2Soundbank;
import decoders.*;
import encoders.*;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.stage.Stage;
import modelviewer.ModelViewer;
import osrs.*;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;

public class GUI extends JFrame {

    public static CacheLibrary cacheLibrary;

    public static DevicePcmPlayer devicePcmPlayer;

    public static MidiPcmStream midiPcmStream;

    static JPanel contentPanel;

    static JPanel contentPreviewPane;

    JScrollPane cacheScrollPane;

    JTree cacheTree;

    final JMenuItem modelDecoder;

    JTextField songNameInput;

    public static JLabel cacheOperationInfo;

    DefaultMutableTreeNode cacheNode;

    DefaultMutableTreeNode indexNode;

    DefaultMutableTreeNode archiveNode;

    DefaultMutableTreeNode fileNode;

    public static int selectedIndex;
    public int[] selectedIndices;
    public static int selectedArchive;
    public int[] selectedArchives;
    public static int selectedFile;
    public int[] selectedFiles;

    private static final File defaultCachePath;

    static {
        defaultCachePath = new File(System.getProperty("user.home") + File.separator + "jagexcache" + File.separator + "oldschool" + File.separator + "LIVE");
    }

    JFXPanel modelViewPanel;
    Scene modelScene;
    ModelViewer modelViewer;

    public GUI() {
        super("Old School RuneScape Cache Tools v0.5-beta");
        setSize(640, 480);
        setMinimumSize(new Dimension(640, 480));
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setIconImage(new ImageIcon(Objects.requireNonNull(getClass().getClassLoader().getResource("logo.png"))).getImage());
        setLayout(new GridLayout());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        try {
            UIManager.setLookAndFeel(new FlatDarculaLaf());
            SwingUtilities.updateComponentTreeUI(this);
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        JMenuBar jMenuBar = new JMenuBar();
        setJMenuBar(jMenuBar);

        JMenu fileMenu = new JMenu("File");
        jMenuBar.add(fileMenu);

        JMenuItem loadCache = new JMenuItem("Load Cache");
        loadCache.addActionListener(e -> chooseCacheFolder());
        fileMenu.add(loadCache);

        JMenuItem findFile = new JMenuItem("Select Cache Data");
        findFile.addActionListener(e -> findSelectFile());
        fileMenu.add(findFile);

        JMenuItem searchCache = new JMenuItem("Search Cache for...");
        searchCache.addActionListener(e -> searchCacheData());
        fileMenu.add(searchCache);

        JMenu encoderMenu = new JMenu("Data Encoders");
        jMenuBar.add(encoderMenu);

        JMenuItem midiEncoder = new JMenuItem("MIDI Encoder");
        midiEncoder.addActionListener(e -> new MidiEncoder(this));
        encoderMenu.add(midiEncoder);

        JMenuItem modelEncoder = new JMenuItem("Model Encoder");
        modelEncoder.addActionListener(e -> new ModelEncoder(this));
        encoderMenu.add(modelEncoder);

        JMenuItem vorbisEncoder = new JMenuItem("Vorbis Encoder");
        vorbisEncoder.addActionListener(e -> new VorbisEncoder(this));
        encoderMenu.add(vorbisEncoder);

        JMenu decoderMenu = new JMenu("Data Decoders");
        jMenuBar.add(decoderMenu);

        JMenuItem enumDumper = new JMenuItem("Enum Decoder");
        enumDumper.addActionListener(e -> new EnumDecoder());
        //decoderMenu.add(enumDumper);

        JMenuItem sfxDumper = new JMenuItem("SFX Decoder");
        sfxDumper.addActionListener(e -> new SoundEffectDecoder(this));
        decoderMenu.add(sfxDumper);

        JMenuItem midiDecoder = new JMenuItem("MIDI Decoder");
        midiDecoder.addActionListener(e -> new MidiDecoder(this));
        decoderMenu.add(midiDecoder);

        modelDecoder = new JMenuItem("Model Decoder");
        decoderMenu.add(modelDecoder);

        JMenuItem rs3ModelDecoder = new JMenuItem("RS3 Model Decoder");
        rs3ModelDecoder.addActionListener(e -> new ModelDecoderRS3(this));
        //decoderMenu.add(rs3ModelDecoder);

        JMenuItem vorbisDecoder = new JMenuItem("Vorbis Decoder");
        vorbisDecoder.addActionListener(e -> new VorbisDecoder(this));
        decoderMenu.add(vorbisDecoder);

        JMenuItem soundBankDecoder = new JMenuItem("Sound Bank Decoder");
        soundBankDecoder.addActionListener(e -> new SoundBankDecoder(this));
        decoderMenu.add(soundBankDecoder);

        JMenu batchDecoderMenu = new JMenu("Batch Decoders");
        jMenuBar.add(batchDecoderMenu);

        JMenuItem enumBatchDecoder = new JMenuItem("Configuration - Enums");
        enumBatchDecoder.addActionListener(e -> new EnumDecoder());
        batchDecoderMenu.add(enumBatchDecoder);

        JMenu toolsMenu = new JMenu("Tools");
        jMenuBar.add(toolsMenu);

        JMenuItem musicPlayer = new JMenuItem("Music Player");
        musicPlayer.addActionListener(e -> chooseMusicTrack());
        toolsMenu.add(musicPlayer);

        JMenuItem musicPort = new JMenuItem("Use Music Port");
        musicPort.addActionListener(e -> useMusicPort());
        toolsMenu.add(musicPort);

        JMenuItem quickTest = new JMenuItem("Quick Sound Test");
        quickTest.addActionListener(e -> quickTestSound());
        //toolsMenu.add(quickTest);

        JMenuItem synthPatchEditor = new JMenuItem("Synth Patch Editor");
        synthPatchEditor.addActionListener(e -> editSynthPatch());
        //toolsMenu.add(synthPatchEditor);

        JMenuItem xteaKeys = new JMenuItem("Xtea Keys Tool");
        xteaKeys.addActionListener(e -> xteaKeysTool());
        toolsMenu.add(xteaKeys);

        JMenuItem test = new JMenuItem("Test tool");
        test.addActionListener(e -> testTool());
        toolsMenu.add(test);

        JMenuItem convertToOldModel = new JMenuItem("Model - Convert to Old Format");
        convertToOldModel.addActionListener(e -> new ModelOldConverter(this));
        toolsMenu.add(convertToOldModel);

        JMenuItem convertToNewModel = new JMenuItem("Model - Convert to New Format");
        convertToNewModel.addActionListener(e -> new ModelNewConverter(this));
        //toolsMenu.add(convertToNewModel);

        JLabel loadCacheLabel = new JLabel("Please load your cache from the File menu to begin!");
        loadCacheLabel.setVerticalAlignment(SwingConstants.CENTER);
        loadCacheLabel.setHorizontalAlignment(SwingConstants.CENTER);

        contentPanel = new JPanel();
        contentPanel.setLayout(new BorderLayout());
        contentPanel.add(loadCacheLabel);

        setContentPane(contentPanel);
        loadCache(defaultCachePath);
        initModelToolModes();
    }

    private void findSelectFile() {
        String findPrompt = JOptionPane.showInputDialog("Please enter the Index, Archive, and File ID of what you want to select.", selectedIndex + "-" + selectedArchive + "-" + selectedFile);
        if (findPrompt != null) {
            String[] values = findPrompt.split("-");
            selectedIndex = Integer.parseInt(values[0]);
            selectedArchive = Integer.parseInt(values[1]);
            selectedFile = Integer.parseInt(values[2]);
        }
    }

    private void searchCacheData() {
        String searchPrompt = JOptionPane.showInputDialog("Enter the name of what you are looking for (Please check console for info on the search).");
        if (searchPrompt != null) {
            int searchValueHash = searchPrompt.toLowerCase().hashCode();
            new Thread(() -> {
                System.out.println("Searching for: " + searchPrompt);
                for (int index = 0; index < cacheLibrary.indices().length; index++) {
                    for (int archive = 0; archive < cacheLibrary.index(index).archives().length; archive++) {
                        if (cacheLibrary.index(index).archive(archive) != null) {
                            int archiveHashName = Objects.requireNonNull(cacheLibrary.index(index).archive(archive)).getHashName();
                            if (archiveHashName == searchValueHash) {
                                System.out.println("Index " + index + ", " + "Archive " + archive + ", File 0");
                            }
                            //Enum Definitions
                            if (index == 2 && archive == 8) {
                                if (cacheLibrary.index(index).archive(archive) != null) {
                                    for (int file = 0; file < Objects.requireNonNull(cacheLibrary.index(index).archive(archive)).files().length; file++) {
                                        if (Objects.requireNonNull(cacheLibrary.index(index).archive(archive)).file(file) != null) {
                                            EnumComposition enumComposition = new EnumComposition();
                                            enumComposition.decode(new Buffer(Objects.requireNonNull(Objects.requireNonNull(cacheLibrary.index(index).archive(archive)).file(file)).getData()));
                                            if (enumComposition.strVals != null) {
                                                for (String stringValue : enumComposition.strVals) {
                                                    if (stringValue.contains(searchPrompt)) {
                                                        System.out.println("Index " + index + ", " + "Archive " + archive + ", File " + file);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            //NPC Definitions
                            if (index == 2 && archive == 9) {
                                if (cacheLibrary.index(index).archive(archive) != null) {
                                    for (int file = 0; file < Objects.requireNonNull(cacheLibrary.index(index).archive(archive)).files().length; file++) {
                                        if (Objects.requireNonNull(cacheLibrary.index(index).archive(archive)).file(file) != null) {
                                            NPCComposition npcComposition = new NPCComposition();
                                            npcComposition.decode(new Buffer(cacheLibrary.data(index, archive, file)));
                                            if (npcComposition.name.contains(searchPrompt)) {
                                                System.out.println("Index " + index + ", " + "Archive " + archive + ", File " + file);
                                            }
                                        }
                                    }
                                }
                            } else {
                                if (cacheLibrary.index(index).archive(archive) != null) {
                                    for (int file = 0; file < Objects.requireNonNull(cacheLibrary.index(index).archive(archive)).files().length; file++) {
                                        if (Objects.requireNonNull(cacheLibrary.index(index).archive(archive)).file(file) != null) {
                                            if (cacheLibrary.data(index, archive, file) != null) {
                                                if (new String(Objects.requireNonNull(cacheLibrary.data(index, archive, file))).contains(searchPrompt)) {
                                                    System.out.println("Index " + index + ", " + "Archive " + archive + ", File " + file);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                System.out.println("Done searching");
            }).start();
        }
    }

    private void testTool() {
        /*
        NPCComposition npcComposition = new NPCComposition();
        npcComposition.decode(new Buffer(cacheLibrary.data(selectedIndex, selectedArchive, selectedFile)));
        System.out.println("Name: " + npcComposition.name);
        System.out.println("Model IDs: " + Arrays.toString(npcComposition.models));
        System.out.println("Size: " + npcComposition.size);
        System.out.println("Idle Sequence: " + npcComposition.idleSequence);
        System.out.println("Walk Sequence: " + npcComposition.walkSequence);
        System.out.println("Walk Back Sequence: " + npcComposition.walkBackSequence);
        System.out.println("Walk Left Sequence: " + npcComposition.walkLeftSequence);
        System.out.println("Walk Right Sequence: " + npcComposition.walkRightSequence);
        System.out.println("Actions: " + Arrays.toString(npcComposition.actions));
         */
    }

    private void editSynthPatch() {

    }

    private void chooseMusicTrack() {

        JSplitPane musicPlayerMasterPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        JPanel musicPlayerPanel = new JPanel();

        JLabel songInfoLabel = new JLabel("Current Loaded Song:");

        songNameInput = new JTextField("", 30);
        songNameInput.setText(AppConstants.currentSongName);
        songNameInput.addActionListener(e -> updateCurrentSong(songNameInput));

        JButton loadMusicButton = new JButton("Load MIDI");
        loadMusicButton.addActionListener(e -> loadSong());

        JButton queueMidiButton = new JButton("Queue Next MIDI");
        queueMidiButton.addActionListener(e -> queueNextSong());

        JButton musicPlayButton = new JButton("Play");
        musicPlayButton.addActionListener(e -> playSong());

        JButton musicStopButton = new JButton("Stop");
        musicStopButton.addActionListener(e -> stopSong());

        JButton musicRenderButton = new JButton("Render to File");
        musicRenderButton.addActionListener(e -> renderSong());

        JButton musicRenderAllButton = new JButton("Render all Music to Files");
        musicRenderAllButton.addActionListener(e -> renderAllSongs());

        JButton exitMusicPlayerButton = new JButton("Exit Music Player");
        exitMusicPlayerButton.addActionListener(e -> exitPlayer());

        musicPlayerPanel.add(songInfoLabel);
        musicPlayerPanel.add(songNameInput);
        musicPlayerPanel.add(loadMusicButton);
        if (AppConstants.streaming) {
            musicPlayerPanel.add(queueMidiButton);
        }
        musicPlayerPanel.add(musicPlayButton);
        musicPlayerPanel.add(musicStopButton);
        musicPlayerPanel.add(musicRenderButton);
        musicPlayerPanel.add(musicRenderAllButton);
        musicPlayerPanel.add(exitMusicPlayerButton);

        JPanel settingsPanel = new JPanel();

        JLabel musicIndexLabel = new JLabel("Cache Index Selection");

        String[] musicIndices = new String[]{"Music Tracks (6)", "Music Jingles (11)"};
        JComboBox<String> musicIndexComboBox = new JComboBox<>(musicIndices);
        musicIndexComboBox.addActionListener(e -> changeIndex(musicIndexComboBox));

        JLabel sampleRateLabel = new JLabel("Sample Rate");

        JTextField sampleRateSetter = new JTextField("" + AppConstants.sampleRate, 8);
        sampleRateSetter.addActionListener(e -> updateSampleRate(sampleRateSetter));
        sampleRateSetter.addActionListener(new FieldActionListener(sampleRateSetter));

        JLabel volumeLevelLabel = new JLabel("Volume Level");

        JTextField volumeLevelSetter = new JTextField("" + AppConstants.volumeLevel, 8);
        volumeLevelSetter.addActionListener(e -> updateVolumeLevel(volumeLevelSetter));
        volumeLevelSetter.addActionListener(new FieldActionListener(volumeLevelSetter));

        JCheckBox stereoSoundSetter = new JCheckBox("Use Stereo Sound");
        stereoSoundSetter.setSelected(AppConstants.stereo);
        stereoSoundSetter.addActionListener(e -> updateStereoMode(stereoSoundSetter));

        JCheckBox shuffleModeSetter = new JCheckBox("Shuffle Music");
        shuffleModeSetter.setSelected(AppConstants.shuffle);
        shuffleModeSetter.addActionListener(e -> updateShuffleMode(shuffleModeSetter));

        JLabel customSoundFontLabel = new JLabel("SoundFont Path");

        JTextField customSoundFontSetter = new JTextField("" + AppConstants.customSoundFontPath, 16);
        customSoundFontSetter.addActionListener(e -> updateCurrentSoundFont(customSoundFontSetter));
        customSoundFontSetter.addActionListener(new FieldActionListener(customSoundFontSetter));

        JCheckBox customSoundFontCheckBox = new JCheckBox("Use Custom SoundFont");
        customSoundFontCheckBox.setSelected(AppConstants.usingSoundFont);
        customSoundFontCheckBox.addActionListener(e -> updateCustomSoundFontMode(customSoundFontCheckBox));

        settingsPanel.add(musicIndexLabel);
        settingsPanel.add(musicIndexComboBox);
        settingsPanel.add(sampleRateLabel);
        settingsPanel.add(sampleRateSetter);
        settingsPanel.add(volumeLevelLabel);
        settingsPanel.add(volumeLevelSetter);
        settingsPanel.add(stereoSoundSetter);
        settingsPanel.add(shuffleModeSetter);
        settingsPanel.add(customSoundFontCheckBox);
        settingsPanel.add(customSoundFontLabel);
        settingsPanel.add(customSoundFontSetter);
        settingsPanel.revalidate();

        musicPlayerMasterPanel.setTopComponent(musicPlayerPanel);
        musicPlayerMasterPanel.setBottomComponent(settingsPanel);
        musicPlayerMasterPanel.setEnabled(false);
        musicPlayerMasterPanel.setResizeWeight(0.25);
        musicPlayerMasterPanel.revalidate();

        setContentPane(musicPlayerMasterPanel);
        this.revalidate();
    }

    private void useMusicPort() {
        new Thread(() -> {
            if (midiPcmStream != null) {
                stopSong();
            }

            MusicTrack musicTrack = new MusicTrack();
            musicTrack.table = new NodeHashTable(cacheLibrary.index(15).archives().length);
            for (Archive archive : cacheLibrary.index(15).archives()) {
                byte[] bytes = new byte[128];
                Arrays.fill(bytes, (byte) 1);
                musicTrack.table.put(new ByteArrayNode(bytes), archive.getId());
            }

            midiPcmStream = new MidiPcmStream();
            midiPcmStream.setInitialPatch(9, 128);
            midiPcmStream.loadMusicTrack(musicTrack, cacheLibrary.index(15), new SoundCache(cacheLibrary.index(4), cacheLibrary.index(14)), 0);
            midiPcmStream.setPcmStreamVolume(AppConstants.volumeLevel);

            MidiReceiver midiReceiver = new MidiReceiver(midiPcmStream);
            MidiDevice.Info[] infoArray = MidiSystem.getMidiDeviceInfo();

            AppConstants.pcmPlayerProvider = new DevicePcmPlayerProvider();
            devicePcmPlayer = (DevicePcmPlayer) AppConstants.pcmPlayerProvider.player();

            try {
                for (MidiDevice.Info info : infoArray) {
                    MidiDevice midiDevice = MidiSystem.getMidiDevice(info);
                    if (midiDevice.getDeviceInfo().getName().contains("Port")) {
                        if (!midiDevice.isOpen()) {
                            midiDevice.open();
                            if (midiDevice.getMaxTransmitters() != 0) {
                                midiDevice.getTransmitter().setReceiver(midiReceiver);
                                System.out.println(midiDevice.getDeviceInfo() + " set!");
                            }
                        }
                    }
                }

                devicePcmPlayer.init();
                devicePcmPlayer.setStream(midiPcmStream);
                devicePcmPlayer.open(512);
                devicePcmPlayer.samples = new int[512];
                while (true) {
                    devicePcmPlayer.fill(devicePcmPlayer.samples, 256);
                    devicePcmPlayer.write();
                }
            } catch (MidiUnavailableException | LineUnavailableException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void changeIndex(JComboBox<String> musicIndexComboBox) {
        if (musicIndexComboBox.getSelectedItem() == "Music Tracks (6)") {
            AppConstants.currentMusicIndex = 6;
        }
        if (musicIndexComboBox.getSelectedItem() == "Music Jingles (11)") {
            AppConstants.currentMusicIndex = 11;
        }
    }

    private void updateCurrentSong(JTextField songName) {
        if (songName.getText() != null) {
            AppConstants.currentSongName = songName.getText();
        }
    }

    private void loadSong() {
        JFileChooser chooseMidi = new JFileChooser();
        chooseMidi.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (chooseMidi.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selected = chooseMidi.getSelectedFile();
            if (selected.getName().endsWith(".mid")) {
                try {
                    AppConstants.currentMusicFiles = selected.getParentFile().listFiles();
                    AppConstants.midiMusicFileBytes = Files.readAllBytes(Paths.get(selected.toURI()));
                    AppConstants.currentSongName = selected.getName().replace(".mid", "").trim() + " (Custom)";
                    songNameInput.setText(AppConstants.currentSongName);
                    if (AppConstants.shuffle) {
                        if (AppConstants.currentSongName.contains("(Custom)")) {
                            File nextSelected = AppConstants.currentMusicFiles[(int) (Math.random() * AppConstants.currentMusicFiles.length)];
                            if (nextSelected.getName().endsWith(".mid")) {
                                AppConstants.nextMidiFileBytes = Files.readAllBytes(Paths.get(nextSelected.toURI()));
                                AppConstants.nextSongName = nextSelected.getName().replace(".mid", "").trim() + "(Custom)";
                            }
                        }
                        else {
                            AppConstants.nextSongName = String.valueOf((int) (Math.random() * cacheLibrary.index(AppConstants.currentMusicIndex).archives().length));
                        }
                    }
                } catch (IOException e) {
                e.printStackTrace();
                }
            }
        }
    }

    private void queueNextSong() {
        JFileChooser chooseMidi = new JFileChooser();
        chooseMidi.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (chooseMidi.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selected = chooseMidi.getSelectedFile();
            if (selected.getName().endsWith(".mid")) {
                try {
                    if (AppConstants.streaming) {
                        FileOutputStream fileOutputStream = new FileOutputStream(System.getProperty("user.home") + File.separator + "Documents" + File.separator + "Current Song.txt");
                        DataOutputStream dataOutputStream = new DataOutputStream(fileOutputStream);
                        AppConstants.currentMusicFiles = selected.getParentFile().listFiles();
                        AppConstants.nextMidiFileBytes = Files.readAllBytes(Paths.get(selected.toURI()));
                        AppConstants.nextSongName = selected.getName().replace(".mid", "").trim() + " (Custom)";
                        if (AppConstants.currentSongName.contains(" - ")) {
                            int index = AppConstants.currentSongName.lastIndexOf(" - ");
                            String name = AppConstants.currentSongName.substring(index).replace(".mid", "").replace(" - ", "").trim();
                            if (name.contains("(Custom)")) {
                                name = name.replace("(Custom)", "").trim();
                            }
                            dataOutputStream.write(("Current Song: " + name).getBytes(StandardCharsets.UTF_8));
                        } else {
                            String name = AppConstants.currentSongName.replace(".mid", "").trim();
                            if (name.contains("(Custom)")) {
                                name = name.replace("(Custom)", "").trim();
                            }
                            dataOutputStream.write(("Current Song: " + name).getBytes(StandardCharsets.UTF_8));
                        }
                        if (AppConstants.shuffle) {
                            if (AppConstants.nextSongName.contains(" - ")) {
                                int index = AppConstants.nextSongName.lastIndexOf(" - ");
                                String name = AppConstants.nextSongName.substring(index).replace(" - ", "").trim();
                                if (name.contains("(Custom)")) {
                                    name = name.replace("(Custom)", "").trim();
                                }
                                dataOutputStream.write(("\nNext Song: " + name).getBytes(StandardCharsets.UTF_8));
                            } else {
                                String name = AppConstants.nextSongName.replace(".mid", "").trim();
                                if (name.contains("(Custom)")) {
                                    name = name.replace("(Custom)", "").trim();
                                }
                                dataOutputStream.write(("\nNext Song: " + name).getBytes(StandardCharsets.UTF_8));
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void playSong() {
        try {
            if (AppConstants.streaming) {
                FileOutputStream fileOutputStream = new FileOutputStream(System.getProperty("user.home") + File.separator + "Documents" + File.separator + "Current Song.txt");
                DataOutputStream dataOutputStream = new DataOutputStream(fileOutputStream);
                if (AppConstants.currentSongName.contains(" - ")) {
                    int index = AppConstants.currentSongName.lastIndexOf(" - ");
                    String name = AppConstants.currentSongName.substring(index).replace(".mid", "").replace(" - ", "").trim();
                    if (name.contains("(Custom)")) {
                        name = name.replace("(Custom)", "").trim();
                    }
                    dataOutputStream.write(("Current Song: " + name).getBytes(StandardCharsets.UTF_8));
                } else {
                    String name = AppConstants.currentSongName.replace(".mid", "").trim();
                    if (name.contains("(Custom)")) {
                        name = name.replace("(Custom)", "").trim();
                    }
                    dataOutputStream.write(("Current Song: " + name).getBytes(StandardCharsets.UTF_8));
                }
                if (AppConstants.shuffle) {
                    if (AppConstants.currentSongName.contains("(Custom)")) {
                        File selected = AppConstants.currentMusicFiles[(int) (Math.random() * AppConstants.currentMusicFiles.length)];
                        if (selected.getName().endsWith(".mid")) {
                            AppConstants.nextMidiFileBytes = Files.readAllBytes(Paths.get(selected.toURI()));
                            AppConstants.nextSongName = selected.getName().replace(".mid", "").trim() + "(Custom)";
                        }
                    } else {
                        AppConstants.nextSongName = String.valueOf((int) (Math.random() * cacheLibrary.index(AppConstants.currentMusicIndex).archives().length));
                    }
                    if (AppConstants.nextSongName.contains(" - ")) {
                        int index = AppConstants.nextSongName.lastIndexOf(" - ");
                        String name = AppConstants.nextSongName.substring(index).replace(" - ", "").trim();
                        if (name.contains("(Custom)")) {
                            name = name.replace("(Custom)", "").trim();
                        }
                        dataOutputStream.write(("\nNext Song: " + name).getBytes(StandardCharsets.UTF_8));
                    }
                }
            }
            initSoundEngine();
        } catch (IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    private void stopSong() {
        new Thread(() -> {
            if (midiPcmStream != null) {
                while (midiPcmStream.getPcmStreamVolume() != 0) {
                    if (midiPcmStream.getPcmStreamVolume() == 0) {
                        break;
                    } else {
                        midiPcmStream.setPcmStreamVolume(midiPcmStream.getPcmStreamVolume() - 1);
                    }
                }
                midiPcmStream = null;
                devicePcmPlayer.setStream(null);
            }
        }).start();
    }

    private void renderSong() {
        try {
            if (midiPcmStream == null) {
                midiPcmStream = new MidiPcmStream();
                midiPcmStream.setInitialPatch(9, 128);

                AppConstants.pcmPlayerProvider = new DevicePcmPlayerProvider();
                devicePcmPlayer = (DevicePcmPlayer) AppConstants.pcmPlayerProvider.player();

                MusicTrack musicTrack = new MusicTrack();
                if (AppConstants.currentSongName.contains("(Custom)")) {
                    musicTrack = MusicTrack.setTrack(AppConstants.midiMusicFileBytes);
                } else {
                    try {
                        if (Integer.parseInt(AppConstants.currentSongName) != -1) {
                            musicTrack = MusicTrack.readTrack(cacheLibrary.index(AppConstants.currentMusicIndex), Integer.parseInt(AppConstants.currentSongName), 0);
                        }
                    } catch (NumberFormatException e) {
                        musicTrack = MusicTrack.readTrackFromString(cacheLibrary.index(AppConstants.currentMusicIndex), AppConstants.currentSongName);
                    }
                }

                SoundCache soundCache = new SoundCache(cacheLibrary.index(4), cacheLibrary.index(14));

                if (musicTrack != null && midiPcmStream.loadMusicTrack(musicTrack, cacheLibrary.index(15), soundCache, 0)) {
                    midiPcmStream.setPcmStreamVolume(AppConstants.volumeLevel);
                    midiPcmStream.setMusicTrack(musicTrack, false);
                    if (AppConstants.usingSoundFont) {
                        midiPcmStream.loadSoundFont(new SF2Soundbank(new File(AppConstants.customSoundFontPath)), -1);
                    }
                    devicePcmPlayer.init();
                    devicePcmPlayer.setStream(midiPcmStream);
                    devicePcmPlayer.samples = new int[512];
                    while (midiPcmStream != null && midiPcmStream.isReady()) {
                        devicePcmPlayer.fill(devicePcmPlayer.samples, 256);
                        devicePcmPlayer.writeToBuffer();
                    }
                    byte[] rendered = devicePcmPlayer.byteArrayOutputStream.toByteArray();
                    AudioInputStream audioInputStream = new AudioInputStream(new ByteArrayInputStream(rendered), devicePcmPlayer.format, rendered.length);
                    try {
                        File outputFilePath = new File(cacheLibrary.getPath() + File.separator + "Output");
                        boolean madeDirectory = outputFilePath.mkdirs();
                        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, new File(outputFilePath + File.separator + AppConstants.currentSongName + "_Index_" + AppConstants.currentMusicIndex + ".wav"));
                        if (madeDirectory) {
                            cacheOperationInfo.setText("An output directory in your current cache folder was created!\n" + AppConstants.currentSongName + " was then rendered successfully to your output folder!");
                        } else {
                            cacheOperationInfo.setText(AppConstants.currentSongName + " was rendered successfully to your output folder!");
                        }
                        AppConstants.currentSongName = null;
                        stopSong();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void renderAllSongs() {
        for (int music = 0; music < 2000; music++) {
            if (cacheLibrary.index(6).archive(music) != null) {
                AppConstants.currentMusicIndex = 6;
                AppConstants.currentSongName = String.valueOf(music);
                renderSong();
            }
        }

        for (int jingle = 0; jingle < 1000; jingle++) {
            if (cacheLibrary.index(11).archive(jingle) != null) {
                AppConstants.currentMusicIndex = 11;
                AppConstants.currentSongName = String.valueOf(jingle);
                renderSong();
            }
        }
    }

    private void exitPlayer() {
        stopSong();
        setContentPane(contentPanel);
        revalidate();
    }

    private void initSoundEngine() throws LineUnavailableException {
        new Thread(() -> {
            try {
                if (midiPcmStream == null) {
                    midiPcmStream = new MidiPcmStream();
                    midiPcmStream.setInitialPatch(9, 128);

                    AppConstants.pcmPlayerProvider = new DevicePcmPlayerProvider();
                    devicePcmPlayer = (DevicePcmPlayer) AppConstants.pcmPlayerProvider.player();

                    MusicTrack musicTrack = null;
                    if (AppConstants.currentSongName.contains("(Custom)")) {
                        musicTrack = MusicTrack.setTrack(AppConstants.midiMusicFileBytes);
                    }
                    else {
                        try {
                            if (Integer.parseInt(AppConstants.currentSongName) > -1) {
                                musicTrack = MusicTrack.readTrack(cacheLibrary.index(AppConstants.currentMusicIndex), Integer.parseInt(AppConstants.currentSongName), 0);
                            }
                        } catch (NumberFormatException e) {
                            musicTrack = MusicTrack.readTrackFromString(cacheLibrary.index(AppConstants.currentMusicIndex), AppConstants.currentSongName);
                        }
                    }

                    SoundCache soundCache = new SoundCache(cacheLibrary.index(4), cacheLibrary.index(14));

                    if (musicTrack != null && midiPcmStream.loadMusicTrack(musicTrack, cacheLibrary.index(15), soundCache, 0)) {
                        midiPcmStream.setPcmStreamVolume(AppConstants.volumeLevel);
                        midiPcmStream.setMusicTrack(musicTrack, false);
                        if (AppConstants.usingSoundFont) {
                            midiPcmStream.loadSoundFont(new SF2Soundbank(new File(AppConstants.customSoundFontPath)), -1);
                        }
                        else {
                            midiPcmStream.loadMusicTrack(musicTrack, cacheLibrary.index(15), soundCache, 0);
                        }
                        devicePcmPlayer.init();
                        devicePcmPlayer.setStream(midiPcmStream);
                        devicePcmPlayer.open(16384);
                        devicePcmPlayer.samples = new int[512];
                        while (midiPcmStream != null && midiPcmStream.isReady()) {
                            devicePcmPlayer.fill(devicePcmPlayer.samples, 256);
                            devicePcmPlayer.write();
                            if (AppConstants.shuffle && !midiPcmStream.isReady()) {
                                stopSong();
                                AppConstants.currentSongName = AppConstants.nextSongName;
                                AppConstants.midiMusicFileBytes = AppConstants.nextMidiFileBytes;
                                playSong();
                                System.out.println("Playing " + AppConstants.currentSongName);
                            }
                        }
                    }
                }
            } catch (LineUnavailableException | IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void updateSampleRate(JTextField sampleRateSetter) {
        if (sampleRateSetter.getText() != null) {
            AppConstants.sampleRate = Integer.parseInt(sampleRateSetter.getText());
        }
    }

    private void updateVolumeLevel(JTextField volumeLevelSetter) {
        if (volumeLevelSetter.getText() != null) {
            AppConstants.volumeLevel = Integer.parseInt(volumeLevelSetter.getText());
            if (midiPcmStream != null) {
                midiPcmStream.setPcmStreamVolume(AppConstants.volumeLevel);
            }
        }
    }

    private void updateStereoMode(JCheckBox stereoSoundSetter) {
        AppConstants.stereo = stereoSoundSetter.isSelected();
    }

    private void updateShuffleMode(JCheckBox shuffleModeSetter) {
        AppConstants.shuffle = shuffleModeSetter.isSelected();
    }

    private void updateCurrentSoundFont(JTextField soundFontSetter) {
        if (soundFontSetter.getText() != null) {
            AppConstants.customSoundFontPath = soundFontSetter.getText();
        }
    }

    private void updateCustomSoundFontMode(JCheckBox soundFontSetter) {
        AppConstants.usingSoundFont = soundFontSetter.isSelected();
    }

    private void chooseCacheFolder() {
        JFileChooser cacheChooser = new JFileChooser();
        cacheChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (cacheChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selected = cacheChooser.getSelectedFile();
            loadCache(selected);
        }
    }

    private void loadCache(File cache) {

        if (cache.isDirectory() && cache.exists()) {
            try {
                cacheLibrary = new CacheLibrary(cache.getPath(), false, null);
                if (cacheLibrary.is317()) {
                    AppConstants.cacheType = "RuneScape 2";
                }
                if (cacheLibrary.isOSRS()) {
                    AppConstants.cacheType = "Old School RuneScape";
                }
                if (!cacheLibrary.isOSRS() && cacheLibrary.indices().length > 15) {
                    AppConstants.cacheType = "RuneScape High Definition";

                }
                if (cacheLibrary.isRS3()) {
                    AppConstants.cacheType = "RuneScape 3";
                }
                initModelToolModes();

                modelViewer = new ModelViewer();
                modelViewPanel = new JFXPanel();
                Platform.runLater(() -> {
                    try {
                        modelScene = modelViewer.start(new Stage()).getScene();
                        modelViewPanel.setScene(modelScene);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                initFileViewer();
                contentPreviewPane.revalidate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        this.revalidate();
    }

    private void initModelToolModes() {
        if (AppConstants.cacheType.equals("RuneScape 2")) {
            if (modelDecoder.getActionListeners() != null) {
                for (ActionListener actionListener : modelDecoder.getActionListeners()) {
                    modelDecoder.removeActionListener(actionListener);
                }
            }
            modelDecoder.addActionListener(e -> new ModelDecoderRS2(this));
        }
        if (AppConstants.cacheType.equals("Old School RuneScape")) {
            if (modelDecoder.getActionListeners() != null) {
                for (ActionListener actionListener : modelDecoder.getActionListeners()) {
                    modelDecoder.removeActionListener(actionListener);
                }
            }
            modelDecoder.addActionListener(e -> new ModelDecoderOS(this));
        }
        if (AppConstants.cacheType.equals("RuneScape High Definition")) {
            if (modelDecoder.getActionListeners() != null) {
                for (ActionListener actionListener : modelDecoder.getActionListeners()) {
                    modelDecoder.removeActionListener(actionListener);
                }
            }
            modelDecoder.addActionListener(e -> new ModelDecoderHD(this));
        }
        if (AppConstants.cacheType.equals("RuneScape 3")) {
            if (modelDecoder.getActionListeners() != null) {
                for (ActionListener actionListener : modelDecoder.getActionListeners()) {
                    modelDecoder.removeActionListener(actionListener);
                }
            }
            modelDecoder.addActionListener(e -> new ModelDecoderRS3(this));
        }
    }

    private void initFileViewer() {

        contentPanel.removeAll();

        JLabel cacheLoadedInfo = new JLabel("Loaded cache from local path - " + cacheLibrary.getPath());
        cacheLoadedInfo.setFont(cacheLoadedInfo.getFont().deriveFont(Font.BOLD, 14));

        cacheOperationInfo = cacheLoadedInfo;

        JSplitPane splitCacheViewPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        buildTreeNode();

        cacheTree = new JTree();
        cacheTree.setModel(new DefaultTreeModel(cacheNode));

        contentPreviewPane = new JPanel();
        contentPreviewPane.setLayout(new GridLayout());

        cacheScrollPane = new JScrollPane(cacheTree);
        cacheScrollPane.setViewportView(cacheTree);
        cacheScrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(20, 0));

        JSplitPane cacheInfoSplitPanel = new JSplitPane();

        JPanel cacheInfoPanel = new JPanel();
        cacheInfoPanel.setLayout(new GridLayout());

        SpringLayout springLayout = new SpringLayout();
        JPanel cacheOperationsButtonPanel = new JPanel();
        cacheOperationsButtonPanel.setLayout(springLayout);

        JButton addFilesButton = new JButton("Add Files");
        addFilesButton.addActionListener(e -> addCacheFiles());
        springLayout.putConstraint(SpringLayout.NORTH, addFilesButton, 0, SpringLayout.NORTH, cacheOperationsButtonPanel);
        springLayout.putConstraint(SpringLayout.WEST, addFilesButton, 0, SpringLayout.WEST, cacheOperationsButtonPanel);

        JButton exportFilesButton = new JButton("Export Files");
        exportFilesButton.addActionListener(e -> exportFilesData());
        springLayout.putConstraint(SpringLayout.NORTH, exportFilesButton, 0, SpringLayout.NORTH, cacheOperationsButtonPanel);
        springLayout.putConstraint(SpringLayout.WEST, exportFilesButton, 100, SpringLayout.WEST, cacheOperationsButtonPanel);

        JButton removeArchiveButton = new JButton("Remove Archive");
        removeArchiveButton.addActionListener(e -> removeArchiveFile());
        springLayout.putConstraint(SpringLayout.NORTH, removeArchiveButton, 0, SpringLayout.NORTH, cacheOperationsButtonPanel);
        springLayout.putConstraint(SpringLayout.WEST, removeArchiveButton, 200, SpringLayout.WEST, cacheOperationsButtonPanel);

        JButton removeFileButton = new JButton("Remove File");
        removeFileButton.addActionListener(e -> removeCacheFile());
        springLayout.putConstraint(SpringLayout.NORTH, removeFileButton, 30, SpringLayout.NORTH, cacheOperationsButtonPanel);
        springLayout.putConstraint(SpringLayout.WEST, removeFileButton, 0, SpringLayout.WEST, cacheOperationsButtonPanel);

        JButton setArchiveNameHashButton = new JButton("Set Archive name hash");
        setArchiveNameHashButton.addActionListener(e -> setCacheArchiveNameHash());
        springLayout.putConstraint(SpringLayout.NORTH, setArchiveNameHashButton, 30, SpringLayout.NORTH, cacheOperationsButtonPanel);
        springLayout.putConstraint(SpringLayout.WEST, setArchiveNameHashButton, 150, SpringLayout.WEST, cacheOperationsButtonPanel);

        JButton setArchiveNameButton = new JButton("Set Archive name");
        setArchiveNameButton.addActionListener(e -> setCacheArchiveName());
        springLayout.putConstraint(SpringLayout.NORTH, setArchiveNameButton, 60, SpringLayout.NORTH, cacheOperationsButtonPanel);
        springLayout.putConstraint(SpringLayout.WEST, setArchiveNameButton, 0, SpringLayout.WEST, cacheOperationsButtonPanel);

        JButton exportAllDataButton = new JButton("Export all Index data");
        exportAllDataButton.addActionListener(e -> dumpAllDataFolders());
        springLayout.putConstraint(SpringLayout.NORTH, exportAllDataButton, 60, SpringLayout.NORTH, cacheOperationsButtonPanel);
        springLayout.putConstraint(SpringLayout.WEST, exportAllDataButton, 150, SpringLayout.WEST, cacheOperationsButtonPanel);

        cacheOperationsButtonPanel.add(addFilesButton);
        cacheOperationsButtonPanel.add(exportFilesButton);
        cacheOperationsButtonPanel.add(removeArchiveButton);
        cacheOperationsButtonPanel.add(removeFileButton);
        cacheOperationsButtonPanel.add(setArchiveNameHashButton);
        cacheOperationsButtonPanel.add(setArchiveNameButton);
        cacheOperationsButtonPanel.add(exportAllDataButton);
        cacheOperationsButtonPanel.revalidate();

        JTable infoTable = new JTable();

        cacheTree.addTreeSelectionListener(e -> {

            if (cacheTree.getSelectionPaths() != null && cacheTree.getSelectionPaths().length > 1) {

                selectedIndices = new int[cacheTree.getSelectionPaths().length];
                selectedArchives = new int[cacheTree.getSelectionPaths().length];
                selectedFiles = new int[cacheTree.getSelectionPaths().length];

                for (int treePathIndex = 0; treePathIndex < cacheTree.getSelectionPaths().length; treePathIndex++) {

                    String[] indexStrings = cacheTree.getSelectionPaths()[treePathIndex].toString().split(",");

                    if (cacheTree.getSelectionPaths()[treePathIndex].toString().contains("Index")) {
                        selectedIndices[treePathIndex] = Integer.parseInt(indexStrings[1].replace("Index ", "").replace("]", "").trim());
                    }
                    if (cacheTree.getSelectionPaths()[treePathIndex].toString().contains("Archive")) {
                        selectedArchives[treePathIndex] = Integer.parseInt(indexStrings[2].replace("Archive ", "").replace("]", "").trim());
                    }
                    if (cacheTree.getSelectionPaths()[treePathIndex].toString().contains("File")) {
                        selectedFiles[treePathIndex] = Integer.parseInt(indexStrings[3].replace("File ", "").replace("]", "").trim());
                    }

                    Object[][] fileFields = new Object[][]{

                            new Object[]{
                                    "Loaded Cache Path", cacheLibrary.getPath()
                            },

                            new Object[]{
                                    "Selected Indices", Arrays.toString(selectedIndices)
                            },

                            new Object[]{
                                    "Selected Archives", Arrays.toString(selectedArchives)
                            },

                            new Object[]{
                                    "Selected Files", Arrays.toString(selectedFiles)
                            },
                    };

                    String[] fileFieldValues = new String[]{
                            "", ""
                    };

                    infoTable.setModel(new DefaultTableModel(fileFields, fileFieldValues));
                    infoTable.setRowHeight(20);
                    infoTable.revalidate();
                }
            }
            else {
                if (Objects.requireNonNull(cacheTree.getSelectionPath()).toString().contains("Index")) {

                    String[] indexStrings = cacheTree.getSelectionPath().toString().split(",");
                    selectedIndex = Integer.parseInt(indexStrings[1].replace("Index ", "").replace("]", "").trim());
                    String indexName = "";

                    if (cacheLibrary.is317()) {
                        AppConstants.cacheType = "RuneScape 2";
                        indexName = String.valueOf(RS2CacheIndices.values()[selectedIndex]).replace("_", " ").trim();
                    }
                    if (cacheLibrary.isOSRS()) {
                        AppConstants.cacheType = "Old School RuneScape";
                        indexName = String.valueOf(OSRSCacheIndices.values()[selectedIndex]).replace("_", " ").trim();
                    }
                    if (!cacheLibrary.isOSRS() && !cacheLibrary.isRS3() && cacheLibrary.indices().length > 15) {
                        AppConstants.cacheType = "RuneScape High Definition";
                        indexName = String.valueOf(RSHDCacheIndices.values()[selectedIndex]).replace("_", " ").trim();

                    }
                    if (cacheLibrary.isRS3()) {
                        AppConstants.cacheType = "RuneScape 3";
                        indexName = String.valueOf(RS3CacheIndices.values()[selectedIndex]).replace("_", " ").trim();
                    }

                    Object[][] indexFields = new Object[][]{

                            new Object[]{
                                    "Loaded Cache Path", cacheLibrary.getPath()
                            },

                            new Object[]{
                                    "Cache Type", AppConstants.cacheType
                            },

                            new Object[]{
                                    "Index Name", indexName
                            },

                            new Object[]{
                                    "Amount of Archives", cacheLibrary.index(selectedIndex).archives().length
                            },

                            new Object[]{
                                    "Index CRC Value", cacheLibrary.index(selectedIndex).getCrc()
                            },

                            new Object[]{
                                    "Index Version", cacheLibrary.index(selectedIndex).getVersion()
                            }
                    };

                    String[] indexFieldValues = new String[]{
                            "", ""
                    };

                    infoTable.setModel(new DefaultTableModel(indexFields, indexFieldValues));
                    infoTable.setRowHeight(20);
                    infoTable.revalidate();
                }

                if (cacheTree.getSelectionPath().toString().contains("Archive")) {

                    String[] indexStrings = cacheTree.getSelectionPath().toString().split(",");
                    selectedIndex = Integer.parseInt(indexStrings[1].replace("Index ", "").replace("]", "").trim());
                    selectedArchive = Integer.parseInt(indexStrings[2].replace("Archive ", "").replace("]", "").trim());

                    Object[][] archiveFields = new Object[][]{

                            new Object[]{
                                    "Loaded Cache Path", cacheLibrary.getPath()
                            },

                            new Object[]{
                                    "Cache Index ID", selectedIndex
                            },

                            new Object[]{
                                    "Archive ID", selectedArchive
                            },

                            new Object[]{
                                    "Archive Name Hash", Objects.requireNonNull(cacheLibrary.index(selectedIndex).archive(selectedArchive)).getHashName()
                            },

                            new Object[]{
                                    "Amount of Files", Objects.requireNonNull(cacheLibrary.index(selectedIndex).archive(selectedArchive)).files().length
                            },

                            new Object[]{
                                    "Archive CRC Value", Objects.requireNonNull(cacheLibrary.index(selectedIndex).archive(selectedArchive)).getCrc()
                            },

                            new Object[]{
                                    "Archive Revision", Objects.requireNonNull(cacheLibrary.index(selectedIndex).archive(selectedArchive)).getRevision()
                            }
                    };

                    String[] archiveFieldValues = new String[]{
                            "", ""
                    };

                    infoTable.setModel(new DefaultTableModel(archiveFields, archiveFieldValues));
                    infoTable.setRowHeight(20);
                    infoTable.revalidate();

                }

                if (cacheTree.getSelectionPath().toString().contains("File")) {

                    String[] indexStrings = cacheTree.getSelectionPath().toString().split(",");
                    selectedIndex = Integer.parseInt(indexStrings[1].replace("Index ", "").replace("]", "").trim());
                    selectedArchive = Integer.parseInt(indexStrings[2].replace("Archive ", "").replace("]", "").trim());
                    selectedFile = Integer.parseInt(indexStrings[3].replace("File ", "").replace("]", "").trim());

                    Object[][] fileFields = new Object[][]{

                            new Object[]{
                                    "Loaded Cache Path", cacheLibrary.getPath()
                            },

                            new Object[]{
                                    "Cache Index ID", selectedIndex
                            },

                            new Object[]{
                                    "Archive ID", selectedArchive
                            },

                            new Object[]{
                                    "File ID", selectedFile
                            },

                            new Object[]{
                                    "File Name Hash", Objects.requireNonNull(Objects.requireNonNull(cacheLibrary.index(selectedIndex).archive(selectedArchive)).file(selectedFile)).getHashName()
                            }
                    };

                    String[] fileFieldValues = new String[]{
                            "", ""
                    };

                    infoTable.setModel(new DefaultTableModel(fileFields, fileFieldValues));
                    infoTable.setRowHeight(20);
                    infoTable.revalidate();
                }
            }

            if (modelViewer != null) {
                modelViewer = null;
                Platform.runLater(() -> {
                    try {
                        modelViewer = new ModelViewer();
                        modelScene = modelViewer.start(new Stage()).getScene();
                        modelViewPanel.setScene(modelScene);
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                });
            }
        });

        cacheInfoPanel.add(infoTable);
        cacheInfoPanel.revalidate();

        cacheInfoSplitPanel.setBottomComponent(cacheOperationsButtonPanel);
        cacheInfoSplitPanel.setTopComponent(cacheInfoPanel);
        cacheInfoSplitPanel.setResizeWeight(0.5);
        cacheInfoSplitPanel.revalidate();

        splitCacheViewPane.setLeftComponent(cacheScrollPane);
        splitCacheViewPane.setRightComponent(cacheInfoSplitPanel);
        splitCacheViewPane.setResizeWeight(0.5);
        splitCacheViewPane.revalidate();

        contentPreviewPane.add(modelViewPanel);
        contentPreviewPane.revalidate();

        JSplitPane splitCacheDetailedViewPane = new JSplitPane();
        splitCacheDetailedViewPane.setLeftComponent(splitCacheViewPane);
        splitCacheDetailedViewPane.setRightComponent(contentPreviewPane);
        splitCacheDetailedViewPane.setResizeWeight(0.3);
        splitCacheDetailedViewPane.revalidate();

        contentPanel.add(splitCacheDetailedViewPane, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.add(cacheLoadedInfo, BorderLayout.SOUTH);
        contentPanel.revalidate();
    }

    private void addCacheFiles() {
        int currentIndex = selectedIndex;
        new Thread(() -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setMultiSelectionEnabled(true);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File[] files = fileChooser.getSelectedFiles();
                for (File file : files) {
                    cacheLibrary.index(currentIndex);
                    try {
                        int fileToAdd = 0;
                        String trimmedName = file.getName().substring(0, file.getName().indexOf(".")).trim();
                        if (trimmedName.contains("-")) {
                            fileToAdd = Integer.parseInt(trimmedName.substring(trimmedName.indexOf("-")).trim().replace("-", "").trim());
                            trimmedName = trimmedName.substring(0, trimmedName.indexOf("-")).trim();
                        }
                        if (cacheLibrary.index(currentIndex).archive(Integer.parseInt(trimmedName)) != null) {
                            Objects.requireNonNull(cacheLibrary.index(currentIndex).archive(Integer.parseInt(trimmedName))).add(fileToAdd, Files.readAllBytes(file.toPath()));
                        }
                        else {
                            cacheLibrary.index(currentIndex).add(Integer.parseInt(trimmedName)).add(fileToAdd, Files.readAllBytes(file.toPath()));
                        }
                        if (cacheLibrary.index(currentIndex).update()) {
                            cacheOperationInfo.setText("Successfully added Archive " + Integer.parseInt(trimmedName) + ", File " + fileToAdd + " to Index " + currentIndex + "!");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                cacheOperationInfo.setText("Operation completed successfully. " + files.length + " files were added to the cache index " + currentIndex);
                loadCache(new File(cacheLibrary.getPath()));
            }
        }).start();
    }

    private void exportFilesData() {
        new Thread(() -> {
            JFileChooser folderChooser = new JFileChooser();
            folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (folderChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File selected = folderChooser.getSelectedFile();
                int currentIndex = selectedIndex;
                try {
                    if (selectedIndices != null) {
                        for (int index : selectedIndices) {
                            if (selectedArchives != null) {
                                for (int archive : selectedArchives) {
                                    if (selectedFiles != null) {
                                        for (int file : selectedFiles) {
                                            File fileData = new File(selected.getPath() + File.separator + archive + "-" + file + ".dat");
                                            FileOutputStream fileOutputStream = new FileOutputStream(fileData);
                                            fileOutputStream.write(Objects.requireNonNull(cacheLibrary.data(index, archive, file)));
                                            fileOutputStream.flush();
                                            fileOutputStream.close();
                                            cacheOperationInfo.setText("Successfully exported Index " + index + ", Archive " + archive + ", File " + file + " to " + selected.getPath() + "!");
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else {
                        int currentArchive = selectedArchive;
                        for (com.displee.cache.index.archive.file.File fileSelection : Objects.requireNonNull(cacheLibrary.index(currentIndex).archive(currentArchive)).files()) {
                            File fileData = new File(selected.getPath() + File.separator + currentArchive + "-" + fileSelection.getId() + ".dat");
                            FileOutputStream fileOutputStream = new FileOutputStream(fileData);
                            if (Objects.requireNonNull(cacheLibrary.index(currentIndex).archive(currentArchive)).file(fileSelection.getId()) != null) {
                                if (Objects.requireNonNull(Objects.requireNonNull(cacheLibrary.index(currentIndex).archive(currentArchive)).file(fileSelection.getId())).getData() != null) {
                                    fileOutputStream.write(Objects.requireNonNull(Objects.requireNonNull(Objects.requireNonNull(cacheLibrary.index(currentIndex).archive(currentArchive)).file(fileSelection.getId())).getData()));
                                    fileOutputStream.flush();
                                    fileOutputStream.close();
                                    cacheOperationInfo.setText("Successfully exported Index " + selectedIndex + ", Archive " + currentArchive + ", File " + fileSelection.getId() + " to " + selected.getPath() + "!");
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                cacheOperationInfo.setText("Operation completed successfully. The selected data was exported to: " + selected.getPath());
                this.revalidate();
            }
        }).start();
    }

    private void setCacheArchiveNameHash() {
        String fileNameToSet = JOptionPane.showInputDialog("Set Archive Name Hash", "");
        if (fileNameToSet != null) {
            if (cacheLibrary.index(selectedIndex).archive(selectedArchive) != null) {
                Archive renamedArchive = cacheLibrary.index(selectedIndex).archive(selectedArchive);
                assert renamedArchive != null;
                renamedArchive.setHashName(Integer.parseInt(fileNameToSet));
                cacheLibrary.index(selectedIndex).add(renamedArchive, renamedArchive.getId());
                if (cacheLibrary.index(selectedIndex).update()) {
                    loadCache(new File(cacheLibrary.getPath()));
                }
            }
        }
        cacheOperationInfo.setText("Operation completed successfully. Archive " + selectedArchive + " was given the hash name value of " + Objects.requireNonNull(fileNameToSet).toLowerCase().hashCode());
        this.revalidate();
    }

    private void setCacheArchiveName() {
        String fileNameToSet = JOptionPane.showInputDialog("Set Archive Name", "");
        if (fileNameToSet != null) {
            if (cacheLibrary.index(selectedIndex).archive(selectedArchive) != null) {
                Archive renamedArchive = cacheLibrary.index(selectedIndex).archive(selectedArchive);
                assert renamedArchive != null;
                renamedArchive.setHashName(fileNameToSet.toLowerCase().hashCode());
                cacheLibrary.index(selectedIndex).add(renamedArchive, renamedArchive.getId());
                if (cacheLibrary.index(selectedIndex).update()) {
                    loadCache(new File(cacheLibrary.getPath()));
                }
            }
        }
        cacheOperationInfo.setText("Operation completed successfully. Archive " + selectedArchive + " was given the name " + fileNameToSet);
        this.revalidate();
    }

    private void removeArchiveFile() {
        cacheLibrary.index(selectedIndex).remove(selectedArchive);
        if (cacheLibrary.index(selectedIndex).update()) {
            loadCache(new File(cacheLibrary.getPath()));
            JOptionPane.showMessageDialog(this, "Cache Archive " + selectedArchive + " has been removed.");
        }
        cacheOperationInfo.setText("Operation completed successfully.");
        this.revalidate();
    }

    private void removeCacheFile() {
        Objects.requireNonNull(cacheLibrary.index(selectedIndex).archive(selectedArchive)).remove(selectedFile);
        if (cacheLibrary.index(selectedIndex).update()) {
            loadCache(new File(cacheLibrary.getPath()));
            JOptionPane.showMessageDialog(this, "Cache File " + selectedFile + " has been removed.");
        }
        cacheOperationInfo.setText("Operation completed successfully.");
        this.revalidate();
    }

    private void dumpAllDataFolders() {
        JFileChooser folderChooser = new JFileChooser();
        folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (folderChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selected = folderChooser.getSelectedFile();
            File indexDirectory = new File(selected + File.separator + selectedIndex);
            if (indexDirectory.mkdirs()) {
                cacheOperationInfo.setText("Created an Index " + selectedIndex + " folder.");
            }
            try {
                int index = selectedIndex;
                cacheLibrary.index(index);
                for (int archive = 0; archive < cacheLibrary.index(index).archives().length; archive++) {
                    if (cacheLibrary.index(index).archive(archive) != null) {
                        for (int file = 0; file < Objects.requireNonNull(cacheLibrary.index(index).archive(archive)).files().length; file++) {
                            File fileData = new File(indexDirectory + File.separator + archive + "-0.dat");
                            if (new File(indexDirectory + File.separator + archive).mkdirs()) {
                                cacheOperationInfo.setText("made directory for file");
                            }
                            if (cacheLibrary.data(index, archive, file) != null) {
                                byte[] output = cacheLibrary.data(index, archive, file);
                                FileOutputStream fileOutputStream = new FileOutputStream(fileData);
                                DataOutputStream dataOutputStream = new DataOutputStream(fileOutputStream);
                                dataOutputStream.write(Objects.requireNonNull(output));
                                dataOutputStream.flush();
                                dataOutputStream.close();
                            }
                        }
                    }
                    else {
                        if (cacheLibrary.index(index).readArchiveSector(archive) != null) {
                            File fileData = new File(indexDirectory + File.separator + archive + "-0.dat");
                            if (new File(indexDirectory + File.separator + archive).mkdirs()) {
                                cacheOperationInfo.setText("made directory for file");
                            }
                            FileOutputStream fileOutputStream = new FileOutputStream(fileData);
                            DataOutputStream dataOutputStream = new DataOutputStream(fileOutputStream);
                            dataOutputStream.write(Objects.requireNonNull(cacheLibrary.index(index).readArchiveSector(archive)).getData());
                            dataOutputStream.flush();
                            dataOutputStream.close();
                        }
                    }
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        cacheOperationInfo.setText("Operation completed successfully.");
    }

    private void buildTreeNode() {
        cacheNode = new DefaultMutableTreeNode("Main Cache");
        cacheLibrary.indices();
        for (Index index : cacheLibrary.indices()) {
            indexNode = new DefaultMutableTreeNode(index);
            cacheNode.add(indexNode);
            index.archives();
            for (Archive archive : index.archives()) {
                archiveNode = new DefaultMutableTreeNode("Archive " + archive.getId());
                indexNode.add(archiveNode);
                archive.getFiles();
                for (com.displee.cache.index.archive.file.File file : archive.files()) {
                    fileNode = new DefaultMutableTreeNode("File " + file.getId());
                    archiveNode.add(fileNode);
                }
            }
        }
        this.revalidate();
    }

    private void xteaKeysTool() {
        String xteaKeysInput = JOptionPane.showInputDialog("Please enter your XTEA key values, separated by commas.", "0, 0, 0, 0");
        String[] xteaKeys = xteaKeysInput.split(",");
        int[] keys = new int[xteaKeys.length];
        for (int key = 0; key < keys.length; key++) {
            keys[key] = Integer.parseInt(xteaKeys[key].replace(" ", "").trim());
            System.out.println(keys[key]);
        }
        Xtea xtea = new Xtea(keys);
        if (keys.length == 4) {
            try {
                if (cacheLibrary.data(5, selectedArchive, 0, keys) != null) {
                    FileOutputStream fileOutputStream = new FileOutputStream("./" + selectedArchive + ".dat");
                    cacheOperationInfo.setText("Successfully decoded! The key combination was valid.");
                    fileOutputStream.write(xtea.decrypt(cacheLibrary.data(5, selectedArchive, 0, keys)));
                    fileOutputStream.flush();
                    fileOutputStream.close();
                    JOptionPane.showMessageDialog(this, "Successfully decoded! The key combination was valid.");
                } else {
                    cacheOperationInfo.setText("ERROR: Incorrect key combination. The file was not decoded.");
                    JOptionPane.showMessageDialog(this, "Sorry, the keys combination was incorrect. Please try again.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            cacheOperationInfo.setText("ERROR: Incorrect key combination. The file was not decoded.");
            JOptionPane.showMessageDialog(this, "Sorry, the key combination was incorrect. Please try again.");
        }
    }

    private void quickTestSound() {
        new Thread(() -> {
            MidiPcmStream[] midiPcmStreams = initMidiPcmStreams(AppConstants.customSoundFontsPath);
            DevicePcmPlayer[] devicePcmPlayers = initDevicePcmPlayers(midiPcmStreams);
            while (true) {
                playDevicePcmPlayers(devicePcmPlayers);
            }
        }).start();
    }

    private MidiPcmStream[] initMidiPcmStreams(String soundFontsPath) {

        MidiPcmStream[] midiPcmStreams = new MidiPcmStream[2];

        for (int index = 0; index < midiPcmStreams.length; index++) {
            midiPcmStreams[index] = new MidiPcmStream();
            midiPcmStreams[index].setInitialPatch(9, 128);
            MusicTrack musicTrack = null;
            if (AppConstants.currentSongName.contains("(Custom)")) {
                musicTrack = MusicTrack.setTrack(AppConstants.midiMusicFileBytes);
            }
            else {
                try {
                    if (Integer.parseInt(AppConstants.currentSongName) > -1) {
                        musicTrack = MusicTrack.readTrack(cacheLibrary.index(AppConstants.currentMusicIndex), Integer.parseInt(AppConstants.currentSongName), 0);
                    }
                } catch (NumberFormatException e) {
                    musicTrack = MusicTrack.readTrackFromString(cacheLibrary.index(AppConstants.currentMusicIndex), AppConstants.currentSongName);
                }
            }

            SoundCache soundCache = new SoundCache(cacheLibrary.index(4), cacheLibrary.index(14));

            if (musicTrack != null && midiPcmStreams[index].loadMusicTrack(musicTrack, cacheLibrary.index(15), soundCache, 0)) {
                midiPcmStreams[index].setPcmStreamVolume(AppConstants.volumeLevel);
                midiPcmStreams[index].setMusicTrack(musicTrack, false);
                midiPcmStreams[index].loadSoundFonts(soundFontsPath, index);
            }
        }
        return midiPcmStreams;
    }

    private DevicePcmPlayer[] initDevicePcmPlayers(MidiPcmStream[] midiPcmStreams) {

        DevicePcmPlayer[] devicePcmPlayers = new DevicePcmPlayer[2];

        try {
            for (int index = 0; index < devicePcmPlayers.length; index++) {
                devicePcmPlayers[index] = new DevicePcmPlayer();
                devicePcmPlayers[index].init();
                devicePcmPlayers[index].setStream(midiPcmStreams[index]);
                devicePcmPlayers[index].open(2048);
                devicePcmPlayers[index].samples = new int[512];
            }
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
        return devicePcmPlayers;
    }

    private void playDevicePcmPlayers(DevicePcmPlayer[] devicePcmPlayers) {
        devicePcmPlayers[0].fill(devicePcmPlayers[0].samples, 256);
        devicePcmPlayers[1].fill(devicePcmPlayers[1].samples, 256);
        devicePcmPlayers[0].write();
        devicePcmPlayers[1].write();
    }

    private static class FieldActionListener implements ActionListener {

        JTextField currentField;

        public FieldActionListener(JTextField jTextField) {
            currentField = jTextField;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            currentField.setText(currentField.getText());
        }
    }
}
