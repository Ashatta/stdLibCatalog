package com.jetbrains.agapova.stdLibCatalog.domain;

import org.joda.time.DateTime;

/**
 * Created by ashatta on 7/10/14.
 */
/* I have no separate classes fo ClassLink and InterfaceLink because it is possible for a class to have a link
 * to an interface and vice versa
 */
public class TypeLink {
    private TypeEntity first;
    private TypeEntity second;
    private User author;
    private DateTime date;

    public TypeLink(TypeEntity first, TypeEntity second, User author, DateTime date) {
        this.first = first;
        this.second = second;
        this.author = author;
        this.date = date;
    }

    public TypeEntity getFirst() {
        return first;
    }

    public TypeEntity getSecond() {
        return second;
    }

    public User getAuthor() {
        return author;
    }

    public DateTime getDate() {
        return date;
    }
}
