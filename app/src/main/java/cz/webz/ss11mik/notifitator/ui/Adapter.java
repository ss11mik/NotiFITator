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

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.Filter;
import android.widget.TextView;

import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import cz.webz.ss11mik.notifitator.R;
import cz.webz.ss11mik.notifitator.activities.MainActivity;
import cz.webz.ss11mik.notifitator.backend.Repository;
import cz.webz.ss11mik.notifitator.containers.Course;
import cz.webz.ss11mik.notifitator.containers.Event;

public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

    private LayoutInflater inflater;

    private List<Event> originalData;
    private List<Event> filteredData;

    private Chip filterChip;
    private Repository repository;
    private Context context;

    public Adapter(Context context, List<Event> data, final Chip filterChip) {
        this.inflater = LayoutInflater.from(context);
        this.originalData = data;
        this.filteredData = data;
        this.context = context;
        this.filterChip = filterChip;

        repository = new Repository(context);

        filterChip.setOnCloseIconClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filterChip.setVisibility(View.GONE);
                getCourseFilter().filter("");
            }
        });

        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        return filteredData.get(position).hashCode();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.row_event, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, int position) {
        final Event event = filteredData.get(position);

        viewHolder.date.setText(event.getFormattedDate());
        viewHolder.course.setText(event.getCourse());
        viewHolder.type.setText(event.getType());
        viewHolder.text.setText(event.getText());

        viewHolder.type.setChipBackgroundColor(ColorStateList.valueOf(
                context.getColor(getColorByType(event.getType()))));

        repository.getCourseByName(event.getCourse()).observe((MainActivity) context, new Observer<Course>() {
            @Override
            public void onChanged(Course course) {
            /*    if (course == null) {
                    int color = getRandomColor();
                    repository.insertCourse(new Course(event.getCourse(), color));
                    viewHolder.course.setChipBackgroundColor(ColorStateList.valueOf(color));
                }
                else {
                    viewHolder.course.setChipBackgroundColor(ColorStateList.valueOf(course.getColor()));
                }*/
                viewHolder.course.setChipBackgroundColor(ColorStateList.valueOf(course.getColor()));

                viewHolder.course.setVisibility(View.VISIBLE);
            }
        });

        viewHolder.course.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateSelectedChip((Chip) v, filterChip);

                String course = ((Chip) v).getText().toString();
                getCourseFilter().filter(course);
            }
        });

        viewHolder.type.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateSelectedChip((Chip) v, filterChip);

                String type = ((Chip) v).getText().toString();
                getTypeFilter().filter(type);
            }
        });
    }

    private int getColorByType (String type) {
        switch (type) {
            case "task":
            case "termín":
                return R.color.task;
            case "exam":
            case "zkouška":
                return R.color.exam;
            case "accr":
            case "zápočet":
                return R.color.accr;
            case "survey":
            case "dotazník":
                return R.color.survey;
            case "info":
            default:
                return R.color.info;
        }
    }

    @Override
    public int getItemCount() {
        return filteredData.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private Chip course;
        private Chip date;
        private Chip type;
        private TextView text;

        ViewHolder(View itemView) {
            super(itemView);
            date = itemView.findViewById(R.id.chpEventDate);
            course = itemView.findViewById(R.id.chpEventCourse);
            type = itemView.findViewById(R.id.chpEventType);
            text = itemView.findViewById(R.id.txtEventText);
        }
    }

    public static class MyGrid extends LinearLayoutManager {

        public MyGrid (Context context) {
            super(context);
        }

        @Override
        public boolean supportsPredictiveItemAnimations() {
            return true;
        }
    }

    Event getItem(int id) {
        return filteredData.get(id);
    }


    public Filter getCourseFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();

                final List<Event> list = originalData;

                int count = list.size();
                final ArrayList<Event> nlist = new ArrayList<>(count);

                Event filterableEvent;

                for (int i = 0; i < count; i++) {
                    filterableEvent = list.get(i);
                    if (filterableEvent.getCourse().equals(constraint))
                        nlist.add(filterableEvent);
                }

                results.values = nlist;
                results.count = nlist.size();

                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results.count == 0 || constraint == "") {
                    filteredData = originalData;
                    notifyDataSetChanged();
                }
                else {
                    filteredData = (ArrayList<Event>) results.values;

                }
                notifyDataSetChanged();
            }
        };
    }

    public Filter getTypeFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();

                final List<Event> list = originalData;

                int count = list.size();
                final ArrayList<Event> nlist = new ArrayList<>(count);

                Event filterableEvent;

                for (int i = 0; i < count; i++) {
                    filterableEvent = list.get(i);
                    if (filterableEvent.getType().equals(constraint))
                        nlist.add(filterableEvent);
                }

                results.values = nlist;
                results.count = nlist.size();

                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results.count == 0 || constraint == "") {
                    filteredData = originalData;
                }
                else {
                    filteredData = (ArrayList<Event>) results.values;

                }
                notifyDataSetChanged();
            }
        };
    }

    private void animateSelectedChip (final Chip chip, final Chip filter) {

        int[] chipLocation = new int[2];
        chip.getLocationOnScreen(chipLocation);

        int[] filterLocation = new int[2];
        filter.getLocationOnScreen(filterLocation);

        chipLocation[0] += chip.getMeasuredWidth();
        if (filter.getVisibility() == View.VISIBLE)
            filterLocation[0] += filter.getMeasuredWidth();

        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        int densityDpi = dm.densityDpi;

        double distance =
                Math.sqrt(
                        Math.pow(chipLocation[0] - filterLocation[0], 2)
                                + Math.pow(chipLocation[1] - filterLocation[1], 2));

        TranslateAnimation anim = new TranslateAnimation(
                chipLocation[0] - filterLocation[0],
                0,
                chipLocation[1] - filterLocation[1],
                0);

        anim.setDuration((long) (distance * densityDpi / 1716));

        filter.setVisibility(View.VISIBLE);
        filter.setText(chip.getText().toString());
        filter.setChipBackgroundColor(chip.getChipBackgroundColor());

        anim.setInterpolator(new AccelerateDecelerateInterpolator());

        filter.startAnimation(anim);
    }
}
