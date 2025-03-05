package com.application;

import com.displee.cache.CacheLibrary;
import osrs.MusicPatch;
import osrs.SoundCache;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;

public class PatchTest {

    public static void main(String[] args) throws IOException {
        CacheLibrary cacheLibrary = new CacheLibrary(System.getProperty("user.home") + File.separator + "Documents" + File.separator + "772 Cache Music Data", false, null);
        for (int patchID = 0; patchID < 10000; patchID++) {
            if (cacheLibrary.index(15).archive(patchID) != null) {
                File fileData = new File(cacheLibrary.getPath() + File.separator + "idx15" + File.separator + patchID + ".dat");
                FileOutputStream fileOutputStream = new FileOutputStream(fileData);
                fileOutputStream.write(Objects.requireNonNull(cacheLibrary.data(15, patchID, 0)));
                fileOutputStream.flush();
                fileOutputStream.close();
                MusicPatch musicPatch = new MusicPatch(cacheLibrary.data(15, patchID, 0));
                musicPatch.method4945(new SoundCache(cacheLibrary.index(4), cacheLibrary.index(14)), null, null);
            }
        }
    }

}
