package org.jetbrains.stdLibCatalog.domain;

import org.joda.time.DateTime;

public class TypeLink {
    private TypeConstructor first;
    private TypeConstructor second;
    private User author;
    private DateTime date;

    public TypeLink(TypeConstructor first, TypeConstructor second, User author, DateTime date) {
        this.first = first;
        this.second = second;
        this.author = author;
        this.date = date;
    }

    public TypeConstructor getFirst() {
        return first;
    }

    public TypeConstructor getSecond() {
        return second;
    }

    public User getAuthor() {
        return author;
    }

    public DateTime getDate() {
        return date;
    }
}
