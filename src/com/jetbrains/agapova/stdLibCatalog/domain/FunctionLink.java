package com.jetbrains.agapova.stdLibCatalog.domain;

import org.joda.time.DateTime;

/**
 * Created by ashatta on 7/10/14.
 */
public class FunctionLink {
    private FunctionEntity first;
    private FunctionEntity second;
    private User author;
    private DateTime date;

    public FunctionLink(FunctionEntity first, FunctionEntity second, User author, DateTime date) {
        this.first = first;
        this.second = second;
        this.author = author;
        this.date = date;
    }

    public FunctionEntity getFirst() {
        return first;
    }

    public FunctionEntity getSecond() {
        return second;
    }

    public User getAuthor() {
        return author;
    }

    public DateTime getDate() {
        return date;
    }
}
