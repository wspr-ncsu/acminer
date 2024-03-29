/*
 * Copyright (c) 2009, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.sag.common.io.zipfs;

import java.nio.file.DirectoryStream;
import java.nio.file.ClosedDirectoryStreamException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.io.IOException;

/**
 *
 * @author  Xueming Shen, Rajendra Gutupalli, Jaya Hangal
 */

class ZipDirectoryStream implements DirectoryStream<Path> {

    private final ZipFileSystem zipfs;
    private final byte[] path;
    private final DirectoryStream.Filter<? super Path> filter;
    private volatile boolean isClosed;
    private volatile Iterator<Path> itr;

    ZipDirectoryStream(ZipPath zipPath,
                       DirectoryStream.Filter<? super java.nio.file.Path> filter)
        throws IOException
    {
        this.zipfs = zipPath.getFileSystem();
        this.path = zipPath.getResolvedPath();
        this.filter = filter;
        // sanity check
        if (!zipfs.isDirectory(path))
            throw new NotDirectoryException(zipPath.toString());
    }

    @Override
    public synchronized Iterator<Path> iterator() {
        if (isClosed)
            throw new ClosedDirectoryStreamException();
        if (itr != null)
            throw new IllegalStateException("Iterator has already been returned");

        try {
            itr = zipfs.iteratorOf(path, filter);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return new Iterator<Path>() {
            //private Path next;
            @Override
            public boolean hasNext() {
                if (isClosed)
                    return false;
                return itr.hasNext();
            }

            @Override
            public synchronized Path next() {
                if (isClosed)
                    throw new NoSuchElementException();
                return itr.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public synchronized void close() throws IOException {
        isClosed = true;
    }


}
