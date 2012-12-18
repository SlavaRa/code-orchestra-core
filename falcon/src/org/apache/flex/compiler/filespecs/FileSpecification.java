/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.flex.compiler.filespecs;

import static org.apache.flex.compiler.filespecs.CombinedFile.getBOM;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.flex.compiler.filespecs.CombinedFile.BOM;

/**
 * A file specification that reads the content of the file from the file itself.
 */
public class FileSpecification extends BaseFileSpecification implements IBinaryFileSpecification {
	/**
	 * Constructor.
	 * 
	 * @param path
	 *            The path of the file.
	 */
	public FileSpecification(String path) {
		super(path);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof BaseFileSpecification))
			return false;
		BaseFileSpecification other = (BaseFileSpecification) obj;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		return true;
	}

	@Override
    public Reader createReader() throws FileNotFoundException {
		File fileHandle = getFileHandle();
		if (!fileHandle.exists())
			throw new FileNotFoundException(fileHandle.getAbsolutePath());
		try {
			return createNIOReader();
		}
		catch (FileNotFoundException e)
		{
		    throw e;
		}
        catch (final IOException e)
        {
            // could not open the file, but the file seems to exist.
            // we can either return null, or maybe a reader that pretends
            // the file is empty or throws an IOException as soon as you try to read
            // from it.

            // We'll return a Reader that just throws the above IOException when
            // someone tries to read some characters.
            return new Reader()
            {
                @Override
                public int read(char[] cbuf, int off, int len) throws IOException
                {
                    throw e;
                }

                @Override
                public void close() throws IOException
                {
                }
            };
        }
	}

	private Reader createNIOReader() throws IOException {
	    final File file = getFileHandle();
	    
	    // Skip BOM header.
        final BufferedInputStream strm = new BufferedInputStream(new FileInputStream(file));
	    final BOM bom = getBOM(strm);
        strm.skip(bom.pattern.length);

        final Reader reader = new BufferedReader(
                new InputStreamReader(strm, bom.charset));
        return reader;
	}

    @Override
    public InputStream createInputStream() throws FileNotFoundException
    {
        return new BufferedInputStream(new FileInputStream(getFileHandle()));
    }
}
