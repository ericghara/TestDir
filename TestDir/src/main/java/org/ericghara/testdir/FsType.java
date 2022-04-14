package org.ericghara.testdir;

import com.google.common.jimfs.Configuration;

import java.util.stream.Stream;

import static com.google.common.jimfs.Configuration.*;

public enum FsType {

    OSX(osX() ),
    UNIX(unix() ),
    WIN(windows() );

    final Configuration os;

    FsType(Configuration os) {
        this.os = os;
    }

    public Configuration configuration() {
        return os;
    }

    public static FsType currentPlatform() {
        final Configuration cur = Configuration.forCurrentPlatform();
        return Stream.of(FsType.values())
                     .filter(v -> v.configuration() == cur)
                     .findFirst().orElseThrow(
                             () -> new IllegalStateException("Could not match the current platform to an OS type"));
    }

}
