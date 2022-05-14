package runelite.definitions;

public class ParticleEmitterConfig {

    public int anInt834;
    public ParticleEmitterConfig aClass87_835;
    public int anInt840;
    public int anInt841;
    public int anInt843;
    public int anInt844;
    public int anInt845;
    public int anInt846;
    public int anInt847;
    public int anInt848;
    public int faceX;
    public int faceY;
    public int faceZ;
    public byte priority;
    int type;

    public ParticleEmitterConfig(int i_1, int i_2, int i_3, int i_4, byte b_5) {
        type = i_1;
        faceX = i_2;
        faceY = i_3;
        faceZ = i_4;
        priority = b_5;
    }

    public ParticleEmitterConfig method1488(int i_1, int i_2, int i_3) {
        return new ParticleEmitterConfig(type, i_1, i_2, i_3, priority);
    }
}
