package com.lgm.androidlab.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LabCatalogTest {

    @Test
    public void catalogHasEightLabs() {
        assertEquals(8, LabCatalog.all().size());
    }

    @Test
    public void idsAreUniqueAndSlugLike() {
        Set<String> seen = new HashSet<>();
        for (LabItem item : LabCatalog.all()) {
            assertTrue("id inválido: " + item.id(), item.id().matches("[a-z][a-z0-9_]*"));
            assertTrue("id duplicado: " + item.id(), seen.add(item.id()));
        }
    }

    @Test
    public void everyLabPointsToAnActivity() {
        for (LabItem item : LabCatalog.all()) {
            assertTrue(item.id() + " não é uma Activity",
                    Activity.class.isAssignableFrom(item.activity()));
        }
    }

    @Test
    public void resourceIdsAreNonZero() {
        for (LabItem item : LabCatalog.all()) {
            assertTrue(item.titleRes() != 0);
            assertTrue(item.descriptionRes() != 0);
            assertTrue(item.iconRes() != 0);
            assertTrue(item.accentRes() != 0);
        }
    }

    @Test
    public void byIdFindsKnownAndRejectsUnknown() {
        assertNotNull(LabCatalog.byId("particles"));
        assertEquals("particles", LabCatalog.byId("particles").id());
        assertNull(LabCatalog.byId("nope"));
    }

    @Test
    public void listIsImmutable() {
        List<LabItem> all = LabCatalog.all();
        try {
            all.clear();
            throw new AssertionError("lista deveria ser imutável");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
    }
}
