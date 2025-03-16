/*
 * Copyright (c) 2025 Mattia Marelli
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package org.mth.cleaner;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.mth.cleaner.ui.FlatLightUI;
import org.mth.cleaner.ui.PathChronologyItem;
import org.mth.sqlite.MyBatisHelperKt;
import org.tinylog.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.mth.cleaner.ui.FlatLightUI.installFont;

public class ApplicationContext {

    public static final boolean DEBUG = false;
    public static final Path APP_FOLDER;

    static {
        if (DEBUG)
            APP_FOLDER = Path.of("C:\\Users\\matti\\OneDrive\\Documenti\\Java\\JFileCleaner\\src\\main\\resources\\prefs");
        else {
            APP_FOLDER = Path.of(System.getProperty("user.home"), ".mthApp", "JFC");
            //noinspection ResultOfMethodCallIgnored
            APP_FOLDER.toFile().mkdirs();
        }
    }

    public static final Path PREFERENCES_FILE = APP_FOLDER.resolve("preferences.properties");
    public static JFrame FRAME;
    public static final ExecutorService jobExecutor = Executors.newSingleThreadExecutor();
    private static final List<PathChronologyItem> pathChronology = new ArrayList<>();

    public static void addToChronology(Path path) {
        Optional<PathChronologyItem> item = pathChronology.stream()
                .filter(pathChronologyItem -> pathChronologyItem.getPath().equals(path))
                .findAny();

        if (item.isEmpty()) {
            pathChronology.add(new PathChronologyItem(path, System.currentTimeMillis()));
        } else {
            item.get().setTimestamp(System.currentTimeMillis());
        }

        pathChronology.sort(Comparator.comparingLong(PathChronologyItem::getTimestamp).reversed());
    }

    public static Collection<PathChronologyItem> getChronology(int maxElementCount) {
        return pathChronology.stream().limit(maxElementCount).toList();
    }

    public static ResourceBundle getBundle() {
        return ResourceBundle.getBundle("org.mth.cleaner.application");
    }

    @NotNull
    public static String i18nString(String key) {
        return getBundle().getString(key);
    }

    public static ImageIcon getSvgIcon(String icon, int w, int h) {
        try {
            return new FlatSVGIcon(ApplicationContext.getResourceAsStream(icon)).derive(w, h);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    public static ImageIcon getSvgIcon(String icon, int w, int h, Color from, Color to) {
        try {
            FlatSVGIcon svgIcon = new FlatSVGIcon(ApplicationContext.getResourceAsStream(icon)).derive(w, h);
            FlatSVGIcon.ColorFilter colorFilter = new FlatSVGIcon.ColorFilter();
            colorFilter.add(from, to);
            svgIcon.setColorFilter(colorFilter);
            return svgIcon;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private static Properties loadPreferences() {
        var properties = new Properties();

        try {
            properties.load(new FileInputStream(PREFERENCES_FILE.toFile()));
        } catch (IOException e) {
            Logger.error("Cannot load application preferences");
        }
        return properties;
    }

    private static void onWindowOpening(@NotNull FlatLightUI frame, Properties properties) {
        frame.injectPreferences(properties);
    }

    private static void onWindowClosing(@NotNull FlatLightUI frame) {
        jobExecutor.shutdown();

        var props = new Properties();
        props.putAll(frame.getPreferences());
        props.setProperty("chronology.count", Integer.toString(pathChronology.size()));

        for (int i = 0; i < pathChronology.size(); i++) {
            PathChronologyItem item = pathChronology.get(i);
            props.setProperty("chronology.%d.path".formatted(i), item.getPath().normalize().toString()); //NON-NLS
            props.setProperty("chronology.%d.timestamp".formatted(i), Long.toString(item.getTimestamp())); //NON-NLS
        }

        try {
            props.store(new FileOutputStream(PREFERENCES_FILE.toFile()), null);
        } catch (IOException ex) {
            Logger.error(ex);
        }
    }

    private static void restoreChronology(@NotNull Properties properties) {
        pathChronology.clear();

        var c = Integer.parseInt(properties.getProperty("chronology.count", "0"));

        for (int i = 0; i < c; i++) {
            var item = new PathChronologyItem(
                    Path.of(properties.getProperty("chronology.%d.path".formatted(i))),
                    Long.parseLong(properties.getProperty("chronology.%d.timestamp".formatted(i)))
            );
            pathChronology.add(item);
        }

        // sort the list, recent folders first
        pathChronology.sort(Comparator.comparingLong(PathChronologyItem::getTimestamp).reversed());
    }

    public static InputStream getResourceAsStream(String resource) {
        return ApplicationContext.class.getResourceAsStream(resource);
    }

    public static void fire(@NonNls String event, char newValue, char oldValue) {
        FRAME.firePropertyChange(event, oldValue, newValue);
    }

    public static void main(String[] args) {
        //        UtilitiesKt.createRandomFiles(5, Path.of("C:\\Users\\matti\\OneDrive\\Documenti\\Java\\JFileCleaner\\file_test"), 1);

        MyBatisHelperKt.connectToHistoryDatabase();

        setuLaf();

        var preferences = loadPreferences();

        var width = Integer.parseInt(preferences.getProperty("frame.width", "720"));
        var height = Integer.parseInt(preferences.getProperty("frame.height", "500"));

        var frame = new FlatLightUI();
        frame.setPreferredSize(new Dimension(width, height));
        frame.pack();
        frame.setVisible(true);
        frame.setLocationRelativeTo(null);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                onWindowOpening(frame, preferences);
                restoreChronology(preferences);
            }

            @Override
            public void windowClosing(WindowEvent e) {
                onWindowClosing(frame);
            }
        });

        FRAME = frame;
    }

    public static void setuLaf() {
        Logger.debug("Installing fonts...");
        installFont();

        Logger.debug("Read custom LAF properties...");
        FlatLaf.registerCustomDefaultsSource("org.mth.cleaner");
        FlatLightLaf.setup();
    }
}
