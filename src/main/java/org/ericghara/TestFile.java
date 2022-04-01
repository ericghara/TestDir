package org.ericghara;

import java.math.BigDecimal;
import java.nio.file.Path;

public interface TestFile {

    /**
     *
     * @return File size in Bytes
     */
    long getSize();

    /**
     *
     * @param units Size units to return
     * @return size of the testFile in {@code units}
     * @see org.ericghara.SizeUnit
     */
    BigDecimal getSize(SizeUnit units);

    Path getPath();

}
