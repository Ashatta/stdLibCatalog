package org.jetbrains.stdLibCatalog.domain;

import org.joda.time.DateTime;

public class FunctionLink {
    private MemberEntity first;
    private MemberEntity second;
    private User author;
    private DateTime date;

    public FunctionLink(MemberEntity first, MemberEntity second, User author, DateTime date) {
        this.first = first;
        this.second = second;
        this.author = author;
        this.date = date;
    }

    public MemberEntity getFirst() {
        return first;
    }

    public MemberEntity getSecond() {
        return second;
    }

    public User getAuthor() {
        return author;
    }

    public DateTime getDate() {
        return date;
    }
}
