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

package cz.webz.ss11mik.notifitator.backend;

import android.app.Application;
import android.content.Context;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import cz.webz.ss11mik.notifitator.containers.Course;
import cz.webz.ss11mik.notifitator.containers.Event;

public class ViewModel extends AndroidViewModel {


    private Repository repository;

    public ViewModel(Application application) {
        super(application);
        repository = new Repository(application);
    }

    public void fetch(Repository.FetchCallback callback, Context context) {
        repository.fetch(callback, context);
    }


    public LiveData<List<Event>> getEvents() {
        return repository.getEvents();
    }

    public List<Event> getTodayEvents() {
        return repository.getTodayEvents();
    }

    public List<Event> getTomorrowEvents() {
        return repository.getTomorrowEvents();
    }



    public void insertCourse(final Course course) {
        repository.insertCourse(course);
    }

    public LiveData<Course> getCourseByName(String name) {
        return repository.getCourseByName(name);
    }
}


