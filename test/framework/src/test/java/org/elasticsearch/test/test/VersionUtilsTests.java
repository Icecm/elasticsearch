/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.test.test;

import org.elasticsearch.Version;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.test.VersionUtils;

import java.util.List;

public class VersionUtilsTests extends ESTestCase {

    public void testAllVersionsSorted() {
        List<Version> allVersions = VersionUtils.allReleasedVersions();
        for (int i = 0, j = 1; j < allVersions.size(); ++i, ++j) {
            assertTrue(allVersions.get(i).before(allVersions.get(j)));
        }
    }

    public void testRandomVersionBetween() {
        // full range
        Version got = VersionUtils.randomVersionBetween(random(), VersionUtils.getFirstVersion(), Version.CURRENT);
        assertTrue(got.onOrAfter(VersionUtils.getFirstVersion()));
        assertTrue(got.onOrBefore(Version.CURRENT));
        got = VersionUtils.randomVersionBetween(random(), null, Version.CURRENT);
        assertTrue(got.onOrAfter(VersionUtils.getFirstVersion()));
        assertTrue(got.onOrBefore(Version.CURRENT));
        got = VersionUtils.randomVersionBetween(random(), VersionUtils.getFirstVersion(), null);
        assertTrue(got.onOrAfter(VersionUtils.getFirstVersion()));
        assertTrue(got.onOrBefore(Version.CURRENT));

        // sub range
        got = VersionUtils.randomVersionBetween(random(), Version.V_5_0_0,
                Version.V_6_0_0_alpha2_UNRELEASED);
        assertTrue(got.onOrAfter(Version.V_5_0_0));
        assertTrue(got.onOrBefore(Version.V_6_0_0_alpha2_UNRELEASED));

        // unbounded lower
        got = VersionUtils.randomVersionBetween(random(), null, Version.V_6_0_0_alpha2_UNRELEASED);
        assertTrue(got.onOrAfter(VersionUtils.getFirstVersion()));
        assertTrue(got.onOrBefore(Version.V_6_0_0_alpha2_UNRELEASED));
        got = VersionUtils.randomVersionBetween(random(), null, VersionUtils.allReleasedVersions().get(0));
        assertTrue(got.onOrAfter(VersionUtils.getFirstVersion()));
        assertTrue(got.onOrBefore(VersionUtils.allReleasedVersions().get(0)));

        // unbounded upper
        got = VersionUtils.randomVersionBetween(random(), Version.V_5_0_0, null);
        assertTrue(got.onOrAfter(Version.V_5_0_0));
        assertTrue(got.onOrBefore(Version.CURRENT));
        got = VersionUtils.randomVersionBetween(random(), VersionUtils.getPreviousVersion(), null);
        assertTrue(got.onOrAfter(VersionUtils.getPreviousVersion()));
        assertTrue(got.onOrBefore(Version.CURRENT));

        // range of one
        got = VersionUtils.randomVersionBetween(random(), VersionUtils.getFirstVersion(), VersionUtils.getFirstVersion());
        assertEquals(got, VersionUtils.getFirstVersion());
        got = VersionUtils.randomVersionBetween(random(), Version.CURRENT, Version.CURRENT);
        assertEquals(got, Version.CURRENT);
        got = VersionUtils.randomVersionBetween(random(), Version.V_6_0_0_alpha2_UNRELEASED,
                Version.V_6_0_0_alpha2_UNRELEASED);
        assertEquals(got, Version.V_6_0_0_alpha2_UNRELEASED);

        // implicit range of one
        got = VersionUtils.randomVersionBetween(random(), null, VersionUtils.getFirstVersion());
        assertEquals(got, VersionUtils.getFirstVersion());
        got = VersionUtils.randomVersionBetween(random(), Version.CURRENT, null);
        assertEquals(got, Version.CURRENT);
    }
}
