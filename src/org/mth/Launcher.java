package org.mth;

import com.formdev.flatlaf.FlatDarculaLaf;
import org.mth.gui.UIFix;
import org.mth.sqlite.MyBatisHelperKt;

import javax.swing.*;

public class Launcher {
    public static void main(String[] args) throws UnsupportedLookAndFeelException {
        MyBatisHelperKt.connectToPreferencesDatabase();

        ApplicationKt.randomFiles(10);

        UIManager.setLookAndFeel(new FlatDarculaLaf());
        new UIFix().fix();

        Application.INSTANCE.setVisible(true);
    }
}
