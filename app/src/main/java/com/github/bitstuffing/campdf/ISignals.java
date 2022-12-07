package com.github.bitstuffing.campdf;

/**
 * ISignals values
 */
public interface ISignals {
    // Request code for selecting a PDF document.
    public static final int REQUEST_CODE = 2;

    // handler message values
    public static final int ERROR_MESSAGE = -1;
    public static final int INFO_MESSAGE = 0;
    public static final int LOADING = 1;
    public static final int LOADED = 3;
    public static final int SHOW_ALERT = 10;
    public static final int REFRESH_LISTVIEW = 20;
    public static final int REFRESH = 11;
}
