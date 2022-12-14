/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.alpha.settings;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceFragment;

import com.android.internal.logging.nano.MetricsProto;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;

import com.android.settingslib.search.SearchIndexable;

@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class AlphaSettings extends DashboardFragment {
    private static final String TAG = "AlphaSettings";

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.ALPHA;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.alpha_settings;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setAlphaDashboardStyle();
    }

    public void onResume() {
        super.onResume();
        //setAlphaDashboardStyle();
    }

    private void setAlphaDashboardStyle() {
        int mDashBoardStyle = geSettingstDashboardStyle();
        final PreferenceScreen mScreen = getPreferenceScreen();
        final int mCount = mScreen.getPreferenceCount();
        for (int i = 0; i < mCount; i++) {
            final Preference mPreference = mScreen.getPreference(i);

            String mKey = mPreference.getKey();

            if (mKey == null) continue;

            if (mKey.equals("alphabet_header")) {
                mPreference.setLayoutResource(R.layout.settings_alphabet_header);
                continue;
            }

            if (mDashBoardStyle > 0) { // 0 = stock aosp style
                if (mDashBoardStyle == 1 && mKey.equals("statusbar_settings_category")) {
                    mPreference.setLayoutResource(R.layout.dot_dashboard_preference_full_accent_icon);
                } else if (mDashBoardStyle == 2 && mKey.equals("statusbar_settings_category")) {
                    mPreference.setLayoutResource(R.layout.dot_dashboard_preference_full_accent_2_icon);
                } else {
                    mPreference.setLayoutResource(R.layout.dot_dashboard_preference_full_icon);
                }
            }
        }
    }

    private int geSettingstDashboardStyle() {
        return Settings.System.getIntForUser(getContext().getContentResolver(),
                Settings.System.SETTINGS_DASHBOARD_STYLE, 2, UserHandle.USER_CURRENT);
    }

    @Override
    public int getHelpResource() {
        return R.string.help_uri_display;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.alpha_settings);
}
