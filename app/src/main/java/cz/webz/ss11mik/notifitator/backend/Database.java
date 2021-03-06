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

import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import cz.webz.ss11mik.notifitator.containers.Course;
import cz.webz.ss11mik.notifitator.containers.Event;
import cz.webz.ss11mik.notifitator.backend.converters.DateConverter;

@androidx.room.Database(entities = {Event.class, Course.class}, version = 3, exportSchema = false)
@TypeConverters({DateConverter.class})
public abstract class Database extends RoomDatabase {
    public abstract MyDao eventDao();
}
