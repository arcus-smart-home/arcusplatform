/*
 * Copyright 2019 Arcus Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.iris.security;

import java.io.ObjectStreamClass;
import java.util.Base64;
import java.util.Date;

import org.apache.shiro.lang.io.DefaultSerializer;
import org.apache.shiro.lang.io.Serializer;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.support.DefaultSubjectContext;
import org.junit.Assert;
import org.junit.Test;

/**
 * {@code GuicedCassandraSessionDAO} writes new sessions as per-attribute JSON,
 * but rows written before that change still hold a whole Java-serialized
 * {@link SimpleSession} produced by Shiro's {@code DefaultSerializer}, and the
 * DAO still reads them ({@code serialized_value}). Those rows outlive a deploy,
 * so a Shiro upgrade that changed the wire form of {@link SimpleSession} or
 * {@link SimplePrincipalCollection} would log everyone out -- or worse, fail
 * mid-read.
 *
 * <p>
 * The blob below was produced by the Apache Shiro 1.13.0 build, immediately
 * before the 2.x migration, and is asserted as a literal. Both classes
 * implement their own {@code writeObject}/{@code readObject}, so this pins the
 * actual stream format, not just the field list.
 * </p>
 */
public class LegacySessionDeserializationTest {

   /**
    * A {@code SimpleSession} serialized by Shiro 1.13.0's
    * {@code org.apache.shiro.io.DefaultSerializer}, carrying both session
    * attributes the DAO knows how to read.
    */
   private static final String SHIRO_113_SESSION_BASE64 =
         "rO0ABXNyACpvcmcuYXBhY2hlLnNoaXJvLnNlc3Npb24ubWd0LlNpbXBsZVNlc3Npb26dHKG41YxibgMAAHhwdwIA23QAJGRlYWRi"
       + "ZWVmLTAwMDAtMTExMS0yMjIyLTMzMzM0NDQ0NTU1NXNyAA5qYXZhLnV0aWwuRGF0ZWhqgQFLWXQZAwAAeHB3CAAAAYvP5WgAeHNx"
       + "AH4AA3cIAAABi8/la+h4dxEAAAAAABt3QAAHMS4yLjMuNHNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZh"
       + "Y3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAACdABQb3JnLmFwYWNoZS5zaGlyby5zdWJqZWN0LnN1cHBvcnQuRGVm"
       + "YXVsdFN1YmplY3RDb250ZXh0X0FVVEhFTlRJQ0FURURfU0VTU0lPTl9LRVlzcgARamF2YS5sYW5nLkJvb2xlYW7NIHKA1Zz67gIA"
       + "AVoABXZhbHVleHABdABNb3JnLmFwYWNoZS5zaGlyby5zdWJqZWN0LnN1cHBvcnQuRGVmYXVsdFN1YmplY3RDb250ZXh0X1BSSU5D"
       + "SVBBTFNfU0VTU0lPTl9LRVlzcgAyb3JnLmFwYWNoZS5zaGlyby5zdWJqZWN0LlNpbXBsZVByaW5jaXBhbENvbGxlY3Rpb26of1gl"
       + "xqMISgMAAUwAD3JlYWxtUHJpbmNpcGFsc3QAD0xqYXZhL3V0aWwvTWFwO3hwc3IAF2phdmEudXRpbC5MaW5rZWRIYXNoTWFwNMBO"
       + "XBBswPsCAAFaAAthY2Nlc3NPcmRlcnhxAH4ABj9AAAAAAAAMdwgAAAAQAAAAAXQACUlyaXNSZWFsbXNyABdqYXZhLnV0aWwuTGlu"
       + "a2VkSGFzaFNldNhs11qV3SoeAgAAeHIAEWphdmEudXRpbC5IYXNoU2V0ukSFlZa4tzQDAAB4cHcMAAAAED9AAAAAAAACdAANdXNl"
       + "ci11dWlkLTEyM3QAEHNlY29uZC1wcmluY2lwYWx4eAB3AQFxAH4AEHh4eA==";

   /** Observed on Shiro 1.13.0. */
   private static final long SIMPLE_SESSION_SVUID = -7125642695178165650L;

   /** Observed on Shiro 1.13.0. */
   private static final long SIMPLE_PRINCIPAL_COLLECTION_SVUID = -6305224034025797558L;

   @Test
   public void testSerialVersionUidsAreUnchanged() {
      Assert.assertEquals("SimpleSession serialVersionUID changed; sessions persisted by the"
            + " previous release will no longer deserialize",
            SIMPLE_SESSION_SVUID,
            ObjectStreamClass.lookup(SimpleSession.class).getSerialVersionUID());
      Assert.assertEquals("SimplePrincipalCollection serialVersionUID changed; principals persisted"
            + " by the previous release will no longer deserialize",
            SIMPLE_PRINCIPAL_COLLECTION_SVUID,
            ObjectStreamClass.lookup(SimplePrincipalCollection.class).getSerialVersionUID());
   }

   @Test
   public void testDeserializeSessionWrittenByShiro113() {
      Serializer<SimpleSession> serializer = new DefaultSerializer<SimpleSession>();
      SimpleSession session = serializer.deserialize(Base64.getDecoder().decode(SHIRO_113_SESSION_BASE64));

      Assert.assertNotNull(session);
      Assert.assertEquals("deadbeef-0000-1111-2222-333344445555", session.getId());
      Assert.assertEquals("1.2.3.4", session.getHost());
      Assert.assertEquals(new Date(1700000000000L), session.getStartTimestamp());
      Assert.assertEquals(new Date(1700000001000L), session.getLastAccessTime());
      Assert.assertEquals(1800000L, session.getTimeout());
      Assert.assertFalse(session.isExpired());

      Assert.assertEquals(Boolean.TRUE, session.getAttribute(DefaultSubjectContext.AUTHENTICATED_SESSION_KEY));

      Object principals = session.getAttribute(DefaultSubjectContext.PRINCIPALS_SESSION_KEY);
      Assert.assertTrue("expected a SimplePrincipalCollection, got "
            + (principals == null ? "null" : principals.getClass().getName()),
            principals instanceof SimplePrincipalCollection);

      SimplePrincipalCollection collection = (SimplePrincipalCollection) principals;
      Assert.assertEquals("user-uuid-123", collection.getPrimaryPrincipal());
      Assert.assertEquals(java.util.Arrays.asList("user-uuid-123", "second-principal"), collection.asList());
      Assert.assertEquals(java.util.Collections.singleton("IrisRealm"), collection.getRealmNames());
   }

   /** A session written now must still read back, i.e. the round trip is intact. */
   @Test
   public void testRoundTrip() {
      SimpleSession session = new SimpleSession("5.6.7.8");
      session.setId("round-trip");
      session.setTimeout(1234L);
      SimplePrincipalCollection collection = new SimplePrincipalCollection("principal", "IrisRealm");
      session.setAttribute(DefaultSubjectContext.PRINCIPALS_SESSION_KEY, collection);
      session.setAttribute(DefaultSubjectContext.AUTHENTICATED_SESSION_KEY, Boolean.TRUE);

      Serializer<SimpleSession> serializer = new DefaultSerializer<SimpleSession>();
      SimpleSession read = serializer.deserialize(serializer.serialize(session));

      Assert.assertEquals("round-trip", read.getId());
      Assert.assertEquals("5.6.7.8", read.getHost());
      Assert.assertEquals(1234L, read.getTimeout());
      Assert.assertEquals(collection, read.getAttribute(DefaultSubjectContext.PRINCIPALS_SESSION_KEY));
   }
}
