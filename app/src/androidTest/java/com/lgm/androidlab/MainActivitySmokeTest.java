package com.lgm.androidlab;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.lgm.androidlab.catalog.LabCatalog;
import com.lgm.androidlab.catalog.LabItem;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Abre a tela inicial e cada laboratório; garante que nada quebra ao inflar. */
@RunWith(AndroidJUnit4.class)
public class MainActivitySmokeTest {

    @Test
    public void catalogIsVisible() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.labList)).check(matches(isDisplayed()));
            onView(withText(R.string.lab_particles_title)).check(matches(isDisplayed()));
            scenario.onActivity(activity -> {
                if (activity.findViewById(R.id.toolbar) == null) {
                    throw new AssertionError("toolbar ausente na tela inicial");
                }
            });
        }
    }

    @Test
    public void everyLabLaunches() {
        for (LabItem item : LabCatalog.all()) {
            try (ActivityScenario<?> scenario = ActivityScenario.launch(item.activity())) {
                scenario.onActivity(activity -> {
                    if (activity.findViewById(R.id.toolbar) == null) {
                        throw new AssertionError("toolbar ausente em " + item.id());
                    }
                });
            }
        }
    }
}
