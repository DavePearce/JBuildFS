// Copyright 2021 David James Pearce
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package jbuildstore.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import jbuildstore.core.Content;

/**
 * A simple form of content representing a text file.
 *
 * @author David J. Pearce
 *
 */
public class TextFile implements Content {
	/**
	 * Standard content type for UTF8 text files.
	 */
	public static final Content.Type<TextFile> ContentTypeUtf8 = ContentType(StandardCharsets.UTF_8);
	/**
	 * Standard content type for ASCII text files.
	 */
	public static final Content.Type<TextFile> ContentTypeASCII = ContentType(StandardCharsets.US_ASCII);
	/**
	 * Content type representing a text file for a given encoding (e.g.
	 * <code>StandardCharsets.US_ASCII).
	 */
	public static final Content.Type<TextFile> ContentType(Charset encoding) {
		return new Content.Type<>() {

			@Override
			public TextFile read(InputStream input) throws IOException {
				// Read all bytes from input stream
				byte[] bytes = input.readAllBytes();
				// Convert them into a string
				return new TextFile(new String(bytes, encoding));
			}

			@Override
			public void write(OutputStream output, TextFile value) throws IOException {
				// Extract bytes from text file
				byte[] bytes = value.getBytes(encoding);
				// Write them to output stream
				output.write(bytes);
			}

			@Override
			public String suffix() {
				return "txt";
			}

		};
	}

    private final String content;

    public TextFile(String content) {
        this.content = content;
    }

    public byte[] getBytes(Charset encoding) {
    	return content.getBytes(encoding);
    }

	@Override
	public Type<?> getContentType() {
		// TODO Auto-generated method stub
		return null;
	}

    public Line getEnclosingLine(int offset) {
        int line = 1;
        int start = 0;

        for(int i=0;i!=content.length();++i) {
            if(i == offset) {
                // advance to end of line
                while(i < content.length() && content.charAt(i) != '\n') {
                    i++;
                }
                // done
                return new Line(start, i - start, line);
            } else if(content.charAt(i) == '\n') {
                start = i+1;
                line = line + 1;
            }
        }
        return null;
    }

    public class Line {
        private final int offset;
        private final int length;
        private final int number;

        public Line(int offset, int length, int number) {
            this.offset = offset;
            this.length = length;
            this.number = number;
        }

        public int getOffset() {
            return offset;
        }

        public int getLength() {
            return length;
        }

        public int getNumber() {
            return number;
        }

        public String getText() {
            return content.substring(offset,offset+length);
        }
    }
}
