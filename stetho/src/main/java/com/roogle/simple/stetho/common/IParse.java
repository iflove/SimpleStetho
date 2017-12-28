package com.roogle.simple.stetho.common;

import android.support.annotation.NonNull;

import org.json.JSONException;

import java.text.ParseException;

public interface IParse<T> {
    T parse(@NonNull String s) throws JSONException, ParseException;
}
