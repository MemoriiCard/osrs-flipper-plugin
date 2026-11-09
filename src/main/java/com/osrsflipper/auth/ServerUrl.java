package com.osrsflipper.auth;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Properties;

/**
 * Checks the server address before any token is sent to it. Only HTTPS is allowed, except to this
 * computer for development, so a mistyped or planted address can't receive tokens in the clear.
 */
public final class ServerUrl
{
	static final String LOCAL = "http://localhost:8000";

	/**
	 * The Server URL setting's default. Tester builds bake the beta server in with
	 * {@code ./gradlew shadowJar -PserverUrl=https://...}, which writes {@code server.properties};
	 * otherwise it's this computer, for development.
	 */
	public static final String DEFAULT = defaultFrom(ServerUrl.class.getResourceAsStream("/com/osrsflipper/server.properties"));

	private ServerUrl()
	{
	}

	static String defaultFrom(InputStream in)
	{
		if (in == null)
		{
			return LOCAL;
		}
		try (InputStream stream = in)
		{
			Properties props = new Properties();
			props.load(stream);
			String url = normalize(props.getProperty("serverUrl"));
			return url == null ? LOCAL : url;
		}
		catch (IOException e)
		{
			return LOCAL;
		}
	}

	/** The address without a trailing slash, or null if it isn't allowed. */
	public static String normalize(String value)
	{
		if (value == null)
		{
			return null;
		}
		try
		{
			URI uri = new URI(value.trim());
			String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
			String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
			if (host.isEmpty() || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null)
			{
				return null;
			}
			boolean local = host.equals("localhost") || host.equals("127.0.0.1");
			if (!scheme.equals("https") && !(scheme.equals("http") && local))
			{
				return null;
			}
			String text = uri.toString();
			while (text.endsWith("/"))
			{
				text = text.substring(0, text.length() - 1);
			}
			return text;
		}
		catch (URISyntaxException e)
		{
			return null;
		}
	}
}
