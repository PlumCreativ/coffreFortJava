package com.coffrefort.client;

import com.coffrefort.client.util.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("FormatUtil Tests")
public class FormatUtilTest {

    @Test
    @DisplayName("Devrait formater la taille des fichiers correctement")
    public void testFormatFileSize() {
        assertEquals("1,5 KB", FileUtils.formatSize(1536));
        assertEquals("2,3 MB", FileUtils.formatSize(2411724));
        assertEquals("1,00 GB", FileUtils.formatSize(1073741824));
    }
}