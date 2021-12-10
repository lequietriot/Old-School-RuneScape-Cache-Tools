package runescape;

public enum CacheConstantsOSRS {

    ANIMATIONS(0, "Animations"),
    SKELETONS(1, "Skeletons"),
    CONFIGURATION(2, "Configuration"),
    INTERFACES(3, "Interfaces"),
    SOUND_FX(4, "Sound FX"),
    MAPS(5, "Maps"),
    MUSIC_TRACKS(6, "Music Tracks"),
    MODELS(7, "Models"),
    SPRITES(8, "Sprites"),
    TEXTURES(9, "Textures"),
    BINARIES(10, "Binaries"),
    MUSIC_JINGLES(11, "Music Jingles"),
    CLIENT_SCRIPTS(12, "Client Scripts"),
    FONTS(13, "Fonts"),
    MUSIC_SAMPLES(14, "Music Samples"),
    MUSIC_PATCHES(15, "Music Patches"),
    UNUSED_WORLD_MAP(16, "World Map"),
    ICONS(17, "Icons"),
    WORLD_MAP_GEOGRAPHY(18, "World Map Geography"),
    WORLD_MAP(19, "World Map"),
    WORLD_MAP_GROUND(20, "World Map Ground");

    private final String indexDescription;
    private final int indexID;

    CacheConstantsOSRS(int id, String description) {
        indexID = id;
        indexDescription = description;
    }

    public int getIndexID() {
        return indexID;
    }

    public String getIndexDescription() {
        return indexDescription;
    }

}
