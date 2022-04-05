package org.ericghara.parser;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum LineType {

    FILE("F"),
    DIRECTORY("D");

    @Getter
    private final String key;

}
