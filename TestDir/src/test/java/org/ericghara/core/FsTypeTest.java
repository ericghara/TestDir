package org.ericghara.core;

import com.google.common.jimfs.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.google.common.jimfs.Configuration.osX;
import static org.ericghara.core.FsType.OSX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class})
class FsTypeTest {

    @Test
    @DisplayName("currentPlatform gets returns enum matching current OS")
    void currentPlatformTest() {
        try (MockedStatic<Configuration> configurationMock =
                     mockStatic(Configuration.class,
                                withSettings().defaultAnswer(CALLS_REAL_METHODS)) ) {
            Configuration osx = osX();
            configurationMock.when(Configuration::forCurrentPlatform).thenReturn( osx );
            assertEquals(OSX, FsType.currentPlatform() );
        }
    }
}