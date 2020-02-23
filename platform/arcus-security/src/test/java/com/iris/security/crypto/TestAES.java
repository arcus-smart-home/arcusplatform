package com.iris.security.crypto;

import com.iris.Utils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

import java.security.Security;

public class TestAES {
    private AES aes;

    @Rule
    public Timeout globalTimeout = Timeout.seconds(10); // 10 seconds max per method tested

    @Before
    public void setUp() throws Exception {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        aes = new AES("ylntN5dSPuTQoWga7TArBdlMhEibX+bG4h6sit2Jfq4=", "8e2DM7K/G2I=");
    }

    @Test
    public void testAesCbc() {
        String ctext = aes.encryptUnsafe("foo", "foo1");
        String decoded = aes.decryptUnsafe("foo", ctext);

        Assert.assertEquals(decoded, "foo1");
    }

    @Test
    public void testAesCbcUpgrade() {
        String ctext = aes.encryptUnsafe("foo", "foo1");
        String decoded = aes.decrypt("foo", ctext);

        Assert.assertEquals(decoded, "foo1");
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testAesCbcManipulated() {
        // thrown as java.lang.RuntimeException: javax.crypto.BadPaddingException:
        thrown.expect(RuntimeException.class);
        String ctext = aes.encryptUnsafe("foo", "foo1");
        byte[] tamper = Utils.b64Decode(ctext);
        tamper[3] = 'A';
        String tampered = Utils.b64Encode(tamper);
        String decoded = aes.decrypt("foo", tampered);
    }

    @Test
    public void testAesGcmManipulated() {
        // thrown as java.lang.RuntimeException: AEADBadTagException:
        thrown.expect(RuntimeException.class);
        String ctext = aes.encryptSafe("foo", "foo1");
        byte[] tamper = Utils.b64Decode(ctext);
        tamper[3] = 'A';
        String tampered = Utils.b64Encode(tamper);
        String decoded = aes.decryptSafe("foo", tampered);
    }

    @Test
    public void testAesGcm() {
        String ctext = aes.encryptSafe("foo", "a very secret secret");
        String decoded = aes.decryptSafe("foo", ctext);

        Assert.assertEquals(decoded, "a very secret secret");
    }

    @Test
    public void testAesGcmMessage() {
        String ctext = aes.encryptSafe("foo", "a very secret secret");
        String decoded = aes.decryptSafe("foo", ctext);

        Assert.assertNotEquals(decoded, "not the same");
    }
}
