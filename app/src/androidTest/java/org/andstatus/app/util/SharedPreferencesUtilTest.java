/*
 * Copyright (c) 2016 yvolk (Yuri Volkov), http://yurivolkov.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.andstatus.app.util;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.test.InstrumentationTestCase;

import org.andstatus.app.context.MyContextHolder;
import org.andstatus.app.context.TestSuite;

import java.io.File;

/**
 * @author yvolk@yurivolkov.com
 */
public class SharedPreferencesUtilTest extends InstrumentationTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        TestSuite.initialize(this);
    }

    public void testPrefsDirectory() {
        File prefsDir = SharedPreferencesUtil.prefsDirectory(MyContextHolder.get().context());
        assertTrue("Prefs dir: " + prefsDir.getAbsolutePath(), prefsDir.exists());
        File defaultSharedPreferencesFile = SharedPreferencesUtil.defaultSharedPreferencesPath(MyContextHolder.get().context());
        assertTrue("defaultSharedPreferencesFile: " + defaultSharedPreferencesFile.getAbsolutePath(), defaultSharedPreferencesFile.exists());
    }

    public void testResetHasSetDefaultValues() {
        assertEquals(TriState.TRUE, SharedPreferencesUtil.areDefaultPreferenceValuesSet());
        SharedPreferencesUtil.resetHasSetDefaultValues();
        assertEquals(TriState.FALSE, SharedPreferencesUtil.areDefaultPreferenceValuesSet());

        SharedPreferences sp = SharedPreferencesUtil.getSharedPreferences(PreferenceManager.KEY_HAS_SET_DEFAULT_VALUES);
        sp.edit().putBoolean(PreferenceManager.KEY_HAS_SET_DEFAULT_VALUES, true).commit();
        assertEquals(TriState.TRUE, SharedPreferencesUtil.areDefaultPreferenceValuesSet());
    }

}
