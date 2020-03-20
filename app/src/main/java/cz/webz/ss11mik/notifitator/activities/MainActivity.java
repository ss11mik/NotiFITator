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
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.chip.Chip;

import java.util.List;

import cz.webz.ss11mik.notifitator.R;
import cz.webz.ss11mik.notifitator.backend.Repository;
import cz.webz.ss11mik.notifitator.backend.ViewModel;
import cz.webz.ss11mik.notifitator.containers.Event;
import cz.webz.ss11mik.notifitator.services.NotificationWork;
import cz.webz.ss11mik.notifitator.services.SyncJobService;
import cz.webz.ss11mik.notifitator.ui.Adapter;

public class MainActivity extends AppCompatActivity {

    private ViewModel viewModel;

    SwipeRefreshLayout refreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(this);
        long notificationTime = preferences.getLong("notification_time", -1);

        SyncJobService.scheduleSyncJob(this);
        NotificationWork.scheduleNotificationWork(this, notificationTime);

        int appTheme = Integer.parseInt(preferences.getString("theme", "0")) == 0 ?
                R.style.AppTheme : R.style.AppThemeDark;
        setTheme(appTheme);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final RecyclerView recyclerView = findViewById(R.id.recyclerEvents);
        recyclerView.setLayoutManager(new Adapter.MyGrid(this));

        final MainActivity context = this;

        refreshLayout = findViewById(R.id.swipe);
        refreshLayout.setColorSchemeResources(
                android.R.color.white,
                android.R.color.white,
                android.R.color.white);
        refreshLayout.setProgressBackgroundColorSchemeResource(R.color.colorPrimaryDark);

        viewModel = ViewModelProviders.of(this).get(ViewModel.class);
        viewModel.getEvents().observe(this, new Observer<List<Event>>() {
            @Override
            public void onChanged(List<Event> events) {
                TextView placeholder = findViewById(R.id.txtNoEvents);

                if (events.size() > 0) {
                    placeholder.setVisibility(View.INVISIBLE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
                else {
                    placeholder.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.INVISIBLE);
                }

                Adapter adapter = new Adapter(
                        context,
                        events,
                        (Chip) findViewById(R.id.chpFilterCourse));
                recyclerView.setAdapter(adapter);
            }
        });

        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                viewModel.fetch(new Repository.FetchCallback() {
                    @Override
                    public void fetched(final boolean success) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                refreshLayout.setRefreshing(false);
                                if (!success)
                                    Toast.makeText(context, getString(R.string.err_fetch), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }, context);
            }
        });

        Toolbar toolbar = findViewById(R.id.toolbarMain);
        setSupportActionBar(toolbar);
    }


    public void goToSettings(View v) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
}
