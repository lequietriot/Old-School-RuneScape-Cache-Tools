# Old School RuneScape Cache Tools
### The ultimate suite of older RuneScape Cache editing tools!
## This suite features:
### Cache Functions
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
* Data Encoding tools (To RuneScape Format)
  * MIDI Music and MIDI Jingles
  * OGG Vorbis Instrument Samples
####
* Data Decoding tools (To General Format)
  * MIDI Music and MIDI Jingles
  * OBJ Model and MTL Textures
    * Automatically adjusted decoder based on Cache Type (Currently supports: Old School RuneScape & RuneScape High Definition)
  * OGG Vorbis Instrument Samples
  * SoundFont 2 Instrument Patches (Work in Progress)
### Tools
* Music player tool
  * Load Custom MIDI Files
  * Ability to Play or Stop the loaded song
  * Ability to render songs to lossless and uncompressed wav file
  * Choose from either music or jingles in the cache index
  * Listen/Render songs in mono or stereo
  * Shuffle songs from folder or cache where possible
  * Ability to override the instruments with a SoundFont 2 file of choice, using the original instrument configurations.
* Music port tool for utilizing the RuneScape sounds in music producing applications such as Cubase, etc. (MIDI Port must contain "port" in its name)
* Xtea Keys Tool, to decode encrypted map files - The relevant archive/file must be selected in the explorer while using the tool.

### Credits
* Displee (https://github.com/Displee): for the cache library.
* Vincent (Rune-Server): for figuring out the MIDI Encoder.
* Gagravarr (https://github.com/Gagravarr): vorbis-java libraries.
* FormDev Software: for the FlatLaf interface look and feel.
* RuneLite Contributors (https://github.com/runelite/runelite): for the open source Old School RuneScape Client.
* Jagex: for being an inspiration in making this happen.

### Known Bugs/Issues
* Cache Explorer: Not able to dump/export all files of single archive - only the first file is exported?
* Cache Explorer: Some caches may not load correctly

## That's all for now. Enjoy!