package fcup;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetadataServerTest {

    @Test
    void giveMeAnID() {
        final MetadataServer metaServer = new MetadataServer();
        final String a = metaServer.giveMeAnID();
        final String b = metaServer.giveMeAnID();
        assertNotEquals(a, b);
    }
}
