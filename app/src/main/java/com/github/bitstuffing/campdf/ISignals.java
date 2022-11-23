package com.github.bitstuffing.campdf;

/**
 * ISignals values
 */
public interface ISignals {
    // Request code for selecting a PDF document.
    final int REQUEST_CODE = 2;

    // handler message values
    final int ERROR_MESSAGE = -1;
    final int INFO_MESSAGE = 0;
    final int LOADING = 1;
    final int LOADED = 3;
    final int SHOW_ALERT = 10;
    final int REFRESH_LISTVIEW = 20;
}
