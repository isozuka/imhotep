/*
 * Copyright (C) 2018 Indeed Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.indeed.imhotep;

import com.google.common.base.Throwables;
import com.indeed.imhotep.api.GroupStatsIterator;
import com.indeed.util.core.io.Closeables2;
import it.unimi.dsi.fastutil.longs.AbstractLongIterator;
import org.apache.log4j.Logger;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Implementation of GroupStatsIterator over a socket's Inputstream
 *
 * @author aibragimov
 */

public class GroupStatsStreamReader extends AbstractLongIterator implements GroupStatsIterator {

    private static final Logger log = Logger.getLogger(GroupStatsStreamReader.class);

    private DataInputStream stream;
    private final int count;
    // consume stream till the end.
    private final boolean exhaust;
    private int index;

    public GroupStatsStreamReader(final InputStream stream, final int count, final boolean exhaust) {
        this.stream = new DataInputStream(stream);
        this.count = count;
        this.exhaust = exhaust;
        index = 0;
    }

    @Override
    public int getNumGroups() {
        return count;
    }

    @Override
    public boolean hasNext() {
        return stream != null && index < count;
    }

    @Override
    public long nextLong() {
        try {
            index++;
            return stream.readLong();
        } catch ( final IOException e ) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void close() {
        if (exhaust) {
            try {
                // Consume stream till the end because otherwise stream writer can get exception
                // (for example in case of transferring data over socket)
                while (hasNext()) {
                    nextLong();
                }
            } catch (final Exception ignored) {
            }
        }
        Closeables2.closeQuietly(stream, log);
        stream = null;
    }
}
