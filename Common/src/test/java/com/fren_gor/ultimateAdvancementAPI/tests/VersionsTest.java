package com.fren_gor.ultimateAdvancementAPI.tests;

import com.fren_gor.ultimateAdvancementAPI.util.Versions;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static com.fren_gor.ultimateAdvancementAPI.util.Versions.getApiVersion;
import static com.fren_gor.ultimateAdvancementAPI.util.Versions.getNMSVersionsList;
import static com.fren_gor.ultimateAdvancementAPI.util.Versions.getNMSVersionsRange;
import static com.fren_gor.ultimateAdvancementAPI.util.Versions.getSupportedNMSVersions;
import static com.fren_gor.ultimateAdvancementAPI.util.Versions.removeInitialV;
import static org.junit.Assert.*;

public class VersionsTest {

    @Test
    public void apiVersionTest() throws Exception {
        // Class is generated by templating-maven-plugin
        Class<?> mavenProperties = Class.forName("com.fren_gor.ultimateAdvancementAPI.tests.MavenProperties");
        String mavenVersion = (String) mavenProperties.getDeclaredField("API_VERSION").get(null);
        String failMessage = "API_VERSION in Versions class is not the same as the pom.xml version. Please update the Versions.API_VERSION constant.";
        assertEquals(failMessage, mavenVersion, Versions.getApiVersion());
    }

    @Test
    public void removeInitialVTest() {
        assertEquals("1_15_R1", removeInitialV("v1_15_R1"));
        assertEquals("1_15_R1", removeInitialV("1_15_R1"));
        assertEquals("", removeInitialV(""));
        assertEquals("v1_15_R1", removeInitialV("vv1_15_R1"));
        assertNull(removeInitialV(null));
    }

    @Test
    public void getNMSVersionTest() {
        try (MockedStatic<Bukkit> bukkitMock = Mockito.mockStatic(Bukkit.class)) {
            Server server = InterfaceImplementer.newFakeServer();
            bukkitMock.when(Bukkit::getServer).thenReturn(server);
            assertSame("Mock failed", Bukkit.getServer(), server);
            assertEquals("serverVersion1_17_R1", Versions.getNMSVersion());
        }
    }

    @Test
    public void notNullTest() {
        assertNotNull(getApiVersion());
        assertNotNull(getSupportedNMSVersions());
        for (String nms : getSupportedNMSVersions()) {
            assertNotNull(nms);
            assertNotNull(getNMSVersionsList(nms));
            assertNotNull(getNMSVersionsRange(nms));
        }
    }
}
