package decoders;

import com.application.GUI;
import net.runelite.cache.definitions.SpriteDefinition;
import net.runelite.cache.definitions.loaders.SpriteLoader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;

public class SpritesBatchDecoder {

    public SpritesBatchDecoder() {
        for (int archive = 0; archive < 1000000; archive++) {
            SpriteLoader spriteLoader = new SpriteLoader();
            byte[] spriteData = Objects.requireNonNull(Objects.requireNonNull(GUI.cacheLibrary.index(8).archive(archive)).file(0)).getData();
            if (spriteData != null) {
                SpriteDefinition spriteDefinition = spriteLoader.load(archive, spriteData)[0];
                BufferedImage image = export(spriteDefinition);
                File outputFilePath = new File(GUI.cacheLibrary.getPath() + File.separator + "Decoded Data" + File.separator + "Sprites");
                boolean madeDirectory = outputFilePath.mkdirs();
                if (madeDirectory) {
                    GUI.cacheOperationInfo.setText("Archive " + archive + " was decoded successfully. New folder created in cache directory.");
                } else {
                    GUI.cacheOperationInfo.setText("Archive " + archive + " was decoded successfully. It is in the cache directory.");
                }
                try {
                    FileOutputStream fileOutputStream = new FileOutputStream(outputFilePath + File.separator + archive + ".png");
                    if (image != null) {
                        ImageIO.write(image, "png", fileOutputStream);
                    }
                    fileOutputStream.flush();
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private BufferedImage export(SpriteDefinition spriteDefinition) {
        if (spriteDefinition.width <= 0 && spriteDefinition.height <= 0) {
            return null;
        }
        BufferedImage bi = new BufferedImage(spriteDefinition.width, spriteDefinition.height, BufferedImage.TYPE_INT_ARGB);
        bi.setRGB(0, 0, spriteDefinition.width, spriteDefinition.height, spriteDefinition.pixels, 0, spriteDefinition.width);
        return bi;
    }
}
