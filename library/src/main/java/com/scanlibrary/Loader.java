package com.scanlibrary;

class Loader {
    private static boolean done = false;

    protected static synchronized void load() {
        if (done)
            return;
        
        System.loadLibrary("Scanner");
        System.loadLibrary("opencv_java3");
        
        done = true;
    }
}

