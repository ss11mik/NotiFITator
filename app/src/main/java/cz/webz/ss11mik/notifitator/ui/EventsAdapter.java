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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.lifecycle.Observer;

import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import cz.webz.ss11mik.notifitator.containers.Course;
import cz.webz.ss11mik.notifitator.containers.Event;
import cz.webz.ss11mik.notifitator.activities.MainActivity;
import cz.webz.ss11mik.notifitator.R;
import cz.webz.ss11mik.notifitator.backend.ViewModel;

public class EventsAdapter extends ArrayAdapter<Event> implements Filterable {


    private List<Event> originalData;
    private List<Event> filteredData;

    private Chip filterChip;
    private ViewModel viewModel;
    private Context context;

    private static class ViewHolder {
        private Chip course;
        private Chip date;
        private Chip type;
        private TextView text;

        int position;
    }

    public EventsAdapter(List<Event> data, Context context, final Chip filterChip, ViewModel viewModel) {
        super(context, R.layout.row_event, data);
        this.originalData = data;
        this.filteredData = data;
        this.filterChip = filterChip;
        this.viewModel = viewModel;
        this.context = context;

        filterChip.setOnCloseIconClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filterChip.setVisibility(View.GONE);
                getCourseFilter().filter("");
            }
        });
    }


    @Override
    public View getView(final int position, View view, ViewGroup parent) {

        final Event event = getItem(position);

        if (event.getCourse() == null)
            return null;

        final ViewHolder viewHolder;

        if (view == null) {

            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            view = inflater.inflate(R.layout.row_event, parent, false);

            viewHolder.date = view.findViewById(R.id.chpEventDate);
            viewHolder.course = view.findViewById(R.id.chpEventCourse);
            viewHolder.type = view.findViewById(R.id.chpEventType);
            viewHolder.text = view.findViewById(R.id.txtEventText);

            view.setTag(viewHolder);
        } else
            viewHolder = (ViewHolder) view.getTag();

        viewHolder.date.setText(event.getFormattedDate());
        viewHolder.course.setText(event.getCourse());
        viewHolder.type.setText(event.getType());
        viewHolder.text.setText(event.getText());

        switch (event.getType()) {
            case "task":
            case "termín":
                viewHolder.type.setChipBackgroundColor(ColorStateList.valueOf((getContext().getColor(R.color.task))));
                break;
            case "exam":
            case "zkouška":
                viewHolder.type.setChipBackgroundColor(ColorStateList.valueOf(getContext().getColor(R.color.exam)));
                break;
            case "accr":
            case "zápočet":
                viewHolder.type.setChipBackgroundColor(ColorStateList.valueOf((getContext().getColor(R.color.accr))));
                break;
            case "survey":
            case "dotazník":
                viewHolder.type.setChipBackgroundColor(ColorStateList.valueOf((getContext().getColor(R.color.survey))));
                break;
            case "info":
                viewHolder.type.setChipBackgroundColor(ColorStateList.valueOf((getContext().getColor(R.color.info))));
                break;
        }

        viewModel.getCourseByName(event.getCourse()).observe((MainActivity) context, new Observer<Course>() {
            @Override
            public void onChanged(Course course) {
                if (course == null) {
                    int color = getRandomColor();
                    viewModel.insertCourse(new Course(event.getCourse(), color));
                    viewHolder.course.setChipBackgroundColor(ColorStateList.valueOf(color));
                }
                else {
                    viewHolder.course.setChipBackgroundColor(ColorStateList.valueOf(course.getColor()));
                }
                viewHolder.course.setVisibility(View.VISIBLE);
            }
        });




     //   viewHolder.course.setChipBackgroundColor(ColorStateList.valueOf(getRandomColor()));

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

        viewHolder.position = position;

        return view;
    }

    int getRandomColor() {
        float hue = new Random().nextInt(18) * 20;
        float[] hsv = {hue, 0.7f, 0.6f};
        return Color.HSVToColor(hsv);
    }

    @Override
    public Event getItem(int position) {
        return filteredData.get(position);
    }

    @Override
    public int getCount() {
        return filteredData.size();
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
        filterLocation[0] += filter.getMeasuredWidth();

        TranslateAnimation anim = new TranslateAnimation(
                0,
                filterLocation[0] - chipLocation[0],
                0,
                filterLocation[1] - chipLocation[1]);
        anim.setDuration(500);

        anim.setAnimationListener(new TranslateAnimation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
                filterChip.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) { }

            @Override
            public void onAnimationEnd(Animation animation) {
                filterChip.setVisibility(View.VISIBLE);
                filterChip.setText(chip.getText().toString());
                filterChip.setChipBackgroundColor(chip.getChipBackgroundColor());
            }
        });

        chip.startAnimation(anim);
    }
}
