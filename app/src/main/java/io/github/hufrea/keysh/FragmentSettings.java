package io.github.hufrea.keysh;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import io.github.hufrea.keysh.R;

public class FragmentSettings extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings_preferences, rootKey);
    }
}