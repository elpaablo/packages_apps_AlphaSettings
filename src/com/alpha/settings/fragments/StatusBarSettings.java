/*
 * Copyright (C) 2014-2015 The CyanogenMod Project
 *               2017-2019 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alpha.settings.fragments;

import static com.android.internal.util.custom.clock.ClockConstants.CLOCK_POSITION_RIGHT;
import static com.android.internal.util.custom.clock.ClockConstants.CLOCK_POSITION_CENTER;
import static com.android.internal.util.custom.clock.ClockConstants.CLOCK_POSITION_LEFT;
import static com.android.internal.util.custom.clock.ClockConstants.CLOCK_POSITION_HIDE;
import static com.android.internal.util.custom.clock.ClockConstants.CLOCK_POSITION_DEFAULT;

import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.text.TextUtils;
import android.util.ArraySet;
import android.view.View;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.ListPreference;
import androidx.preference.SwitchPreference;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.util.custom.cutout.CutoutUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.custom.preference.SystemSettingListPreference;
import com.android.settings.custom.utils.TelephonyUtils;
import com.android.settings.search.BaseSearchIndexProvider;

import com.android.settingslib.search.SearchIndexable;

import java.util.Set;


@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class StatusBarSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String CATEGORY_BATTERY = "status_bar_battery_key";
    private static final String CATEGORY_CLOCK = "status_bar_clock_key";

    private static final String ICON_BLACKLIST = "icon_blacklist";

    private static final String STATUS_BAR_CLOCK = "status_bar_clock";
    private static final String STATUS_BAR_BATTERY_STYLE = "status_bar_battery_style";
    private static final String STATUS_BAR_SHOW_BATTERY_PERCENT = "status_bar_show_battery_percent";


    private static final int STATUS_BAR_BATTERY_STYLE_TEXT = 4;
    private static final int STATUS_BAR_BATTERY_STYLE_HIDDEN = 5;

    private static final String KEY_SHOW_FOURG = "show_fourg_icon";
    private static final String KEY_USE_OLD_MOBILETYPE = "use_old_mobiletype";

    private SystemSettingListPreference mStatusBarClock;
    private SystemSettingListPreference mStatusBarBattery;
    private SystemSettingListPreference mStatusBarBatteryShowPercent;
    private SwitchPreference mShowFourg;
    private SwitchPreference mOldMobileType;

    private PreferenceCategory mStatusBarBatteryCategory;
    private PreferenceCategory mStatusBarClockCategory;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.sb_settings);

        final PreferenceScreen prefScreen = getPreferenceScreen();

        mStatusBarClockCategory = prefScreen.findPreference(CATEGORY_CLOCK);
        mStatusBarClock = findPreference(STATUS_BAR_CLOCK);
        mStatusBarClock.setOnPreferenceChangeListener(this);

        mStatusBarBatteryCategory = prefScreen.findPreference(CATEGORY_BATTERY);
        mStatusBarBatteryShowPercent = findPreference(STATUS_BAR_SHOW_BATTERY_PERCENT);
        mStatusBarBattery = findPreference(STATUS_BAR_BATTERY_STYLE);
        mStatusBarBattery.setOnPreferenceChangeListener(this);
        enableStatusBarBatteryDependents(mStatusBarBattery.getIntValue(0));

        mShowFourg = findPreference(KEY_SHOW_FOURG);
        mOldMobileType = findPreference(KEY_USE_OLD_MOBILETYPE);

        if (!TelephonyUtils.isVoiceCapable(getActivity())) {
            prefScreen.removePreference(mShowFourg);
            prefScreen.removePreference(mOldMobileType);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        final String curIconBlacklist = Settings.Secure.getString(getContext().getContentResolver(),
                ICON_BLACKLIST);

        if (TextUtils.delimitedStringContains(curIconBlacklist, ',', "clock")) {
            getPreferenceScreen().removePreference(mStatusBarClockCategory);
        } else {
            getPreferenceScreen().addPreference(mStatusBarClockCategory);
        }

        if (TextUtils.delimitedStringContains(curIconBlacklist, ',', "battery")) {
            getPreferenceScreen().removePreference(mStatusBarBatteryCategory);
        } else {
            getPreferenceScreen().addPreference(mStatusBarBatteryCategory);
        }

        if (isNetworkTrafficOnStatusBar()){
            mStatusBarClock.setEnabled(false);
            mStatusBarClock.setSummary(R.string.status_bar_clock_position_disabled_summary);
        }else{
            int value = Settings.System.getInt(getContentResolver(),
                Settings.System.STATUS_BAR_CLOCK, CLOCK_POSITION_DEFAULT);
            mStatusBarClock.setEnabled(true);
            mStatusBarClock.setValue(String.valueOf(value));
            updateClockSummary(value);
            boolean disallowCenteredClock = CutoutUtils.hasCenteredCutout(getActivity());
            // Adjust status bar preferences for RTL
            if (getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
                if (disallowCenteredClock) {
                    mStatusBarClock.setEntries(R.array.status_bar_clock_position_entries_notch_rtl);
                    mStatusBarClock.setEntryValues(R.array.status_bar_clock_position_values_notch_rtl);
                } else {
                    mStatusBarClock.setEntries(R.array.status_bar_clock_position_entries_rtl);
                    mStatusBarClock.setEntryValues(R.array.status_bar_clock_position_values_rtl);
                }
            } else if (disallowCenteredClock) {
                mStatusBarClock.setEntries(R.array.status_bar_clock_position_entries_notch);
                mStatusBarClock.setEntryValues(R.array.status_bar_clock_position_values_notch);
            } else {
                mStatusBarClock.setEntries(R.array.status_bar_clock_position_entries);
                mStatusBarClock.setEntryValues(R.array.status_bar_clock_position_values);
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        int value = Integer.parseInt((String) newValue);
        String key = preference.getKey();
        switch (key) {
            case STATUS_BAR_BATTERY_STYLE:
                enableStatusBarBatteryDependents(value);
                break;
            case STATUS_BAR_CLOCK:
                updateClockSummary(value);
                break;
        }
        return true;
    }

    private void enableStatusBarBatteryDependents(int batteryIconStyle) {
        mStatusBarBatteryShowPercent.setEnabled(batteryIconStyle != STATUS_BAR_BATTERY_STYLE_TEXT &&
                batteryIconStyle != STATUS_BAR_BATTERY_STYLE_HIDDEN);
    }

    private void updateClockSummary(int value){
        mStatusBarClock.setSummary(getClockPositionSummary(value));
    }

    private String getClockPositionSummary(int value){
        if (value == CLOCK_POSITION_HIDE){
            return getContext().getString(R.string.status_bar_clock_position_hidden);
        } else if (value == CLOCK_POSITION_RIGHT) {
            return getContext().getString(R.string.status_bar_clock_position_right);
        } else if (value == CLOCK_POSITION_CENTER) {
            return getContext().getString(R.string.status_bar_clock_position_center);
        } else {
            return getContext().getString(R.string.status_bar_clock_position_left);
        }
    }

    private boolean isNetworkTrafficOnStatusBar(){
        int mode = Settings.System.getInt(getContentResolver(),
                Settings.System.NETWORK_TRAFFIC_LOCATION, 0);
        return mode == 1;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.ALPHA;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.sb_settings);

}
