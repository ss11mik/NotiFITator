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

package cz.webz.ss11mik.notifitator.ui;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceDialogFragmentCompat;

import java.util.Calendar;
import java.util.Date;

import cz.webz.ss11mik.notifitator.R;
import cz.webz.ss11mik.notifitator.services.NotificationWork;

public class TimeDialog extends PreferenceDialogFragmentCompat {

    private final TimePreference preference;
    private TimePicker picker;

    public TimeDialog(TimePreference preference) {
        this.preference = preference;

        final Bundle b = new Bundle();
        b.putString(ARG_KEY, preference.getKey());
        setArguments(b);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().setTitle("");
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    protected View onCreateDialogView(Context context) {

        Date previousTime = new Date(preference.getTime());
        boolean is24hour = DateFormat.is24HourFormat(getContext());

        picker = new TimePicker(
                getContext());
        picker.setHour(previousTime.getHours());
        picker.setMinute(previousTime.getMinutes());
        picker.setIs24HourView(is24hour);
        return picker;
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {

            Date time = new Date();
            time.setHours(picker.getHour());
            time.setMinutes(picker.getMinute());
            time.setSeconds(0);

            time.setYear(0);
            time.setMonth(0);
            time.setDate(0);

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(time);

            if (preference.callChangeListener(time)) {
                preference.setTime(calendar.getTimeInMillis());
                NotificationWork.scheduleNotificationWork(getContext(), time.getTime());
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }
}
