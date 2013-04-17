/* Copyright 2010-2013 Norconex Inc.
 * 
 * This file is part of Norconex Commons Lang.
 * 
 * Norconex Commons Lang is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Norconex Commons Lang is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Norconex Commons Lang. If not, see <http://www.gnu.org/licenses/>.
 */
package com.norconex.commons.lang.url;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.ProxyHost;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Provides a quick and easy way to stream a URL.
 * @author <a href="mailto:pascal.essiembre@norconex.com">Pascal Essiembre</a>
 */
@SuppressWarnings("nls")
public final class URLStreamer {

    private static final Logger LOG = LogManager.getLogger(URLStreamer.class);
    
    private URLStreamer() {
        super();
    }

    public static InputStream stream(String url) {
        return stream(url, null);
    }
    public static InputStream stream(URL url) {
        return stream(url.toString(), null);
    }
    public static InputStream stream(HttpURL url) {
        return stream(url.toString(), null);
    }

    public static InputStream stream(String url, Credentials creds) {
        return stream(url, creds, null);
    }
    public static InputStream stream(URL url, Credentials creds) {
        return stream(url.toString(), creds, null);
    }
    public static InputStream stream(HttpURL url, Credentials creds) {
        return stream(url.toString(), creds, null);
    }
    
    public static InputStream stream(
            String url, Credentials creds, ProxyHost proxyHost) {
        return stream(url, creds, proxyHost, null);
    }
    public static InputStream stream(
            URL url, Credentials creds, ProxyHost proxyHost) {
        return stream(url.toString(), creds, proxyHost, null);
    }
    public static InputStream stream(
            HttpURL url, Credentials creds, ProxyHost proxyHost) {
        return stream(url.toString(), creds, proxyHost, null);
    }

    public static InputStream stream(
            String url, Credentials creds, ProxyHost proxyHost, 
            Credentials proxyCreds) {
        HttpClient client = new HttpClient();
        if (proxyHost != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Streaming with proxy: "
                        + proxyHost.getHostName()
                        + ":" + proxyHost.getPort());
            }
            HostConfiguration hostConfig = new HostConfiguration();
            hostConfig.setProxyHost(proxyHost);
            client.setHostConfiguration(hostConfig);
            if (proxyCreds != null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Streaming with proxy credentials.");
                }
                client.getState().setProxyCredentials(new AuthScope(
                        proxyHost.getHostName(), proxyHost.getPort()), 
                        proxyCreds);                
            }
        }
        
        if (creds != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Streaming with credentials.");
            }
            client.getState().setCredentials(new AuthScope(
                    AuthScope.ANY_HOST, 80, AuthScope.ANY_REALM), creds);            
        }
        
        GetMethod call = new GetMethod(url);
        try {
            client.executeMethod(call);
            int statusCode = call.getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                LOG.error("Invalid HTTP response: " + statusCode
                        + ". Response body is: " 
                                + call.getResponseBodyAsString());
                throw new IOException("Cannot stream URL: " + url);
            }
            InputStream stream = call.getResponseBodyAsStream();
            return stream;
        } catch (IOException e) {
            throw new URLException("Could not stream URL: " + url, e);
        }
    }    
    public static InputStream stream(
            URL url, Credentials creds, ProxyHost proxyHost, 
            Credentials proxyCreds) {
        return stream(url.toString(), creds, proxyHost, proxyCreds);
    }
    public static InputStream stream(
            HttpURL url, Credentials creds, ProxyHost proxyHost, 
            Credentials proxyCreds) {
        return stream(url.toString(), creds, proxyHost, proxyCreds);
    }

    
    public static String streamToString(
            String url, Credentials creds, ProxyHost proxyHost) {
        StopWatch watch = null;
        if (LOG.isDebugEnabled()) {
            watch = new StopWatch();
            watch.start();
            LOG.debug("Streaming URL:" + url);
        }
        String out;
        try {
            out = IOUtils.toString(stream(url, creds, proxyHost));
        } catch (IOException e) {
            throw new URLException("Could not stream URL to string: " + url, e);
        }
        if (LOG.isDebugEnabled()) {
            watch.stop();
            LOG.debug("Streaming elapsed time: " + watch.toString());
        }
        return out;
    }
    public static String streamToString(
            URL url, Credentials creds, ProxyHost proxyHost) {
        return streamToString(url.toString(), creds, proxyHost);
    }
    public static String streamToString(
            HttpURL url, Credentials creds, ProxyHost proxyHost) {
        return streamToString(url.toString(), creds, proxyHost);
    }

    public static String streamToString(
            String url, Credentials creds, ProxyHost proxyHost, 
            Credentials proxyCreds) {
        StopWatch watch = null;
        if (LOG.isDebugEnabled()) {
            watch = new StopWatch();
            watch.start();
            LOG.debug("Streaming URL:" + url);
        }
        String out;
        try {
            out = IOUtils.toString(stream(url, creds, proxyHost, proxyCreds));
        } catch (IOException e) {
            throw new URLException("Could not stream URL to string: " + url, e);
        }
        if (LOG.isDebugEnabled()) {
            watch.stop();
            LOG.debug("Streaming elapsed time: " + watch.toString());
        }
        return out;
    }
    public static String streamToString(
            URL url, Credentials creds, ProxyHost proxyHost, 
            Credentials proxyCreds) {
        return streamToString(url.toString(), creds, proxyHost, proxyCreds);
    }
    public static String streamToString(
            HttpURL url, Credentials creds, ProxyHost proxyHost, 
            Credentials proxyCreds) {
        return streamToString(url.toString(), creds, proxyHost, proxyCreds);
    }
    
    public static String streamToString(String url, Credentials creds) {
        return streamToString(url, creds, null);
    }
    public static String streamToString(URL url, Credentials creds) {
        return streamToString(url.toString(), creds, null);
    }
    public static String streamToString(HttpURL url, Credentials creds) {
        return streamToString(url.toString(), creds, null);
    }
}