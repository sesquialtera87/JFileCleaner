package org.mth;

import com.formdev.flatlaf.FlatDarculaLaf;
import org.mth.gui.UIFix;
import org.mth.sqlite.MyBatisHelperKt;

import javax.swing.*;

public class Launcher {
    public static void main(String[] args) {
        MyBatisHelperKt.connectToPreferencesDatabase();

        ApplicationKt.randomFiles(10);

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new FlatDarculaLaf());
                new UIFix().fix();
            } catch (UnsupportedLookAndFeelException e) {
                e.printStackTrace();
            }

            Application.INSTANCE.setVisible(true);
        });
    }
}
