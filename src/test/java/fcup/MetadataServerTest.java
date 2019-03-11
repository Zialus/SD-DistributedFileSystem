package fcup;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetadataServerTest {

    @Test
    void giveMeAnID() {
        MetadataServer metaServer = new MetadataServer();
        String a = metaServer.giveMeAnID();
        String b = metaServer.giveMeAnID();
        assertNotEquals(a, b);
    }
}
