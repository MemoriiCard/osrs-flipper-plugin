package com.osrsflipper.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.gson.Gson;
import java.io.File;
import java.nio.file.Files;
import org.junit.Test;

public class ServerUrlTest
{
	@Test
	public void onlyHttpsOrThisComputer()
	{
		assertEquals("https://api.example.com", ServerUrl.normalize(" https://api.example.com/ "));
		assertEquals("http://localhost:8000", ServerUrl.normalize("http://localhost:8000"));
		assertNull(ServerUrl.normalize("http://api.example.com"));
		assertNull(ServerUrl.normalize("https://user:pass@api.example.com"));
		assertNull(ServerUrl.normalize("https://api.example.com/?next=x"));
		assertNull(ServerUrl.normalize("file:///etc/passwd"));
		assertNull(ServerUrl.normalize("not a url"));
		assertNull(ServerUrl.normalize(null));
	}

	@Test
	public void tokenFileRoundTrips() throws Exception
	{
		File dir = Files.createTempDirectory("flipper-auth").toFile();
		TokenFile file = new TokenFile(new Gson(), dir);
		assertNull(file.load());
		file.save("refresh-123", "https://api.example.com");
		assertEquals("refresh-123", file.load().refreshToken);
		assertEquals("https://api.example.com", file.load().server);
		file.clear();
		assertNull(file.load());
	}
}
