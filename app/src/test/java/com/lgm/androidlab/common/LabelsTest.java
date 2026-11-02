package com.lgm.androidlab.common;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Locale;

public class LabelsTest {

    private Locale previous;

    @Before
    public void fixLocale() {
        previous = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }

    @After
    public void restoreLocale() {
        Locale.setDefault(previous);
    }

    @Test
    public void prettifyTurnsEnumNamesIntoTitles() {
        assertEquals("Gosper Gun", Labels.prettify("GOSPER_GUN"));
        assertEquals("Glider", Labels.prettify("GLIDER"));
        assertEquals("", Labels.prettify(""));
    }

    @Test
    public void groupDigitsUsesLocaleGrouping() {
        assertEquals("1,234,567", Labels.groupDigits(1234567));
        assertEquals("0", Labels.groupDigits(0));
        assertEquals("-42", Labels.groupDigits(-42));
    }

    @Test
    public void durationPicksSensibleUnits() {
        assertEquals("0 ms", Labels.duration(0));
        assertEquals("12 ms", Labels.duration(12.4));
        assertEquals("1.50 s", Labels.duration(1500));
    }
}
