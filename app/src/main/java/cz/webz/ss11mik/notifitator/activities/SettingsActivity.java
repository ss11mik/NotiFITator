/*
 * This file is part of the NotiFITator distribution (https://github.com/ss11mik/NotiFITator).
 *  Copyright (c) 2020 Ondřej Mikula.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, version 3.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package cz.webz.ss11mik.notifitator.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Browser;
import android.text.InputType;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.security.GeneralSecurityException;

import cz.webz.ss11mik.notifitator.R;
import cz.webz.ss11mik.notifitator.services.SyncJobService;
import cz.webz.ss11mik.notifitator.ui.TimeDialog;
import cz.webz.ss11mik.notifitator.ui.TimePreference;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(this);

        int appTheme = Integer.parseInt(preferences.getString("theme", "0")) == 0 ?
                R.style.SettingsTheme : R.style.SettingsThemeDark;
        setTheme(appTheme);

        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setBackgroundDrawable(new ColorDrawable(getColor(R.color.colorPrimary)));
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        static final String FRAGMENT_TAG = "settings_fragment";

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);

            final SharedPreferences preferences = getPreferenceManager().getSharedPreferences();

            ((EditTextPreference) findPreference("username")).setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
                @Override
                public void onBindEditText(@NonNull EditText editText) {
                    editText.setSingleLine();
                    editText.selectAll();
                }
            });

            EditTextPreference password = findPreference("password");
            password.setOnBindEditTextListener(
                    new EditTextPreference.OnBindEditTextListener() {
                        @Override
                        public void onBindEditText(@NonNull EditText editText) {
                            editText.setSingleLine();
                            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                          //  editText.setSingleLine();
                        }
                    });
            password.setSummaryProvider(new Preference.SummaryProvider() {
                @Override
                public CharSequence provideSummary(Preference preference) {
                    boolean passwordSet =
                            PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean(
                                    "password_set",
                                    false);

                    if (passwordSet) {
                        return getString(R.string.mock_password);
                    } else {
                        return getString(R.string.not_set);
                    }
                }
            });
            password.setOnPreferenceChangeListener( new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);

                            SharedPreferences sharedPreferences = EncryptedSharedPreferences.create(
                                    "login",
                                    masterKeyAlias,
                                    getContext(),
                                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                            );

                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString(preference.getKey(), (String) newValue);
                            editor.apply();
                        }
                        catch (IOException | GeneralSecurityException e) {

                        }
                    }
                });

                SharedPreferences preferences = PreferenceManager
                        .getDefaultSharedPreferences(getContext());
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean("password_set", ((String) newValue).length() > 0);
                editor.apply();

                return true;
            }});


            Preference syncPeriod = findPreference("sync_period");
            syncPeriod.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    SyncJobService.rescheduleSyncJob(getContext(), Long.valueOf((String) newValue));
                    return true;
                }
            });

            Preference syncCellular = findPreference("sync_cellular");
            syncCellular.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    SyncJobService.rescheduleSyncJob(getContext(), Long.valueOf(preferences.getString("sync_period", "-1")));
                    return true;
                }
            });


            Preference developer = findPreference("developer");
            developer.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent view = new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(getString(R.string.url_ss11mik)));
                    Bundle bundle = new Bundle();
                    view.putExtra(Browser.EXTRA_HEADERS, bundle);
                    startActivity(view);
                    return false;
                }
            });
        }

        @Override
        public void onDisplayPreferenceDialog(Preference preference) {
            DialogFragment dialogFragment = null;
            if (preference instanceof TimePreference) {
                dialogFragment = new TimeDialog((TimePreference) preference);
            }

            if (dialogFragment != null) {
                dialogFragment.setTargetFragment(this, 0);
                dialogFragment.show(getFragmentManager(), FRAGMENT_TAG);
            } else {
                super.onDisplayPreferenceDialog(preference);
            }
        }
    }
}