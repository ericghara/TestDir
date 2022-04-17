package org.ericghara.csv;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum LineType {

    /**
     * File Line type
     */
    FILE("F"),

    /**
     * Directory Line Type
     */
    DIRECTORY("D");

    @Getter
    private final String key;

}
