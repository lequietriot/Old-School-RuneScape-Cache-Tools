package com.application;

import com.formdev.flatlaf.FlatDarculaLaf;

public class Main {

    public static void main(String[] args) {
        FlatDarculaLaf.setup();
        GUI gui = new GUI();
        gui.setVisible(true);
    }
}
