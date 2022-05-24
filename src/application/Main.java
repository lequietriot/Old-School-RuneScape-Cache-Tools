package application;

import com.formdev.flatlaf.FlatIntelliJLaf;

public class Main {

    public static void main(String[] args) {
        FlatIntelliJLaf.setup();
        GUI gui = new GUI();
        gui.setVisible(true);
    }
}
