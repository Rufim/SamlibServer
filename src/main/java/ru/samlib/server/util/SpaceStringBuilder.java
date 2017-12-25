package ru.samlib.server.util;

public class SpaceStringBuilder {

    final StringBuilder stringBuilder;
    boolean first = true;

    public SpaceStringBuilder() {
        this.stringBuilder = new StringBuilder("");
    }

    public SpaceStringBuilder(StringBuilder stringBuilder) {
        this.stringBuilder = stringBuilder;
    }

    public SpaceStringBuilder comma(Object... statement) {
        if (statement != null) {
            boolean first = true;
            for (Object o : statement) {
                if (!first) {
                    stringBuilder.append(",");
                }
                first = false;
                stringBuilder.append(o.toString());
            }
        }
        return this;
    }

    public SpaceStringBuilder sap(Object... statement) {
        if (statement != null) {
            for (Object o : statement) {
                stringBuilder.append(" ");
                stringBuilder.append(o.toString());
            }
        }
        return this;
    }

    public SpaceStringBuilder aps(Object... statement) {
        if (statement != null) {
            for (Object o : statement) {
                stringBuilder.append(o.toString());
                stringBuilder.append(" ");
            }
        }
        return this;
    }

    public SpaceStringBuilder in(Object... statement) {
        if (statement != null) {
            stringBuilder.append(" in (");
            comma(statement);
            stringBuilder.append(") ");
        }
        return this;
    }

    public SpaceStringBuilder ap(Object... statement) {
        if (statement != null) {
            for (Object o : statement) {
                stringBuilder.append(o.toString());
            }
        }
        return this;
    }

    public SpaceStringBuilder dot() {
        stringBuilder.append(".");
        return this;
    }

    public SpaceStringBuilder comma() {
        stringBuilder.append(",");
        return this;
    }

    public SpaceStringBuilder str(Object statement) {
        stringBuilder.append("'");
        stringBuilder.append(statement);
        stringBuilder.append("'");
        return this;
    }


    public SpaceStringBuilder and() {
        if (first) {
            stringBuilder.append(" ");
            first = false;
        } else {
            stringBuilder.append(" AND ");
        }
        return this;
    }

    public SpaceStringBuilder or() {
        stringBuilder.append(" OR ");
        return this;
    }

    public SpaceStringBuilder nl() {
        stringBuilder.append("\n");
        return this;
    }

    @Override
    public String toString() {
        return stringBuilder.toString();
    }

}
