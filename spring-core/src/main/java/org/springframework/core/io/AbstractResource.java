/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.NestedIOException;
import org.springframework.lang.Nullable;
import org.springframework.util.ResourceUtils;

/**
 * Convenience base class for {@link Resource} implementations,
 * pre-implementing typical behavior.
 *
 * <p>The "exists" method will check whether a File or InputStream can
 * be opened; "isOpen" will always return false; "getURL" and "getFile"
 * throw an exception; and "toString" will return the description.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 28.12.2003
 */
public abstract class AbstractResource implements Resource {

	/**
	 * This implementation checks whether a File can be opened,
	 * falling back to whether an InputStream can be opened.
	 * This will cover both directories and content resources.
	 * 这个实现检查文件是否可以打开，返回到是否可以打开InputStream。这支持目录和内容资源。
	 */
	@Override
	public boolean exists() {
		// Try file existence: can we find the file in the file system?
		// 判断文件存在：我们可以在文件系统中找到文件吗？
		if (isFile()) {
			try {
				// 基于 File 进行判断
				return getFile().exists();
			} catch (IOException ex) {
				Log logger = LogFactory.getLog(getClass());
				if (logger.isDebugEnabled()) {
					logger.debug("Could not retrieve File for existence check of " + getDescription(), ex);
				}
			}
		}
		// Fall back to stream existence: can we open the stream?
		// 基于 InputStream 进行判断
		try {
			getInputStream().close();
			return true;
		} catch (Throwable ex) {
			Log logger = LogFactory.getLog(getClass());
			if (logger.isDebugEnabled()) {
				logger.debug("Could not retrieve InputStream for existence check of " + getDescription(), ex);
			}
			return false;
		}
	}

	/**
	 * This implementation always returns {@code true} for a resource
	 * that {@link #exists() exists} (revised as of 5.1).
	 * 是否可读
	 */
	@Override
	public boolean isReadable() {
		return exists();
	}

	/**
	 * This implementation always returns {@code false}.
	 * 默认直接返回 false，表示未被打开
	 */
	@Override
	public boolean isOpen() {
		return false;
	}

	/**
	 * This implementation always returns {@code false}.
	 * 默认直接返回false，表示不为 File
	 */
	@Override
	public boolean isFile() {
		return false;
	}

	/**
	 * This implementation throws a FileNotFoundException, assuming
	 * that the resource cannot be resolved to a URL.
	 * 这个实现抛出一个FileNotFoundException，假设资源不能解析为URL。抛出 FileNotFoundException异常异常，交给子类实现
	 */
	@Override
	public URL getURL() throws IOException {
		throw new FileNotFoundException(getDescription() + " cannot be resolved to URL");
	}

	/**
	 * This implementation builds a URI based on the URL returned
	 * by {@link #getURL()}.
	 * 此实现基于getURL()返回的URL构建URI。
	 */
	@Override
	public URI getURI() throws IOException {
		URL url = getURL();
		try {
			return ResourceUtils.toURI(url);
		}
		catch (URISyntaxException ex) {
			throw new NestedIOException("Invalid URI [" + url + "]", ex);
		}
	}

	/**
	 * This implementation throws a FileNotFoundException, assuming
	 * that the resource cannot be resolved to an absolute file path.
	 * 这个实现抛出一个FileNotFoundException，假设资源不能解析为绝对文件路径。
	 */
	@Override
	public File getFile() throws IOException {
		throw new FileNotFoundException(getDescription() + " cannot be resolved to absolute file path");
	}

	/**
	 * This implementation returns {@link Channels#newChannel(InputStream)}
	 * with the result of {@link #getInputStream()}.
	 * <p>This is the same as in {@link Resource}'s corresponding default method
	 * but mirrored here for efficient JVM-level dispatching in a class hierarchy.
	 * 此实现返回Channels.newChannel（InputStream）和getInputStream()的结果。 这与Resource对应的默认方法中的方法相同，但在这里进行了镜像，以便在类层次结构中进行有效的JVM级调度。
	 */
	@Override
	public ReadableByteChannel readableChannel() throws IOException {
		return Channels.newChannel(getInputStream());
	}

	/**
	 * This method reads the entire InputStream to determine the content length.
	 * <p>For a custom sub-class of {@code InputStreamResource}, we strongly
	 * recommend overriding this method with a more optimal implementation, e.g.
	 * checking File length, or possibly simply returning -1 if the stream can
	 * only be read once.
	 * 此方法读取整个InputStream以确定内容长度。对于{@code InputStreamResource}的自定义子类，我们强烈建议使用更优化的实现覆盖此方法，例如。检查文件长度，或者如果流可以的话只返回-1只能读一次。
	 * @see #getInputStream()
	 */
	@Override
	public long contentLength() throws IOException {
		InputStream is = getInputStream();
		try {
			long size = 0;
			// 每次最多读取 255 字节
			byte[] buf = new byte[256];
			int read;
			while ((read = is.read(buf)) != -1) {
				size += read;
			}
			return size;
		}
		finally {
			try {
				is.close();
			}
			catch (IOException ex) {
				Log logger = LogFactory.getLog(getClass());
				if (logger.isDebugEnabled()) {
					logger.debug("Could not close content-length InputStream for " + getDescription(), ex);
				}
			}
		}
	}

	/**
	 * This implementation checks the timestamp of the underlying File,
	 * if available.
	 * 此实现检查基础文件的时间戳（如果可用），返回最后修改时间
	 *
	 * @see #getFileForLastModifiedCheck()
	 */
	@Override
	public long lastModified() throws IOException {
		File fileToCheck = getFileForLastModifiedCheck();
		long lastModified = fileToCheck.lastModified();
		if (lastModified == 0L && !fileToCheck.exists()) {
			throw new FileNotFoundException(getDescription() +
					" cannot be resolved in the file system for checking its last-modified timestamp");
		}
		return lastModified;
	}

	/**
	 * Determine the File to use for timestamp checking.
	 * <p>The default implementation delegates to {@link #getFile()}.
	 * 确定用于时间戳检查的文件。默认实现委托给getFile()
	 *
	 * @return the File to use for timestamp checking (never {@code null}) 用于时间戳检查的文件（从不为空）
	 * @throws FileNotFoundException if the resource cannot be resolved as
	 * an absolute file path, i.e. is not available in a file system    如果资源无法解析为绝对文件路径，即在文件系统中不可用
	 * @throws IOException in case of general resolution/reading failures       在一般解析/读取失败的情况
	 */
	protected File getFileForLastModifiedCheck() throws IOException {
		return getFile();
	}

	/**
	 * This implementation throws a FileNotFoundException, assuming
	 * that relative resources cannot be created for this resource.
	 * 此实现抛出FileNotFoundException假设无法为此资源创建相对资源。抛出 FileNotFoundException异常异常，交给子类实现
	 */
	@Override
	public Resource createRelative(String relativePath) throws IOException {
		throw new FileNotFoundException("Cannot create a relative resource for " + getDescription());
	}

	/**
	 * This implementation always returns {@code null},
	 * assuming that this resource type does not have a filename.
	 * 假设此资源类型没有文件名，则此实现始终返回null
	 */
	@Override
	@Nullable
	public String getFilename() {
		return null;
	}


	/**
	 * This implementation compares description strings.
	 * 此实现比较描述字符串。
	 *
	 * @see #getDescription()
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof Resource &&
				((Resource) other).getDescription().equals(getDescription())));
	}

	/**
	 * This implementation returns the description's hash code.
	 * 此实现返回描述的哈希代码
	 *
	 * @see #getDescription()
	 */
	@Override
	public int hashCode() {
		return getDescription().hashCode();
	}

	/**
	 * This implementation returns the description of this resource.
	 * 此实现返回此资源的描述
	 *
	 * @see #getDescription()
	 */
	@Override
	public String toString() {
		return getDescription();
	}

}
