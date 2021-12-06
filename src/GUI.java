import org.displee.CacheLibrary;
import org.displee.cache.index.Index;
import org.displee.cache.index.archive.Archive;
import runescape.*;

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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public class GUI extends JFrame {

    static CacheLibrary cacheLibrary;

    static DevicePcmPlayer devicePcmPlayer;

    static MidiPcmStream midiPcmStream;

    JPanel contentPanel;

    JTextField songNameInput;

    DefaultMutableTreeNode cacheNode;

    DefaultMutableTreeNode indexNode;

    DefaultMutableTreeNode archiveNode;

    DefaultMutableTreeNode fileNode;

    int selectedIndex;
    int selectedArchive;
    int selectedFile;

    private static final File defaultCachePath;

    static {
        defaultCachePath = new File(System.getProperty("user.home") + File.separator + "jagexcache" + File.separator + "oldschool" + File.separator + "LIVE");
    }

    GUI() {
        super("Old School RuneScape Cache Tools");
        setSize(600, 480);
        setLocationRelativeTo(null);
        setResizable(false);
        setIconImage(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JMenuBar jMenuBar = new JMenuBar();
        setJMenuBar(jMenuBar);

        JMenu fileMenu = new JMenu("File");
        jMenuBar.add(fileMenu);

        JMenuItem loadCache = new JMenuItem("Load Cache");
        loadCache.addActionListener(e -> chooseCacheFolder());
        fileMenu.add(loadCache);

        JMenu toolsMenu = new JMenu("Tools");
        jMenuBar.add(toolsMenu);

        JMenuItem musicPlayer = new JMenuItem("Music Player");
        musicPlayer.addActionListener(e -> chooseMusicTrack());
        toolsMenu.add(musicPlayer);

        JMenuItem enumDumper = new JMenuItem("Enum Printer");
        enumDumper.addActionListener(e -> printEnums());
        //toolsMenu.add(enumDumper);

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
        songNameInput.setText(SoundConstants.currentSongName);
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

        String[] musicIndices = new String[]{"Music Tracks (6)", "Fanfares/Jingles (11)"};
        JComboBox<String> musicIndexComboBox = new JComboBox<>(musicIndices);
        musicIndexComboBox.addActionListener(e -> changeIndex(musicIndexComboBox));

        JLabel sampleRateLabel = new JLabel("Sample Rate");

        JTextField sampleRateSetter = new JTextField("" + SoundConstants.sampleRate, 8);
        sampleRateSetter.addActionListener(e -> updateSampleRate(sampleRateSetter));

        JLabel volumeLevelLabel = new JLabel("Volume Level");

        JTextField volumeLevelSetter = new JTextField("" + SoundConstants.volumeLevel, 8);
        volumeLevelSetter.addActionListener(e -> updateVolumeLevel(volumeLevelSetter));

        JCheckBox stereoSoundSetter = new JCheckBox("Use Stereo Sound");
        stereoSoundSetter.setSelected(SoundConstants.stereo);
        stereoSoundSetter.addActionListener(e -> updateStereoMode(stereoSoundSetter));

        settingsPanel.add(musicIndexLabel);
        settingsPanel.add(musicIndexComboBox);
        settingsPanel.add(sampleRateLabel);
        settingsPanel.add(sampleRateSetter);
        settingsPanel.add(volumeLevelLabel);
        settingsPanel.add(volumeLevelSetter);
        settingsPanel.add(stereoSoundSetter);

        musicPlayerMasterPanel.setLeftComponent(settingsPanel);
        musicPlayerMasterPanel.setRightComponent(musicPlayerPanel);
        musicPlayerMasterPanel.setDividerLocation(200);
        musicPlayerMasterPanel.setEnabled(false);

        setContentPane(musicPlayerMasterPanel);
        revalidate();
    }

    private void changeIndex(JComboBox<String> musicIndexComboBox) {
        if (musicIndexComboBox.getSelectedItem() == "Music Tracks (6)") {
            SoundConstants.selectedMusicIndex = 6;
        }
        if (musicIndexComboBox.getSelectedItem() == "Fanfares/Jingles (11)") {
            SoundConstants.selectedMusicIndex = 11;
        }
    }

    private void updateCurrentSong(JTextField songName) {
        if (songName.getText() != null) {
            SoundConstants.currentSongName = songName.getText();
        }
    }

    private void loadSong() {
        JFileChooser chooseMidi = new JFileChooser();
        chooseMidi.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (chooseMidi.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selected = chooseMidi.getSelectedFile();
            if (selected.getName().endsWith(".mid")) {
                try {
                    SoundConstants.midiMusicFileBytes = Files.readAllBytes(Paths.get(selected.toURI()));
                    SoundConstants.currentSongName = selected.getName().replace(".mid", "").trim() + " (Custom)";
                    songNameInput.setText(SoundConstants.currentSongName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void playSong() {
        try {
            initSoundEngine();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    private void stopSong() {
        if (midiPcmStream != null) {
            midiPcmStream = null;
            devicePcmPlayer.setStream(null);
        }
    }

    private void renderSong() {
        if (midiPcmStream == null) {
            midiPcmStream = new MidiPcmStream();
            midiPcmStream.method4761(9, 128);

            SoundConstants.pcmPlayerProvider = new DevicePcmPlayerProvider();
            devicePcmPlayer = (DevicePcmPlayer) SoundConstants.pcmPlayerProvider.player();

            MusicTrack musicTrack = new MusicTrack();
            if (SoundConstants.currentSongName.contains("(Custom)")) {
                musicTrack = MusicTrack.setTrack(SoundConstants.midiMusicFileBytes);
            }
            else {
                try {
                    if (Integer.parseInt(SoundConstants.currentSongName) != -1) {
                        musicTrack = MusicTrack.readTrack(cacheLibrary.getIndex(SoundConstants.selectedMusicIndex), Integer.parseInt(SoundConstants.currentSongName), 0);
                    }
                } catch (NumberFormatException e) {
                    musicTrack = MusicTrack.readTrackFromString(cacheLibrary.getIndex(SoundConstants.selectedMusicIndex), SoundConstants.currentSongName);
                }
            }

            SoundCache soundCache = new SoundCache(cacheLibrary.getIndex(4), cacheLibrary.getIndex(14));

            if (musicTrack != null && midiPcmStream.loadMusicTrack(musicTrack, cacheLibrary.getIndex(15), soundCache, 0)) {
                midiPcmStream.setPcmStreamVolume(SoundConstants.volumeLevel);
                midiPcmStream.setMusicTrack(musicTrack, false);
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
                    AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, new File(outputFilePath + File.separator + SoundConstants.currentSongName + ".wav"));
                    if (madeDirectory) {
                        JOptionPane.showMessageDialog(getContentPane(), "An output directory in your current cache folder was created!\n" + SoundConstants.currentSongName + " was then rendered successfully to your output folder!");
                    }
                    else {
                        JOptionPane.showMessageDialog(getContentPane(), SoundConstants.currentSongName + " was rendered successfully to your output folder!");
                    }
                    stopSong();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void exitPlayer() {
        stopSong();
        setContentPane(contentPanel);
        revalidate();
    }

    private static void initSoundEngine() throws LineUnavailableException {
        new Thread(() -> {
            try {
                if (midiPcmStream == null) {
                    midiPcmStream = new MidiPcmStream();
                    midiPcmStream.method4761(9, 128);

                    SoundConstants.pcmPlayerProvider = new DevicePcmPlayerProvider();
                    devicePcmPlayer = (DevicePcmPlayer) SoundConstants.pcmPlayerProvider.player();

                    MusicTrack musicTrack = new MusicTrack();
                    if (SoundConstants.currentSongName.contains("(Custom)")) {
                        musicTrack = MusicTrack.setTrack(SoundConstants.midiMusicFileBytes);
                    }
                    else {
                        try {
                            if (Integer.parseInt(SoundConstants.currentSongName) != -1) {
                                musicTrack = MusicTrack.readTrack(cacheLibrary.getIndex(SoundConstants.selectedMusicIndex), Integer.parseInt(SoundConstants.currentSongName), 0);
                            }
                        } catch (NumberFormatException e) {
                            musicTrack = MusicTrack.readTrackFromString(cacheLibrary.getIndex(SoundConstants.selectedMusicIndex), SoundConstants.currentSongName);
                        }
                    }

                    SoundCache soundCache = new SoundCache(cacheLibrary.getIndex(4), cacheLibrary.getIndex(14));

                    if (musicTrack != null && midiPcmStream.loadMusicTrack(musicTrack, cacheLibrary.getIndex(15), soundCache, 0)) {
                        midiPcmStream.setPcmStreamVolume(SoundConstants.volumeLevel);
                        midiPcmStream.setMusicTrack(musicTrack, false);
                        devicePcmPlayer.init();
                        devicePcmPlayer.setStream(midiPcmStream);
                        devicePcmPlayer.open(16384);
                        devicePcmPlayer.samples = new int[512];
                        while (midiPcmStream != null && midiPcmStream.isReady()) {
                            devicePcmPlayer.fill(devicePcmPlayer.samples, 256);
                            devicePcmPlayer.write();
                        }
                    }
                }
            } catch (LineUnavailableException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void updateSampleRate(JTextField sampleRateSetter) {
        if (sampleRateSetter.getText() != null) {
            SoundConstants.sampleRate = Integer.parseInt(sampleRateSetter.getText());
        }
    }

    private void updateVolumeLevel(JTextField volumeLevelSetter) {
        if (volumeLevelSetter.getText() != null) {
            SoundConstants.volumeLevel = Integer.parseInt(volumeLevelSetter.getText());
            if (midiPcmStream != null) {
                midiPcmStream.setPcmStreamVolume(SoundConstants.volumeLevel);
            }
        }
    }

    private void updateStereoMode(JCheckBox stereoSoundSetter) {
        SoundConstants.stereo = stereoSoundSetter.isSelected();
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
    }

    private void initFileViewer() {

        contentPanel.removeAll();

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

        JButton addFilesButton = new JButton("Add Files");
        addFilesButton.addActionListener(e -> addCacheFiles());

        JButton replaceFilesButton = new JButton("Replace Files");
        replaceFilesButton.addActionListener(e -> replaceCacheFiles());

        JButton removeFileButton = new JButton("Remove File");
        removeFileButton.addActionListener(e -> removeCacheFile());

        JButton setArchiveNameButton = new JButton("Set Archive Name");
        setArchiveNameButton.addActionListener(e -> setCacheArchiveName());

        JButton exportAllDataButton = new JButton("Export Index data");
        exportAllDataButton.addActionListener(e -> dumpAllData());

        cacheOperationsButtonPanel.add(addFilesButton);
        cacheOperationsButtonPanel.add(replaceFilesButton);
        cacheOperationsButtonPanel.add(removeFileButton);
        cacheOperationsButtonPanel.add(setArchiveNameButton);
        cacheOperationsButtonPanel.add(exportAllDataButton);

        JTable infoTable = new JTable();

        cacheTree.addTreeSelectionListener(e -> {

            if (cacheTree.getLastSelectedPathComponent().toString().contains("Index")) {

                selectedIndex = Integer.parseInt(cacheTree.getLastSelectedPathComponent().toString().replace("Index ", "").trim());

                Object[][] indexFields = new Object[][] {

                    new Object[] {
                        "Cache Index ID", selectedIndex
                    },

                    new Object[] {
                        "Amount of Archives", cacheLibrary.getIndex(selectedIndex).getArchives().length
                    },

                    new Object[] {
                        "Index CRC Value", cacheLibrary.getIndex(selectedIndex).getCRC()
                    },

                    new Object[] {
                        "Index Version", cacheLibrary.getIndex(selectedIndex).getVersion()
                    }
                };

                String[] indexFieldValues = new String[] {
                        "", ""
                };

                infoTable.setModel(new DefaultTableModel(indexFields, indexFieldValues));
                infoTable.setRowHeight(20);
                infoTable.revalidate();
            }

            if (cacheTree.getLastSelectedPathComponent().toString().contains("Archive")) {

                selectedArchive = Integer.parseInt(cacheTree.getLastSelectedPathComponent().toString().replace("Archive ", "").trim());

                Object[][] archiveFields = new Object[][] {

                    new Object[] {
                        "Cache Index ID", selectedIndex
                    },

                    new Object[] {
                        "Archive ID", selectedArchive
                    },

                    new Object[] {
                        "Archive Name Hash", cacheLibrary.getIndex(selectedIndex).getArchive(selectedArchive).getName()
                    },

                    new Object[] {
                        "Amount of Files", cacheLibrary.getIndex(selectedIndex).getArchive(selectedArchive).getFiles().length
                    },

                    new Object[] {
                        "Archive CRC Value", cacheLibrary.getIndex(selectedIndex).getArchive(selectedArchive).getCRC()
                    },

                    new Object[] {
                        "Archive Revision", cacheLibrary.getIndex(selectedIndex).getArchive(selectedArchive).getRevision()
                    }
                };

                String[] archiveFieldValues = new String[] {
                    "", ""
                };

                infoTable.setModel(new DefaultTableModel(archiveFields, archiveFieldValues));
                infoTable.setRowHeight(20);
                infoTable.revalidate();

            }

            if (cacheTree.getLastSelectedPathComponent().toString().contains("File")) {

                selectedFile = Integer.parseInt(cacheTree.getLastSelectedPathComponent().toString().replace("File ", "").trim());

                Object[][] fileFields = new Object[][] {

                    new Object[] {
                            "Cache Index ID", selectedIndex
                    },

                    new Object[] {
                            "Archive ID", selectedArchive
                    },

                    new Object[] {
                            "File ID", selectedFile
                    },

                    new Object[] {
                            "File Name Hash", cacheLibrary.getIndex(selectedIndex).getArchive(selectedArchive).getFile(selectedFile).getName()
                    }
                };

                String[] fileFieldValues = new String[] {
                        "", ""
                };

                infoTable.setModel(new DefaultTableModel(fileFields, fileFieldValues));
                infoTable.setRowHeight(20);
                infoTable.revalidate();
            }
        });

        cacheInfoPanel.add(infoTable);

        cacheInfoSplitPanel.setRightComponent(cacheOperationsButtonPanel);
        cacheInfoSplitPanel.setLeftComponent(cacheInfoPanel);
        cacheInfoSplitPanel.setDividerLocation(240);
        cacheInfoSplitPanel.setEnabled(false);

        splitCacheViewPane.setLeftComponent(cacheScrollPane);
        splitCacheViewPane.setRightComponent(cacheInfoSplitPanel);
        splitCacheViewPane.setDividerLocation(160);
        splitCacheViewPane.setEnabled(false);

        contentPanel.add(splitCacheViewPane, BorderLayout.CENTER);
        contentPanel.revalidate();
    }

    private void addCacheFiles() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File[] files = fileChooser.getSelectedFiles();
            for (File file : files) {
                if (cacheLibrary.getIndex(selectedIndex) != null) {
                    try {
                        String trimmedName = file.getName().substring(0, file.getName().indexOf(".")).trim();
                        if (trimmedName.contains("-")) {
                            selectedFile = Integer.parseInt(trimmedName.substring(trimmedName.indexOf("-")).trim().replace("-", "").trim());
                            trimmedName = trimmedName.substring(0, trimmedName.indexOf("-")).trim();
                        }
                        cacheLibrary.getIndex(selectedIndex).getArchive(Integer.parseInt(trimmedName)).addFile(selectedFile, Files.readAllBytes(file.toPath()));
                        if (cacheLibrary.getIndex(selectedIndex).update()) {
                            loadCache(new File(cacheLibrary.getPath()));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void replaceCacheFiles() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File[] files = fileChooser.getSelectedFiles();
            for (File file : files) {
                if (cacheLibrary.getIndex(selectedIndex) != null) {
                    try {
                        String trimmedName = file.getName().substring(0, file.getName().indexOf(".")).trim();
                        if (trimmedName.contains("-")) {
                            selectedFile = Integer.parseInt(trimmedName.substring(trimmedName.indexOf("-")).trim().replace("-", "").trim());
                            trimmedName = trimmedName.substring(0, trimmedName.indexOf("-")).trim();
                        }
                        cacheLibrary.getIndex(selectedIndex).getArchive(Integer.parseInt(trimmedName)).addFile(selectedFile, Files.readAllBytes(file.toPath()));
                        if (cacheLibrary.getIndex(selectedIndex).update()) {
                            loadCache(new File(cacheLibrary.getPath()));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    private void setCacheArchiveName() {
        String fileNameToSet = JOptionPane.showInputDialog("Set Archive Name", "");
        if (fileNameToSet != null) {
            if (cacheLibrary.getIndex(selectedIndex).getArchive(selectedArchive) != null) {
                Archive renamedArchive = cacheLibrary.getIndex(selectedIndex).getArchive(selectedArchive);
                renamedArchive.setName(fileNameToSet.toLowerCase().hashCode());
                cacheLibrary.getIndex(selectedIndex).addArchive(renamedArchive, true);
                if (cacheLibrary.getIndex(selectedIndex).update()) {
                    loadCache(new File(cacheLibrary.getPath()));
                }
            }
        }
    }

    private void removeCacheFile() {
        cacheLibrary.getIndex(selectedIndex).getArchive(selectedArchive).removeFile(selectedFile);
        if (cacheLibrary.getIndex(selectedIndex).update()) {
            loadCache(new File(cacheLibrary.getPath()));
            JOptionPane.showMessageDialog(this, "Cache File " + selectedFile + " has been removed.");
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
                    File archiveData = new File(indexDirectory + File.separator + archive.getId() + ".dat");
                    FileOutputStream archiveOutputStream = new FileOutputStream(archiveData);
                    for (org.displee.cache.index.archive.file.File archiveFileData : cacheLibrary.getIndex(selectedIndex).getArchive(archive.getId()).getFiles()) {
                        if (archiveFileData.getId() != 0) {
                            File fileData = new File(indexDirectory + File.separator + archive.getId() + "-" + archiveFileData.getId() + ".dat");
                            FileOutputStream fileOutputStream = new FileOutputStream(fileData);
                            fileOutputStream.write(archiveFileData.getData());
                            fileOutputStream.flush();
                            fileOutputStream.close();
                        }
                        else {
                            archiveOutputStream.write(archiveFileData.getData());
                            archiveOutputStream.flush();
                            archiveOutputStream.close();
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
    }

    private void printEnums() {
        EnumComposition enumComposition = new EnumComposition();
        enumComposition.decode(new Buffer(cacheLibrary.getIndex(selectedIndex).getArchive(selectedArchive).getFile(selectedFile).getData()));
        System.out.println(Arrays.toString(enumComposition.strVals));
    }
}
