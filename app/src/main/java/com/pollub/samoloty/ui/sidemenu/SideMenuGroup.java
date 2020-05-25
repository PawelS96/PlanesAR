/*===============================================================================
Copyright (c) 2018 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2015 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.
===============================================================================*/


package com.pollub.samoloty.ui.sidemenu;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;

import com.vuforia.artest.R;

import java.lang.ref.WeakReference;

/**
 * With this class, you can create menu options as part of a group
 * You can add text, radio buttons, and switches
 */
public class SideMenuGroup {
    private final WeakReference<Activity> mActivityRef;
    private final WeakReference<SampleAppMenuInterface> mMenuInterfaceRef;
    private final LinearLayout mLayout;
    private final LayoutParams mLayoutParams;
    private final LayoutInflater inflater;
    private final int dividerResource;

    private final float mEntriesTextSize;
    private final int mEntriesSidesPadding;
    private final int mEntriesUpDownPadding;
    private final int mEntriesUpDownRadioPadding;
    private final Typeface mFont;

    private final int selectorResource;

    private final SideMenu mSideMenu;
    private RadioGroup mRadioGroup;

    private final OnClickListener mClickListener;
    private final OnCheckedChangeListener mOnCheckedListener;
    private final OnCheckedChangeListener mOnRadioCheckedListener;

    private int fontColor, fontColorAlt, accentColor;


    @SuppressLint("InflateParams")
    public SideMenuGroup(SampleAppMenuInterface menuInterface,
                         Activity context, SideMenu parent, boolean hasTitle, String title,
                         int width) {

        fontColor = context.getColor(R.color.colorFont);
        fontColorAlt = context.getColor(R.color.colorFontAlt);
        accentColor = context.getColor(R.color.colorAccent);

        mActivityRef = new WeakReference<>(context);
        mMenuInterfaceRef = new WeakReference<>(menuInterface);
        mSideMenu = parent;
        mLayoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        inflater = LayoutInflater.from(mActivityRef.get());
        mLayout = (LinearLayout) inflater.inflate(
                R.layout.sample_app_menu_group, null, false);
        mLayout.setLayoutParams(new LinearLayout.LayoutParams(width,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        mEntriesTextSize = mActivityRef.get().getResources().getDimension(
                R.dimen.menu_entries_text);

        mEntriesSidesPadding = (int) mActivityRef.get().getResources().getDimension(
                R.dimen.menu_entries_sides_padding);
        mEntriesUpDownPadding = (int) mActivityRef.get().getResources().getDimension(
                R.dimen.menu_entries_top_down_padding);
        mEntriesUpDownRadioPadding = (int) mActivityRef.get().getResources()
                .getDimension(R.dimen.menu_entries_top_down_radio_padding);
        dividerResource = R.layout.sample_app_menu_group_divider;

        selectorResource = android.R.drawable.list_selector_background;

        mFont = Typeface.create("sans-serif", Typeface.NORMAL);

        TextView titleView = mLayout.findViewById(R.id.menu_group_title);
        titleView.setText(title);
        titleView.setTextSize(mActivityRef.get().getResources().getDimension(
                R.dimen.menu_entries_title));
        titleView.setTextColor(fontColorAlt);
        titleView.setClickable(false);

        if (!hasTitle) {
            mLayout.removeView(titleView);
            View dividerView = mLayout
                    .findViewById(R.id.menu_group_title_divider);
            mLayout.removeView(dividerView);
        }

        mClickListener = v -> {
            int command = Integer.parseInt(v.getTag().toString());
            mMenuInterfaceRef.get().menuProcess(command);
            mSideMenu.hideMenu();
        };

        mOnCheckedListener = (switchView, isChecked) -> {
            boolean result;
            int command = Integer.parseInt(switchView.getTag().toString());
            result = mMenuInterfaceRef.get().menuProcess(command);
            if (!result) {
                switchView.setChecked(!isChecked);
            } else
                mSideMenu.hideMenu();

            Switch sw = (Switch)switchView;

            sw.getThumbDrawable().setColorFilter(isChecked ? accentColor : Color.WHITE, PorterDuff.Mode.MULTIPLY);
            sw.getTrackDrawable().setColorFilter(isChecked ? accentColor : Color.WHITE, PorterDuff.Mode.MULTIPLY);
        };

        mOnRadioCheckedListener = (switchView, isChecked) -> {
            if (isChecked) {
                boolean result;
                int command = Integer.parseInt(switchView.getTag().toString());
                result = mMenuInterfaceRef.get().menuProcess(command);
                if (result) {
                    mSideMenu.hideMenu();
                }
            }
        };
    }


    @SuppressWarnings({"deprecation", "UnusedReturnValue"})
    public View addTextItem(String text, int command) {
        Drawable selectorDrawable = mActivityRef.get().getResources().getDrawable(
                selectorResource);

        TextView newTextView = new TextView(mActivityRef.get());
        newTextView.setText(text);

        newTextView.setBackgroundDrawable(selectorDrawable);

        newTextView.setTypeface(mFont);
        newTextView.setTextSize(mEntriesTextSize);
        newTextView.setTag(command);
        newTextView.setVisibility(View.VISIBLE);
        newTextView.setPadding(mEntriesSidesPadding, mEntriesUpDownPadding,
                mEntriesSidesPadding, mEntriesUpDownPadding);
        newTextView.setClickable(true);
        newTextView.setOnClickListener(mClickListener);
        newTextView.setTextColor(fontColorAlt);
        mLayout.addView(newTextView, mLayoutParams);

        View divider = inflater.inflate(dividerResource, null);
        mLayout.addView(divider, mLayoutParams);

        return newTextView;
    }

    // Add a switch menu option
    @SuppressWarnings("deprecation")
    public View addSelectionItem(String text, int command, boolean on) {

        Drawable selectorDrawable = mActivityRef.get().getResources().getDrawable(selectorResource);
        View returnView;

        Switch newSwitchView = new Switch(mActivityRef.get());
        newSwitchView.setText(text);

        //newSwitchView.setBackground(selectorDrawable);
        newSwitchView.getThumbDrawable().setColorFilter(on ? accentColor : Color.WHITE, PorterDuff.Mode.MULTIPLY);
        newSwitchView.getTrackDrawable().setColorFilter(on ? accentColor : Color.WHITE, PorterDuff.Mode.MULTIPLY);



        newSwitchView.setTypeface(mFont);
        newSwitchView.setTextSize(mEntriesTextSize);
        newSwitchView.setTag(command);
        newSwitchView.setVisibility(View.VISIBLE);
        newSwitchView.setTextColor(fontColor);
        newSwitchView.setPadding(mEntriesSidesPadding, mEntriesUpDownPadding, mEntriesSidesPadding, mEntriesUpDownPadding);
        newSwitchView.setChecked(on);
        newSwitchView.setOnCheckedChangeListener(mOnCheckedListener);
        mLayout.addView(newSwitchView, mLayoutParams);
        returnView = newSwitchView;

        View divider = inflater.inflate(dividerResource, null);
        mLayout.addView(divider, mLayoutParams);

        return returnView;
    }

    @SuppressLint("InflateParams")
    @SuppressWarnings({"deprecation", "UnusedReturnValue"})
    public View addRadioItem(String text, int command, boolean isSelected) {
        if (mRadioGroup == null) {
            mRadioGroup = new RadioGroup(mActivityRef.get());
            mRadioGroup.setVisibility(View.VISIBLE);
            mLayout.addView(mRadioGroup, mLayoutParams);
        }

        Drawable selectorDrawable = mActivityRef.get().getResources().getDrawable(
                selectorResource);

        RadioButton newRadioButton = (RadioButton) inflater.inflate(
                R.layout.sample_app_menu_group_radio_button, null, false);

        newRadioButton.setText(text);
        newRadioButton.setBackground(selectorDrawable);
        newRadioButton.setTypeface(mFont);
        newRadioButton.setTextSize(mEntriesTextSize);
        newRadioButton.setPadding(mEntriesSidesPadding,
                mEntriesUpDownRadioPadding, mEntriesSidesPadding,
                mEntriesUpDownRadioPadding);
        newRadioButton.setCompoundDrawablePadding(0);
        newRadioButton.setTag(command);
        newRadioButton.setVisibility(View.VISIBLE);
        mRadioGroup.addView(newRadioButton, mLayoutParams);

        View divider = inflater.inflate(dividerResource, null);
        mRadioGroup.addView(divider, mLayoutParams);

        if (isSelected) {
            mRadioGroup.check(newRadioButton.getId());
        }

        // Set the listener after changing the UI state to avoid calling the radio button functionality when creating the menu 
        newRadioButton.setOnCheckedChangeListener(mOnRadioCheckedListener);

        return newRadioButton;
    }


    LinearLayout getMenuLayout() {
        return mLayout;
    }
}
