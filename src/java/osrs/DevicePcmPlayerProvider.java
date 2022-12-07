package osrs;

public class DevicePcmPlayerProvider implements PcmPlayerProvider {

    public DevicePcmPlayerProvider() {

    }

    public PcmPlayer player() {
        return new DevicePcmPlayer();
    }
}
