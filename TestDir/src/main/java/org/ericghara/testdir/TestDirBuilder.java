package org.ericghara.testdir;

import com.google.common.jimfs.Jimfs;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import org.ericghara.csv.ReaderUtils;
import org.ericghara.csv.WriteFromCSV;
import org.ericghara.write.ByteSupplier;
import org.ericghara.write.RandomByteSupplier;

import java.io.Reader;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Iterator;

@ToString
class TestDirBuilder {

    @Getter
    private Reader csvStream;
    @Getter
    private Path targetDir;
    @Getter
    private FsType fsType;
    @Getter
    private ByteSupplier byteSupplier;

    private boolean csvSourceSet = false;
    private boolean targetDirSet = false;
    private boolean fsTypeSet = false;
    private boolean byteSupplierSet = false;



    TestDirBuilder() {
        byteSupplier = initDefaultByteSupplier();
        fsType = FsType.currentPlatform();
    }

    public TestDirBuilder setCsvSource(@NonNull Reader csvStream) throws IllegalStateException, NullPointerException {
        if (csvSourceSet) {
            throw new IllegalStateException("The reader has already been set.");
        }
        this.csvStream = csvStream;
        csvSourceSet = true;
        return this;
    }

    public TestDirBuilder setCsvSource(@NonNull String csvStr) throws IllegalStateException, NullPointerException {
        Reader csvStream = ReaderUtils.getStringReader(csvStr);
        return setCsvSource(csvStream);
    }

    public TestDirBuilder setCsvSource(@NonNull Path csvFilePath) throws IllegalStateException, NullPointerException {
        Reader csvStream = ReaderUtils.getFileReader(csvFilePath);
        return setCsvSource(csvStream);
    }

    public TestDirBuilder setDir(@NonNull Path targetDir) {
        if (targetDirSet) {
            throw new IllegalStateException("The targetDir has already been set.");
        }
        this.targetDir = targetDir;
        targetDirSet = true;
        return this;
    }

    public TestDirBuilder setByteSupplier(@NonNull ByteSupplier byteSupplier) {
        if (byteSupplierSet) {
            throw new IllegalStateException("The targetDir has already been set.");
        }
        this.byteSupplier = byteSupplier;
        byteSupplierSet = true;
        return this;
    }

    public TestDirBuilder setFsType(@NonNull FsType os) {
        if (fsTypeSet) {
            throw new IllegalStateException("The targetDir has already been set.");
        }
        this.fsType = os;
        fsTypeSet = true;
        return this;
    }


    /**
     *
     * @return {@link TestDir} based on the provided configuration
     * @throws IllegalStateException if both fsType and dir fields have been set
     */
    public TestDir build() throws IllegalStateException {
        if (fsTypeSet && targetDirSet) {
            throw new IllegalStateException("Cannot specify a path and an fsType.  " +
                    "If a path is provided the Filesystem of the provided path will be used");
        }
        if (!targetDirSet) {
            targetDir = createJimFS();
        }
        var testDir = new TestDir(targetDir, byteSupplier);
        if (csvSourceSet) {
            var writer = new WriteFromCSV();
            writer.write(testDir, csvStream);
        }
        return testDir;
    }

    private static ByteSupplier initDefaultByteSupplier() {
        var rand = new RandomByteSupplier();
        return  rand::getAsByte;
    }

    private Path createJimFS() {
        FileSystem fs = Jimfs.newFileSystem(fsType.configuration() );
        Iterator<Path> roots = fs.getRootDirectories().iterator();
        if (!roots.hasNext() ) {
            throw new IllegalStateException("An internal error occurred.  Could not determine the JimFS root directory."); // should never happen
        }
        return roots.next();
    }
}
