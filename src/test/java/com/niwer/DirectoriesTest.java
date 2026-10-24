package com.niwer;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import niwer.photon.Directories;

public class DirectoriesTest {

    @Test
    public void testLoadDirectories() {
        Directories.load();
        assertTrue(Directories.BASE_DIR.exists());
        assertTrue(Directories.LOGS_DIR.exists());
        assertTrue(Directories.BACKUPS_DIR.exists());
        assertTrue(Directories.configFile.exists());
    }

    @Test
    public void testGetConfig() {
        Directories.load();
        assertTrue(Directories.config() != null);
    }

    @Test
    public void testSaveConfig() {
        Directories.load();
        Directories.save();
        assertTrue(Directories.configFile.exists());
    }
}