package com.gpgenius.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/** Which announcements the panel shows: the server's list minus what the player closed this session. */
public final class Announcements
{
	/** The panel has little room; the server already puts the loudest first. */
	public static final int MAX_SHOWN = 3;

	private Announcements()
	{
	}

	public static List<Me.Announcement> visible(List<Me.Announcement> fromServer, Set<String> closed)
	{
		if (fromServer == null || fromServer.isEmpty())
		{
			return Collections.emptyList();
		}
		List<Me.Announcement> out = new ArrayList<>();
		for (Me.Announcement a : fromServer)
		{
			if (a == null || a.message == null || a.message.isEmpty())
			{
				continue;
			}
			if (a.canDismiss() && closed.contains(a.id))
			{
				continue;
			}
			out.add(a);
			if (out.size() == MAX_SHOWN)
			{
				break;
			}
		}
		return out;
	}

	/** Links open only on the GP Genius server the plugin is signed in to. */
	public static boolean opensOn(String link, String server)
	{
		return link != null && server != null && !server.isEmpty()
			&& (link.equals(server) || link.startsWith(server + "/"));
	}
}
