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

package cz.webz.ss11mik.notifitator.services;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import cz.webz.ss11mik.notifitator.backend.Repository;

public class SyncJobService extends JobService {

    private static final int JOB_SYNC_ID = 0;

    @Override
    public boolean onStartJob(JobParameters params) {
        Repository repository = new Repository(this);
        repository.fetch(new Repository.FetchCallback() {
            @Override
            public void fetched(boolean success) {

            }
        }, this);

        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }

    public static void scheduleSyncJob(Context context) {
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(context);

        if (preferences.getBoolean("first_run", true)) {
            JobScheduler scheduler = (JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE);
            scheduler.cancelAll();

            long syncPeriod = Long.parseLong(preferences.getString("sync_period", "-1"));

            if (syncPeriod == -1)
                return;


            ComponentName serviceName = new ComponentName(
                    context.getPackageName(),
                    SyncJobService.class.getName());
            JobInfo.Builder builder = new JobInfo.Builder(JOB_SYNC_ID, serviceName)
                    .setRequiresDeviceIdle(false)
                    .setPersisted(true)
                    .setPeriodic(syncPeriod)
                    .setRequiresCharging(false);

            if (preferences.getBoolean("sync_cellular", false))
                builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
            else
                builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED);

            scheduler.schedule(builder.build());
            preferences.edit().putBoolean("first_run", false).apply();
        }
    }

    public static void rescheduleSyncJob(Context context, long syncPeriod) {
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(context);

        JobScheduler scheduler = (JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE);
        scheduler.cancelAll();

        if (syncPeriod == -1)
            return;

        ComponentName serviceName = new ComponentName(
                context.getPackageName(),
                SyncJobService.class.getName());
        JobInfo.Builder builder = new JobInfo.Builder(JOB_SYNC_ID, serviceName)
                .setRequiresDeviceIdle(false)
                .setPersisted(true)
                .setPeriodic(syncPeriod)
                .setRequiresCharging(false);

        if (preferences.getBoolean("sync_cellular", false))
            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        else
            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED);

        scheduler.schedule(builder.build());

    }
}
