package com.dremio.support.diagnostics.stress;

public enum Protocol {

    HTTP,
    JDBC;

    @Override
    public String toString() {
        return switch (this.ordinal()) {
            case 0 -> "HTTP";
            case 1 -> "JDBC";
            default -> null;
        };
    }
}
