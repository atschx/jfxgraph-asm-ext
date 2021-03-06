package com.jfxgraph.scanner.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

import com.jfxgraph.scanner.util.ResourceUtils;

/**
 * Abstract base class for resources which resolve URLs into File references, such as {@link UrlResource} or
 * {@link ClassPathResource}.
 * 
 * @author Albert
 * @version $Id: AbstractFileResolvingResource.java,v0.5 2013年10月27日 下午5:18:21 Albert Exp .
 * @since 1.0
 */
public abstract class AbstractFileResolvingResource extends AbstractResource
{

    /**
     * This implementation returns a File reference for the underlying class path resource, provided that it refers to a
     * file in the file system.
     * 
     * @see org.springframework.util.ResourceUtils#getFile(java.net.URL, String)
     */
    @Override
    public File getFile() throws IOException
    {
        URL url = getURL();
        return ResourceUtils.getFile(url, getDescription());
    }

    /**
     * This implementation determines the underlying File (or jar file, in case of a resource in a jar/zip).
     */
    @Override
    protected File getFileForLastModifiedCheck() throws IOException
    {
        URL url = getURL();
        if (ResourceUtils.isJarURL(url))
        {
            URL actualUrl = ResourceUtils.extractJarFileURL(url);
            return ResourceUtils.getFile(actualUrl, "Jar URL");
        } else
        {
            return getFile();
        }
    }

    /**
     * This implementation returns a File reference for the underlying class path resource, provided that it refers to a
     * file in the file system.
     * 
     * @see org.springframework.util.ResourceUtils#getFile(java.net.URI, String)
     */
    protected File getFile(URI uri) throws IOException
    {
        return ResourceUtils.getFile(uri, getDescription());
    }

    @Override
    public boolean exists()
    {
        try
        {
            URL url = getURL();
            if (ResourceUtils.isFileURL(url))
            {
                // Proceed with file system resolution...
                return getFile().exists();
            } else
            {
                // Try a URL connection content-length header...
                URLConnection con = url.openConnection();
                ResourceUtils.useCachesIfNecessary(con);
                HttpURLConnection httpCon = (con instanceof HttpURLConnection ? (HttpURLConnection) con : null);
                if (httpCon != null)
                {
                    httpCon.setRequestMethod("HEAD");
                    int code = httpCon.getResponseCode();
                    if (code == HttpURLConnection.HTTP_OK)
                    {
                        return true;
                    } else if (code == HttpURLConnection.HTTP_NOT_FOUND)
                    {
                        return false;
                    }
                }
                if (con.getContentLength() >= 0)
                {
                    return true;
                }
                if (httpCon != null)
                {
                    // no HTTP OK status, and no content-length header: give up
                    httpCon.disconnect();
                    return false;
                } else
                {
                    // Fall back to stream existence: can we open the stream?
                    InputStream is = getInputStream();
                    is.close();
                    return true;
                }
            }
        } catch (IOException ex)
        {
            return false;
        }
    }

    @Override
    public boolean isReadable()
    {
        try
        {
            URL url = getURL();
            if (ResourceUtils.isFileURL(url))
            {
                // Proceed with file system resolution...
                File file = getFile();
                return (file.canRead() && !file.isDirectory());
            } else
            {
                return true;
            }
        } catch (IOException ex)
        {
            return false;
        }
    }

    @Override
    public long contentLength() throws IOException
    {
        URL url = getURL();
        if (ResourceUtils.isFileURL(url))
        {
            // Proceed with file system resolution...
            return getFile().length();
        } else
        {
            // Try a URL connection content-length header...
            URLConnection con = url.openConnection();
            ResourceUtils.useCachesIfNecessary(con);
            if (con instanceof HttpURLConnection)
            {
                ((HttpURLConnection) con).setRequestMethod("HEAD");
            }
            return con.getContentLength();
        }
    }

    @Override
    public long lastModified() throws IOException
    {
        URL url = getURL();
        if (ResourceUtils.isFileURL(url) || ResourceUtils.isJarURL(url))
        {
            // Proceed with file system resolution...
            return super.lastModified();
        } else
        {
            // Try a URL connection last-modified header...
            URLConnection con = url.openConnection();
            ResourceUtils.useCachesIfNecessary(con);
            if (con instanceof HttpURLConnection)
            {
                ((HttpURLConnection) con).setRequestMethod("HEAD");
            }
            return con.getLastModified();
        }
    }
}
