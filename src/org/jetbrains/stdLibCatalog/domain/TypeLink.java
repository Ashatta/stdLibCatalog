package org.jetbrains.stdLibCatalog.domain;

import org.joda.time.DateTime;

public class TypeLink {
    private Classifier first;
    private Classifier second;
    private User author;
    private DateTime date;

    public TypeLink(Classifier first, Classifier second, User author, DateTime date) {
        this.first = first;
        this.second = second;
        this.author = author;
        this.date = date;
    }

    public Classifier getFirst() {
        return first;
    }

    public Classifier getSecond() {
        return second;
    }

    public User getAuthor() {
        return author;
    }

    public DateTime getDate() {
        return date;
    }
}
