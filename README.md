# Old School RuneScape Cache Tools (v0.5-beta)
### The ultimate suite of RuneScape Cache modification tools!
Made with IntelliJ IDEA using Java 1.8 SDK with various libraries including JavaFX.
## This suite features:
### A simple 3D Model Viewer, built into the main window!
### Cache Functions
* Select Cache Data - A quick way to select a specific archive file in the cache.
* Search Cache (Work in progress)
  * Allows you to search for the location of a file based on any given name. Currently, whatever is searched for prints the results to the system console.
* Cache explorer with functions for manipulating raw data
  * Add Files
  * Export Files
  * Remove File
  * Remove Archive
  * Set Archive name hash (Numerical)
  * Set Archive name (String)
  * Export All Index Data
* Helpful information within the Cache explorer
  * Cache Type (RuneScape 2, Old School RuneScape, RuneScape High Definition, or RuneScape 3)
  * Cache Index Name (based on type)
  * Amount of Archives in Index
    * Cache index ID
    * Archive ID
    * Archive Name Hash
    * Amount of Files
      * File ID
      * File Name Hash
    * Archive CRC Value
    * Archive Revision
  * Index CRC Value
  * Index Version
#### Cache Data Encoding and Decoding
* Individual Data Encoding tools (To RuneScape Format)
  * MIDI Music and MIDI Jingles (For best results encode with Type 0 MIDI files)
  * OBJ Model and MTL File (Textures not supported yet)
  * OGG Vorbis Instrument Samples
####
* Individual Data Decoding tools (To General Format)
  * Configuration
    * Enums
  * Sound Effects
  * MIDI Music and MIDI Jingles
  * OBJ Model and MTL File
    * Automatically adjusted decoder based on Cache Type (Currently supports: RuneScape 2, Old School RuneScape & RuneScape High Definition)
  * OGG Vorbis Instrument Samples
  * SoundFont 2 Instrument Patches
####
  * Batch Data Decoding Tools (To General Format)
    * Configuration
      * Enums
### Tools
* Music player tool
  * Load Custom MIDI Files
  * Ability to Play or Stop the loaded song
  * Ability to render songs to lossless and uncompressed wav file
  * Choose from either music or jingles in the cache index
  * Listen/Render songs in mono or stereo
  * Batch render all music and jingles to the cache output folder
  * Shuffle songs from folder or cache where possible
  * Ability to override the instruments with a SoundFont 2 file of choice, using the original instrument configurations.
* Music port tool for utilizing the RuneScape sounds in music producing applications such as Cubase, etc. (MIDI Port must contain "port" in its name)
* Xtea Keys Tool, to decode encrypted map files - The relevant archive/file must be selected in the explorer while using the tool.
* Model Converter - New to Old, converts the RuneScape High Definition era models to older RuneScape 2 models, preserving the RuneScape format.

### Credits
* Displee (https://github.com/Displee): for the cache library.
* Vincent (Rune-Server): for figuring out the MIDI Encoder.
* Gagravarr (https://github.com/Gagravarr): vorbis-java libraries.
* FormDev Software: for the FlatLaf interface look and feel.
* RuneLite Contributors (https://github.com/runelite/runelite): for the open source Old School RuneScape Client.
* Suic (https://github.com/Suicolen): For the simple JFX model viewer/editor tool.
* Jagex: for being an inspiration in making this happen.

### Known Bugs/Issues
* Cache Explorer: Not able to dump/export all files of single archive - only the first file is exported?
* Cache Explorer: Some caches may not load correctly
* Data Decoders: SoundFont conversion - may not be perfect
* Model Viewer: Does not work on RS2 yet
* Model Viewer: May not work on Mac computers, a workaround would be running the project directly through IntelliJ IDEA.

## That's all for now. Enjoy!