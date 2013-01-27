package com.recordat.Recordat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Bookmark {
    long time;
    String text;

    public Bookmark(long time) {
        this.time = time;
    }

    public void setText(String text) {
        this.text = text;
    }

    public long getTime() {
        return time;
    }

    public String getText() {
        return text;
    }

    public String toString() {
        return text != null ? text : new SimpleDateFormat("mm:ss").format(new Date(time));
    }
}
