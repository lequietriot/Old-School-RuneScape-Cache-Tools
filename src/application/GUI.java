package application;

import application.constants.AppConstants;
import com.sun.media.sound.SF2Soundbank;
import decoders.*;
import encoders.MidiEncoder;
import encoders.VorbisEncoder;
import org.displee.CacheLibrary;
import org.displee.cache.index.Index;
import org.displee.cache.index.archive.Archive;
import runescape.*;

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
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;

public class GUI extends JFrame {

    public static CacheLibrary cacheLibrary;

    static DevicePcmPlayer devicePcmPlayer;

    static MidiPcmStream midiPcmStream;

    static JPanel contentPanel;

    JTextField songNameInput;

    JLabel cacheOperationInfo;

    DefaultMutableTreeNode cacheNode;

    DefaultMutableTreeNode indexNode;

    DefaultMutableTreeNode archiveNode;

    DefaultMutableTreeNode fileNode;

    public int selectedIndex;
    public int[] selectedIndices;
    public int selectedArchive;
    public int[] selectedArchives;
    public int selectedFile;
    public int[] selectedFiles;

    private static final File defaultCachePath;

    static {
        defaultCachePath = new File(System.getProperty("user.home") + File.separator + "jagexcache" + File.separator + "oldschool" + File.separator + "LIVE");
    }

    GUI() {
        super("Old School RuneScape Cache Tools");
        setSize(620, 480);
        setLocationRelativeTo(null);
        setResizable(false);
        setIconImage(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            SwingUtilities.updateComponentTreeUI(this);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        JMenuBar jMenuBar = new JMenuBar();
        setJMenuBar(jMenuBar);

        JMenu fileMenu = new JMenu("File");
        jMenuBar.add(fileMenu);

        JMenuItem loadCache = new JMenuItem("Load Cache");
        loadCache.addActionListener(e -> chooseCacheFolder());
        fileMenu.add(loadCache);

        JMenu encoderMenu = new JMenu("Data Encoders");
        jMenuBar.add(encoderMenu);

        JMenuItem midiEncoder = new JMenuItem("MIDI Encoder");
        midiEncoder.addActionListener(e -> new MidiEncoder(this));
        encoderMenu.add(midiEncoder);

        JMenuItem vorbisEncoder = new JMenuItem("Vorbis Encoder");
        vorbisEncoder.addActionListener(e -> new VorbisEncoder(this));
        encoderMenu.add(vorbisEncoder);

        JMenu decoderMenu = new JMenu("Data Decoders");
        jMenuBar.add(decoderMenu);

        JMenuItem midiDecoder = new JMenuItem("MIDI Decoder");
        midiDecoder.addActionListener(e -> new MidiDecoder(this));
        decoderMenu.add(midiDecoder);

        JMenuItem modelDecoderOS = new JMenuItem("OSRS Model Decoder");
        modelDecoderOS.addActionListener(e -> new ModelDecoderOS(this));
        decoderMenu.add(modelDecoderOS);

        JMenuItem modelDecoderHD = new JMenuItem("RSHD Model Decoder");
        modelDecoderHD.addActionListener(e -> new ModelDecoderHD(this));
        decoderMenu.add(modelDecoderHD);

        JMenuItem vorbisDecoder = new JMenuItem("Vorbis Decoder");
        vorbisDecoder.addActionListener(e -> new VorbisDecoder(this));
        decoderMenu.add(vorbisDecoder);

        JMenuItem soundBankDecoder = new JMenuItem("Sound Bank Decoder");
        soundBankDecoder.addActionListener(e -> new SoundBankDecoder(this));
        decoderMenu.add(soundBankDecoder);

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

        JMenuItem test = new JMenuItem("Test new tool");
        test.addActionListener(e -> testTool());
        //toolsMenu.add(test);

        JLabel loadCacheLabel = new JLabel("Please load your cache from the File menu to begin!");
        loadCacheLabel.setVerticalAlignment(SwingConstants.CENTER);
        loadCacheLabel.setHorizontalAlignment(SwingConstants.CENTER);

        contentPanel = new JPanel();
        contentPanel.setLayout(new BorderLayout());
        contentPanel.add(loadCacheLabel);

        setContentPane(contentPanel);
        loadCache(defaultCachePath);
    }

    private void chooseMusicTrack() {

        JSplitPane musicPlayerMasterPanel = new JSplitPane();

        JPanel musicPlayerPanel = new JPanel();

        JLabel songInfoLabel = new JLabel("Current Loaded Song:");

        songNameInput = new JTextField("", 30);
        songNameInput.setText(AppConstants.currentSongName);
        songNameInput.addActionListener(e -> updateCurrentSong(songNameInput));

        JButton loadMusicButton = new JButton("Load MIDI");
        loadMusicButton.addActionListener(e -> loadSong());

        JButton musicPlayButton = new JButton("Play");
        musicPlayButton.addActionListener(e -> playSong());

        JButton musicStopButton = new JButton("Stop");
        musicStopButton.addActionListener(e -> stopSong());

        JButton musicRenderButton = new JButton("Render to File");
        musicRenderButton.addActionListener(e -> renderSong());

        JButton exitMusicPlayerButton = new JButton("Exit Music Player");
        exitMusicPlayerButton.addActionListener(e -> exitPlayer());

        musicPlayerPanel.add(songInfoLabel);
        musicPlayerPanel.add(songNameInput);
        musicPlayerPanel.add(loadMusicButton);
        musicPlayerPanel.add(musicPlayButton);
        musicPlayerPanel.add(musicStopButton);
        musicPlayerPanel.add(musicRenderButton);
        musicPlayerPanel.add(exitMusicPlayerButton);

        JPanel settingsPanel = new JPanel();

        JLabel musicIndexLabel = new JLabel("Cache Index Selection");

        String[] musicIndices = new String[]{"Music Tracks (6)", "Music Jingles (11)"};
        JComboBox<String> musicIndexComboBox = new JComboBox<>(musicIndices);
        musicIndexComboBox.addActionListener(e -> changeIndex(musicIndexComboBox));

        JLabel sampleRateLabel = new JLabel("Sample Rate");

        JTextField sampleRateSetter = new JTextField("" + AppConstants.sampleRate, 8);
        sampleRateSetter.addActionListener(e -> updateSampleRate(sampleRateSetter));

        JLabel volumeLevelLabel = new JLabel("Volume Level");

        JTextField volumeLevelSetter = new JTextField("" + AppConstants.volumeLevel, 8);
        volumeLevelSetter.addActionListener(e -> updateVolumeLevel(volumeLevelSetter));

        JCheckBox stereoSoundSetter = new JCheckBox("Use Stereo Sound");
        stereoSoundSetter.setSelected(AppConstants.stereo);
        stereoSoundSetter.addActionListener(e -> updateStereoMode(stereoSoundSetter));

        JCheckBox shuffleModeSetter = new JCheckBox("Shuffle Music");
        shuffleModeSetter.setSelected(AppConstants.shuffle);
        shuffleModeSetter.addActionListener(e -> updateShuffleMode(shuffleModeSetter));

        JLabel customSoundFontLabel = new JLabel("SoundFont Path");

        JTextField customSoundFontSetter = new JTextField("" + AppConstants.customSoundFontPath, 16);
        customSoundFontSetter.addActionListener(e -> updateCurrentSoundFont(customSoundFontSetter));

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

        musicPlayerMasterPanel.setLeftComponent(settingsPanel);
        musicPlayerMasterPanel.setRightComponent(musicPlayerPanel);
        musicPlayerMasterPanel.setDividerLocation(160);
        musicPlayerMasterPanel.setEnabled(false);

        setContentPane(musicPlayerMasterPanel);
        revalidate();
    }

    private void useMusicPort() {
        new Thread(() -> {
            if (midiPcmStream != null) {
                stopSong();
            }

            MusicTrack musicTrack = new MusicTrack();
            musicTrack.table = new NodeHashTable(cacheLibrary.getIndex(15).getArchives().length);
            for (Archive archive : cacheLibrary.getIndex(15).getArchives()) {
                byte[] bytes = new byte[128];
                Arrays.fill(bytes, (byte) 1);
                musicTrack.table.put(new ByteArrayNode(bytes), archive.getId());
            }

            midiPcmStream = new MidiPcmStream();
            midiPcmStream.method4761(9, 128);
            midiPcmStream.loadMusicTrack(musicTrack, cacheLibrary.getIndex(15), new SoundCache(cacheLibrary.getIndex(4), cacheLibrary.getIndex(14)), 0);
            midiPcmStream.setPcmStreamVolume(255);

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
                    AppConstants.currentMusicFolder = selected.getParentFile().listFiles();
                    AppConstants.midiMusicFileBytes = Files.readAllBytes(Paths.get(selected.toURI()));
                    AppConstants.currentSongName = selected.getName().replace(".mid", "").trim() + " (Custom)";
                    songNameInput.setText(AppConstants.currentSongName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void playSong() {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(System.getProperty("user.home") + File.separator + "Documents" + File.separator + "Current Song.txt");
            if (AppConstants.currentSongName.contains(" - ")) {
                int index = AppConstants.currentSongName.lastIndexOf(" - ");
                String name = AppConstants.currentSongName.substring(index).replace(".mid", "").replace(" - ", "").trim();
                if (name.contains("(Custom)")) {
                    name = name.replace("(Custom)", "").trim();
                }
                fileOutputStream.write(name.getBytes(StandardCharsets.UTF_8));
            }
            else {
                String name = AppConstants.currentSongName.replace(".mid","").trim();
                if (name.contains("(Custom)")) {
                    name = name.replace("(Custom)", "").trim();
                }
                fileOutputStream.write(name.getBytes(StandardCharsets.UTF_8));
            }
            initSoundEngine();
        } catch (IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    private void stopSong() {
        if (midiPcmStream != null) {
            while (midiPcmStream.getPcmStreamVolume() != 0) {
                if (midiPcmStream.getPcmStreamVolume() == 0) {
                    break;
                } else {
                    midiPcmStream.setPcmStreamVolume(midiPcmStream.getPcmStreamVolume() - 1);
                    try {
                        Thread.sleep((long) (0.025 * AppConstants.volumeLevel));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            midiPcmStream = null;
            devicePcmPlayer.setStream(null);
        }
    }

    private void renderSong() {
        try {
            if (midiPcmStream == null) {
                midiPcmStream = new MidiPcmStream();
                midiPcmStream.method4761(9, 128);

                AppConstants.pcmPlayerProvider = new DevicePcmPlayerProvider();
                devicePcmPlayer = (DevicePcmPlayer) AppConstants.pcmPlayerProvider.player();

                MusicTrack musicTrack = new MusicTrack();
                if (AppConstants.currentSongName.contains("(Custom)")) {
                    musicTrack = MusicTrack.setTrack(AppConstants.midiMusicFileBytes);
                } else {
                    try {
                        if (Integer.parseInt(AppConstants.currentSongName) != -1) {
                            musicTrack = MusicTrack.readTrack(cacheLibrary.getIndex(AppConstants.currentMusicIndex), Integer.parseInt(AppConstants.currentSongName), 0);
                        }
                    } catch (NumberFormatException e) {
                        musicTrack = MusicTrack.readTrackFromString(cacheLibrary.getIndex(AppConstants.currentMusicIndex), AppConstants.currentSongName);
                    }
                }

                SoundCache soundCache = new SoundCache(cacheLibrary.getIndex(4), cacheLibrary.getIndex(14));

                if (musicTrack != null && midiPcmStream.loadMusicTrack(musicTrack, cacheLibrary.getIndex(15), soundCache, 0)) {
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
                        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, new File(outputFilePath + File.separator + AppConstants.currentSongName + ".wav"));
                        if (madeDirectory) {
                            JOptionPane.showMessageDialog(getContentPane(), "An output directory in your current cache folder was created!\n" + AppConstants.currentSongName + " was then rendered successfully to your output folder!");
                        } else {
                            JOptionPane.showMessageDialog(getContentPane(), AppConstants.currentSongName + " was rendered successfully to your output folder!");
                        }
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
                    midiPcmStream.method4761(9, 128);

                    AppConstants.pcmPlayerProvider = new DevicePcmPlayerProvider();
                    devicePcmPlayer = (DevicePcmPlayer) AppConstants.pcmPlayerProvider.player();

                    MusicTrack musicTrack = null;
                    if (AppConstants.currentSongName.contains("(Custom)")) {
                        musicTrack = MusicTrack.setTrack(AppConstants.midiMusicFileBytes);
                    }
                    else {
                        try {
                            if (Integer.parseInt(AppConstants.currentSongName) > -1) {
                                musicTrack = MusicTrack.readTrack(cacheLibrary.getIndex(AppConstants.currentMusicIndex), Integer.parseInt(AppConstants.currentSongName), 0);
                            }
                        } catch (NumberFormatException e) {
                            musicTrack = MusicTrack.readTrackFromString(cacheLibrary.getIndex(AppConstants.currentMusicIndex), AppConstants.currentSongName);
                        }
                    }

                    SoundCache soundCache = new SoundCache(cacheLibrary.getIndex(4), cacheLibrary.getIndex(14));

                    if (musicTrack != null && midiPcmStream.loadMusicTrack(musicTrack, cacheLibrary.getIndex(15), soundCache, 0)) {
                        midiPcmStream.setPcmStreamVolume(AppConstants.volumeLevel);
                        midiPcmStream.setMusicTrack(musicTrack, false);
                        if (AppConstants.usingSoundFont) {
                            midiPcmStream.loadSoundFont(new SF2Soundbank(new File(AppConstants.customSoundFontPath)), -1);
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
                                if (AppConstants.currentSongName.contains("(Custom)")) {
                                    File selected = AppConstants.currentMusicFolder[(int) (Math.random() * AppConstants.currentMusicFolder.length)];
                                    if (selected.getName().endsWith(".mid")) {
                                        AppConstants.midiMusicFileBytes = Files.readAllBytes(Paths.get(selected.toURI()));
                                        AppConstants.currentSongName = selected.getName().replace(".mid", "").trim() + " (Custom)";
                                        songNameInput.setText(AppConstants.currentSongName);
                                    }
                                }
                                else {
                                    AppConstants.currentSongName = String.valueOf((int) (Math.random() * cacheLibrary.getIndex(AppConstants.currentMusicIndex).getArchives().length));
                                }
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
                cacheLibrary = new CacheLibrary(cache.getPath());
                initFileViewer();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.revalidate();
    }

    private void initFileViewer() {

        contentPanel.removeAll();

        cacheOperationInfo = new JLabel("Loaded cache from path - " + cacheLibrary.getPath());
        cacheOperationInfo.setFont(cacheOperationInfo.getFont().deriveFont(Font.BOLD, 12));

        JSplitPane splitCacheViewPane = new JSplitPane();

        buildTreeNode();

        JTree cacheTree = new JTree();
        cacheTree.setModel(new DefaultTreeModel(cacheNode));

        JScrollPane cacheScrollPane = new JScrollPane(cacheTree);
        cacheScrollPane.setViewportView(cacheTree);

        JSplitPane cacheInfoSplitPanel = new JSplitPane();

        JPanel cacheInfoPanel = new JPanel();
        cacheInfoPanel.setLayout(new GridLayout());

        JPanel cacheOperationsButtonPanel = new JPanel();
        cacheOperationsButtonPanel.setLayout(new FlowLayout());

        JButton addFilesButton = new JButton("Add Files");
        addFilesButton.addActionListener(e -> addCacheFiles());

        JButton exportFilesButton = new JButton("Export Files");
        exportFilesButton.addActionListener(e -> exportFilesData());

        JButton removeArchiveButton = new JButton("Remove Archive");
        removeArchiveButton.addActionListener(e -> removeArchiveFile());

        JButton removeFileButton = new JButton("Remove File");
        removeFileButton.addActionListener(e -> removeCacheFile());

        JButton setArchiveNameHashButton = new JButton("Set Archive name hash");
        setArchiveNameHashButton.addActionListener(e -> setCacheArchiveNameHash());

        JButton setArchiveNameButton = new JButton("Set Archive name");
        setArchiveNameButton.addActionListener(e -> setCacheArchiveName());

        JButton exportAllDataButton = new JButton("Export all Index data");
        exportAllDataButton.addActionListener(e -> dumpAllDataFolders());

        cacheOperationsButtonPanel.add(addFilesButton);
        cacheOperationsButtonPanel.add(exportFilesButton);
        cacheOperationsButtonPanel.add(removeArchiveButton);
        cacheOperationsButtonPanel.add(removeFileButton);
        cacheOperationsButtonPanel.add(setArchiveNameHashButton);
        cacheOperationsButtonPanel.add(setArchiveNameButton);
        cacheOperationsButtonPanel.add(exportAllDataButton);

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
                                    "Multiple Selection", "N/A"
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

                    Object[][] indexFields = new Object[][]{

                            new Object[]{
                                    "Cache Index ID", selectedIndex
                            },

                            new Object[]{
                                    "Amount of Archives", cacheLibrary.getIndex(selectedIndex).getArchives().length
                            },

                            new Object[]{
                                    "Index CRC Value", cacheLibrary.getIndex(selectedIndex).getCRC()
                            },

                            new Object[]{
                                    "Index Version", cacheLibrary.getIndex(selectedIndex).getVersion()
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
                                    "Cache Index ID", selectedIndex
                            },

                            new Object[]{
                                    "Archive ID", selectedArchive
                            },

                            new Object[]{
                                    "Archive Name Hash", cacheLibrary.getIndex(selectedIndex).getArchive(selectedArchive).getName()
                            },

                            new Object[]{
                                    "Amount of Files", cacheLibrary.getIndex(selectedIndex).getArchive(selectedArchive).getFiles().length
                            },

                            new Object[]{
                                    "Archive CRC Value", cacheLibrary.getIndex(selectedIndex).getArchive(selectedArchive).getCRC()
                            },

                            new Object[]{
                                    "Archive Revision", cacheLibrary.getIndex(selectedIndex).getArchive(selectedArchive).getRevision()
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
                                    "Cache Index ID", selectedIndex
                            },

                            new Object[]{
                                    "Archive ID", selectedArchive
                            },

                            new Object[]{
                                    "File ID", selectedFile
                            },

                            new Object[]{
                                    "File Name Hash", cacheLibrary.getIndex(selectedIndex).getArchive(selectedArchive).getFile(selectedFile).getName()
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
        });

        cacheInfoPanel.add(infoTable);

        cacheInfoSplitPanel.setRightComponent(cacheOperationsButtonPanel);
        cacheInfoSplitPanel.setLeftComponent(cacheInfoPanel);
        cacheInfoSplitPanel.setDividerLocation(260);
        cacheInfoSplitPanel.setEnabled(false);

        splitCacheViewPane.setLeftComponent(cacheScrollPane);
        splitCacheViewPane.setRightComponent(cacheInfoSplitPanel);
        splitCacheViewPane.setDividerLocation(140);
        splitCacheViewPane.setEnabled(false);

        contentPanel.add(splitCacheViewPane, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.add(cacheOperationInfo, BorderLayout.SOUTH);
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
                    if (cacheLibrary.getIndex(currentIndex) != null) {
                        try {
                            int fileToAdd = 0;
                            String trimmedName = file.getName().substring(0, file.getName().indexOf(".")).trim();
                            if (trimmedName.contains("-")) {
                                fileToAdd = Integer.parseInt(trimmedName.substring(trimmedName.indexOf("-")).trim().replace("-", "").trim());
                                trimmedName = trimmedName.substring(0, trimmedName.indexOf("-")).trim();
                            }
                            if (cacheLibrary.getIndex(currentIndex).getArchive(Integer.parseInt(trimmedName)) != null) {
                                cacheLibrary.getIndex(currentIndex).getArchive(Integer.parseInt(trimmedName)).addFile(fileToAdd, Files.readAllBytes(file.toPath()));
                            }
                            else {
                                cacheLibrary.getIndex(currentIndex).addArchive(Integer.parseInt(trimmedName)).addFile(fileToAdd, Files.readAllBytes(file.toPath()));
                            }
                            if (cacheLibrary.getIndex(currentIndex).update()) {
                                loadCache(new File(cacheLibrary.getPath()));
                            }
                            cacheOperationInfo.setText("Successfully added Archive " + Integer.parseInt(trimmedName) + ", File " + fileToAdd + " to Index " + currentIndex + "!");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            cacheOperationInfo.setText("Operation completed successfully.");
            this.revalidate();
        }).start();
    }

    private void exportFilesData() {
        new Thread(() -> {
            JFileChooser folderChooser = new JFileChooser();
            folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (folderChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File selected = folderChooser.getSelectedFile();
                try {
                    if (selectedArchives != null) {
                        for (int archive = selectedArchives[0]; archive < selectedArchives[selectedArchives.length - 1]; archive++) {
                            if (selectedFiles != null) {
                                for (int file = selectedFiles[0]; file < selectedFiles[selectedFiles.length - 1]; file++) {
                                    File fileData = new File(selected.getPath() + File.separator + selectedArchives[archive] + "-" + selectedFiles[file] + ".dat");
                                    FileOutputStream fileOutputStream = new FileOutputStream(fileData);
                                    if (cacheLibrary.getIndex(selectedIndex) != null) {
                                        if (cacheLibrary.getIndex(selectedIndex).getArchive(selectedArchives[archive]) != null) {
                                            if (cacheLibrary.getIndex(selectedIndex).getArchive(selectedArchives[archive]).getFile(selectedFiles[file]) != null) {
                                                if (cacheLibrary.getIndex(selectedIndex).getArchive(selectedArchives[archive]).getFile(selectedFiles[file]).getData() != null) {
                                                    fileOutputStream.write(cacheLibrary.getIndex(selectedIndex).getArchive(selectedArchives[archive]).getFile(selectedFiles[file]).getData());
                                                    fileOutputStream.flush();
                                                    fileOutputStream.close();
                                                    cacheOperationInfo.setText("Successfully exported Index " + selectedIndex + ", Archive " + selectedArchives[archive] + ", File " + selectedFiles[file] + " to " + selected.getPath() + "!");
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else {
                        File fileData = new File(selected.getPath() + File.separator + selectedArchive + "-" + selectedFile + ".dat");
                        FileOutputStream fileOutputStream = new FileOutputStream(fileData);
                        if (cacheLibrary.getIndex(selectedIndex) != null) {
                            if (cacheLibrary.getIndex(selectedIndex).getArchive(selectedArchive) != null) {
                                if (cacheLibrary.getIndex(selectedIndex).getArchive(selectedArchive).getFile(selectedFile) != null) {
                                    if (cacheLibrary.getIndex(selectedIndex).getArchive(selectedArchive).getFile(selectedFile).getData() != null) {
                                        fileOutputStream.write(cacheLibrary.getIndex(selectedIndex).getArchive(selectedArchive).getFile(selectedFile).getData());
                                        fileOutputStream.flush();
                                        fileOutputStream.close();
                                        cacheOperationInfo.setText("Successfully exported Index " + selectedIndex + ", Archive " + selectedArchive + ", File " + selectedFile + " to " + selected.getPath() + "!");
                                    }
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            cacheOperationInfo.setText("Operation completed successfully.");
            this.revalidate();
        }).start();
    }

    private void setCacheArchiveNameHash() {
        String fileNameToSet = JOptionPane.showInputDialog("Set Archive Name Hash", "");
        if (fileNameToSet != null) {
            if (cacheLibrary.getIndex(selectedIndex).getArchive(selectedArchive) != null) {
                Archive renamedArchive = cacheLibrary.getIndex(selectedIndex).getArchive(selectedArchive);
                renamedArchive.setName(Integer.parseInt(fileNameToSet));
                cacheLibrary.getIndex(selectedIndex).addArchive(renamedArchive, true, true, renamedArchive.getId());
                if (cacheLibrary.getIndex(selectedIndex).update()) {
                    loadCache(new File(cacheLibrary.getPath()));
                }
            }
        }
        cacheOperationInfo.setText("Operation completed successfully.");
        this.revalidate();
    }

    private void setCacheArchiveName() {
        String fileNameToSet = JOptionPane.showInputDialog("Set Archive Name", "");
        if (fileNameToSet != null) {
            if (cacheLibrary.getIndex(selectedIndex).getArchive(selectedArchive) != null) {
                Archive renamedArchive = cacheLibrary.getIndex(selectedIndex).getArchive(selectedArchive);
                renamedArchive.setName(fileNameToSet.toLowerCase().hashCode());
                cacheLibrary.getIndex(selectedIndex).addArchive(renamedArchive, true, true, renamedArchive.getId());
                if (cacheLibrary.getIndex(selectedIndex).update()) {
                    loadCache(new File(cacheLibrary.getPath()));
                }
            }
        }
        cacheOperationInfo.setText("Operation completed successfully.");
        this.revalidate();
    }

    private void removeArchiveFile() {
        cacheLibrary.getIndex(selectedIndex).removeArchive(selectedArchive);
        if (cacheLibrary.getIndex(selectedIndex).update()) {
            loadCache(new File(cacheLibrary.getPath()));
            JOptionPane.showMessageDialog(this, "Cache Archive " + selectedArchive + " has been removed.");
        }
        cacheOperationInfo.setText("Operation completed successfully.");
        this.revalidate();
    }

    private void removeCacheFile() {
        cacheLibrary.getIndex(selectedIndex).getArchive(selectedArchive).removeFile(selectedFile);
        if (cacheLibrary.getIndex(selectedIndex).update()) {
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
                JOptionPane.showMessageDialog(this, "Created an Index " + selectedIndex + " folder.");
            }
            for (Archive archive : cacheLibrary.getIndex(selectedIndex).getArchives()) {
                try {
                    for (org.displee.cache.index.archive.file.File archiveFileData : cacheLibrary.getIndex(selectedIndex).getArchive(archive.getId()).getFiles()) {
                        if (archiveFileData.getData() != null) {
                            File fileData = new File(indexDirectory + File.separator + archive.getId() + File.separator + archiveFileData.getId() + ".dat");
                            if (new File(indexDirectory + File.separator + archive.getId()).mkdirs()) {
                                System.out.println("made directory for file");
                            }
                            FileOutputStream fileOutputStream = new FileOutputStream(fileData);
                            DataOutputStream dataOutputStream = new DataOutputStream(fileOutputStream);
                            dataOutputStream.write(archiveFileData.getData());
                            dataOutputStream.flush();
                            dataOutputStream.close();
                            }
                        }
                    } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void dumpAllData() {
        JFileChooser folderChooser = new JFileChooser();
        folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (folderChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selected = folderChooser.getSelectedFile();
            File indexDirectory = new File(selected + File.separator + "Index " + selectedIndex);
            if (indexDirectory.mkdirs()) {
                JOptionPane.showMessageDialog(this, "Created an Index " + selectedIndex + " folder.");
            }
            for (Archive archive : cacheLibrary.getIndex(selectedIndex).getArchives()) {
                try {
                    for (org.displee.cache.index.archive.file.File archiveFileData : cacheLibrary.getIndex(selectedIndex).getArchive(archive.getId()).getFiles()) {
                        if (archiveFileData.getData() != null) {
                            if (archiveFileData.getId() != 0) {
                                File fileData = new File(indexDirectory + File.separator + archive.getId() + "-" + archiveFileData.getId() + ".dat");
                                FileOutputStream fileOutputStream = new FileOutputStream(fileData);
                                fileOutputStream.write(archiveFileData.getData());
                                fileOutputStream.flush();
                                fileOutputStream.close();
                            } else {
                                File archiveData = new File(indexDirectory + File.separator + archive.getId() + "-0" + ".dat");
                                FileOutputStream archiveOutputStream = new FileOutputStream(archiveData);
                                archiveOutputStream.write(archiveFileData.getData());
                                archiveOutputStream.flush();
                                archiveOutputStream.close();
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void buildTreeNode() {
        cacheNode = new DefaultMutableTreeNode("Main Cache");
        if (cacheLibrary.getIndices() != null) {
            for (Index index : cacheLibrary.getIndices()) {
                indexNode = new DefaultMutableTreeNode(index);
                cacheNode.add(indexNode);
                if (index.getArchives() != null) {
                    for (Archive archive : index.getArchives()) {
                        archiveNode = new DefaultMutableTreeNode(archive);
                        indexNode.add(archiveNode);
                        if (archive.getFiles() != null) {
                            for (org.displee.cache.index.archive.file.File file : archive.getFiles()) {
                                fileNode = new DefaultMutableTreeNode(file);
                                archiveNode.add(fileNode);
                            }
                        }
                    }
                }
            }
        }
        this.revalidate();
    }

    private void testTool() {
        this.revalidate();
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
            midiPcmStreams[index].method4761(9, 128);
            MusicTrack musicTrack = null;
            if (AppConstants.currentSongName.contains("(Custom)")) {
                musicTrack = MusicTrack.setTrack(AppConstants.midiMusicFileBytes);
            }
            else {
                try {
                    if (Integer.parseInt(AppConstants.currentSongName) > -1) {
                        musicTrack = MusicTrack.readTrack(cacheLibrary.getIndex(AppConstants.currentMusicIndex), Integer.parseInt(AppConstants.currentSongName), 0);
                    }
                } catch (NumberFormatException e) {
                    musicTrack = MusicTrack.readTrackFromString(cacheLibrary.getIndex(AppConstants.currentMusicIndex), AppConstants.currentSongName);
                }
            }

            SoundCache soundCache = new SoundCache(cacheLibrary.getIndex(4), cacheLibrary.getIndex(14));

            if (musicTrack != null && midiPcmStreams[index].loadMusicTrack(musicTrack, cacheLibrary.getIndex(15), soundCache, 0)) {
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
}
