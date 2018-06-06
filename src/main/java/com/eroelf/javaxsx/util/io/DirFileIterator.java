package com.eroelf.javaxsx.util.io;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.eroelf.javaxsx.util.FileSysUtil;

/**
 * This class converts an input directory to an iterator whose next() method returns the processed result of each line in the files under the directory.
 * Override {@link #lineProcessor(String)} to process each line or just invoke the {@link #forEachRemaining(java.util.function.Consumer) forEachRemaining} method.
 * 
 * @author weikun.zhong
 *
 * @param <E> the return type of the {@link #lineProcessor(String)}.
 * 
 * @see {@link FileIterator}
 * @see {@link FileSysUtil}
 */
public class DirFileIterator<E> extends FileIterator<E>
{
	protected List<File> files;

	public DirFileIterator(String path)
	{
		this(path, 0);
	}

	public DirFileIterator(InputHelper inputHelper, String path)
	{
		this(inputHelper, path, 0);
	}

	public DirFileIterator(String path, int level)
	{
		this(path, new String[0], new String[0], level);
	}

	public DirFileIterator(InputHelper inputHelper, String path, int level)
	{
		this(inputHelper, path, new String[0], new String[0], level);
	}

	public DirFileIterator(String path, String[] acceptRegex, String[] refuseRegex)
	{
		this(path, acceptRegex, refuseRegex, 0);
	}

	public DirFileIterator(InputHelper inputHelper, String path, String[] acceptRegex, String[] refuseRegex)
	{
		this(inputHelper, path, acceptRegex, refuseRegex, 0);
	}

	public DirFileIterator(String path, String[] acceptRegex, String[] refuseRegex, int level)
	{
		this(InputHelper.get(), path, acceptRegex, refuseRegex, level);
	}

	public DirFileIterator(InputHelper inputHelper, String path, String[] acceptRegex, String[] refuseRegex, int level)
	{
		super(false, inputHelper);
		files=FileSysUtil.getFiles(path, acceptRegex, refuseRegex, true, false, level);
		Collections.reverse(files);
	}

	public DirFileIterator(String path, Pattern[] acceptPattern, Pattern[] refusePattern, int level)
	{
		this(InputHelper.get(), path, acceptPattern, refusePattern, level);
	}

	public DirFileIterator(InputHelper inputHelper, String path, Pattern[] acceptPattern, Pattern[] refusePattern, int level)
	{
		super(false, inputHelper);
		files=new ArrayList<>();
		files=FileSysUtil.getFiles(path, files, acceptPattern, refusePattern, true, false, level);
		Collections.reverse(files);
	}

	@Override
	public boolean hasNext()
	{
		if(this.br!=null && super.hasNext())
			return true;
		else
		{
			int len=files.size();
			if(len==0)
				return false;
			else
			{
				this.load(files.remove(len-1));
				return super.hasNext();
			}
		}
	}

	public Stream<E> lines()
	{
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(this, Spliterator.ORDERED|Spliterator.NONNULL), false);
	}
}
