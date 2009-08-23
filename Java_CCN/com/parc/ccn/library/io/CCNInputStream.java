package com.parc.ccn.library.io;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.ccnx.ccn.CCNHandle;
import org.ccnx.ccn.Library;
import org.ccnx.ccn.protocol.ContentName;
import org.ccnx.ccn.protocol.ContentObject;
import org.ccnx.ccn.protocol.PublisherPublicKeyDigest;

import com.parc.ccn.security.crypto.ContentKeys;

/**
 * Perform sequential reads on any block-oriented CCN content, namely that
 * where the name component after the specified name prefix is an optionally
 * segment-encoded integer, and the content blocks are indicated by 
 * monotonically increasing (but not necessarily sequential)
 * optionally segment-encoded integers. For example, a file could be 
 * divided into sequential blocks, while an audio stream might have 
 * blocks named by time offsets into the stream. 
 * 
 * This input stream will read from a sequence of blocks, authenticating
 * each as it goes, and caching what information it can. All it assumes
 * is that the last component of the name is an increasing integer
 * value, where we start with the name we are given, and get right siblings
 * (next blocks) moving forward.
 * 
 * This input stream works with data with and without a header block; it
 * opportunistically queries for a header block and uses its information if
 * one is available. That means an extra interest for content that does not have
 * a header block.
 * 
 * Read size is independent of fragment size; the stream will pull additional
 * content fragments dynamically when possible to fill out the requested number
 * of bytes.
 * 
 * TODO remove header handling from here, add use of lastSegment marker in
 *    blocks leading up to the end. Headers, in whatever form they evolve
 *    into, will be used only by higher-level streams.
 * @author smetters
 *
 */
public class CCNInputStream extends CCNAbstractInputStream {
	
	protected boolean _atEOF = false;
	protected int _readlimit = 0;
	protected int _markOffset = 0;
	protected long _markBlock = 0;

	public CCNInputStream(ContentName name, Long startingSegmentNumber, PublisherPublicKeyDigest publisher, 
			ContentKeys keys, CCNHandle library) throws XMLStreamException,
			IOException {

		super(name, startingSegmentNumber, publisher, keys, library);
	}

	public CCNInputStream(ContentName name, Long startingSegmentNumber, PublisherPublicKeyDigest publisher,
			CCNHandle library) throws XMLStreamException, IOException {

		super(name, startingSegmentNumber, publisher, library);
	}
	
	public CCNInputStream(ContentName name, PublisherPublicKeyDigest publisher, CCNHandle library) 
			throws XMLStreamException, IOException {
		this(name, null, publisher, library);
	}
	
	public CCNInputStream(ContentName name) throws XMLStreamException, IOException {
		this(name, null);
	}
	
	public CCNInputStream(ContentName name, CCNHandle library) throws XMLStreamException, IOException {
		this(name, null, null, library);
	}
	
	public CCNInputStream(ContentName name, long segmentNumber) throws XMLStreamException, IOException {
		this(name, segmentNumber, null, null);
	}
	
	public CCNInputStream(ContentObject firstSegment, CCNHandle library) throws XMLStreamException, IOException {
		super(firstSegment, library);
	}
	
	@Override
	public int available() throws IOException {
		if (null == _segmentReadStream)
			return 0;
		return _segmentReadStream.available();
	}
	
	public boolean eof() { 
		//Library.logger().info("Checking eof: there yet? " + _atEOF);
		return _atEOF; 
	}
		
	@Override
	public void close() throws IOException {
		// don't have to do anything.
	}

	@Override
	public synchronized void mark(int readlimit) {
		_readlimit = readlimit;
		_markBlock = segmentNumber();
		if (null == _segmentReadStream) {
			_markOffset = 0;
		} else {
			try {
				_markOffset = _currentSegment.contentLength() - _segmentReadStream.available();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		Library.logger().finer("mark: block: " + segmentNumber() + " offset: " + _markOffset);
	}

	@Override
	public boolean markSupported() {
		return true;
	}

	protected int readInternal(byte [] buf, int offset, int len) throws IOException {
		
		if (_atEOF) {
			return -1;
		}
		
		Library.logger().finer(baseName() + ": reading " + len + " bytes into buffer of length " + 
				((null != buf) ? buf.length : "null") + " at offset " + offset);
		// is this the first block?
		if (null == _currentSegment) {
			ContentObject firstBlock = getFirstSegment();
			if (null == firstBlock) {
				_atEOF = true;
				return -1; // nothing to read
			}
			setFirstSegment(firstBlock);
		} 
		Library.logger().finer("reading from block: " + _currentSegment.name() + " length: " + 
				_currentSegment.contentLength());
		
		// Now we have a block in place. Read from it. If we run out of block before
		// we've read len bytes, pull next block.
		int lenToRead = len;
		int lenRead = 0;
		long readCount = 0;
		while (lenToRead > 0) {
			if (null == _segmentReadStream) {
				Library.logger().severe("Unexpected null block read stream!");
			}
			if (null != buf) {  // use for skip
				Library.logger().finest("before block read: content length "+_currentSegment.contentLength()+" position "+ tell() +" available: " + _segmentReadStream.available() + " dst length "+buf.length+" dst index "+offset+" len to read "+lenToRead);
				// Read as many bytes as we can
				readCount = _segmentReadStream.read(buf, offset, lenToRead);
			} else {
				readCount = _segmentReadStream.skip(lenToRead);
			}

			if (readCount <= 0) {
				Library.logger().info("Tried to read at end of block, go get next block.");
				setCurrentSegment(getNextSegment());
				if (null == _currentSegment) {
					Library.logger().info("next block was null, setting _atEOF, returning " + ((lenRead > 0) ? lenRead : -1));
					_atEOF = true;
					if (lenRead > 0) {
						return lenRead;
					}
					return -1; // no bytes read, at eof
				}
				Library.logger().info("now reading from block: " + _currentSegment.name() + " length: " + 
						_currentSegment.contentLength());
			} else {
				offset += readCount;
				lenToRead -= readCount;
				lenRead += readCount;
				Library.logger().finest("     read " + readCount + " bytes for " + lenRead + " total, " + lenToRead + " remaining.");
			}
		}
		return lenRead;
	}

	@Override
	public synchronized void reset() throws IOException {
		// TODO: when first block is read in constructor this check can be removed
		if (_currentSegment == null)
			setFirstSegment(getSegment(_markBlock));
		else
			setCurrentSegment(getSegment(_markBlock));
		_segmentReadStream.skip(_markOffset);
		_atEOF = false;
		Library.logger().finer("reset: block: " + segmentNumber() + " offset: " + _markOffset + " eof? " + _atEOF);
	}
	
	@Override
	public long skip(long n) throws IOException {
		
		Library.logger().info("in skip("+n+")");
		
		if (n < 0) {
			return 0;
		}
		
		return readInternal(null,0, (int)n);
	}
	
	protected int segmentCount() {
		return 0;
	}

	public long seek(long position) throws IOException {
		Library.logger().info("Seeking stream to " + position);
		// TODO: when first block is read in constructor this check can be removed
		if (_currentSegment == null)
			setFirstSegment(getFirstSegment());
		else
			setCurrentSegment(getFirstSegment());
		return skip(position);
	}

	public long tell() {
		try {
			return _currentSegment.contentLength() - _segmentReadStream.available();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} // could implement a running count...
	}
	
	public long length() {
		return -1;
	}
	
	public ContentName baseName() { return _baseName; }
}

