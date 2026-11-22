package com.gpgenius.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.junit.Test;

public class AnnouncementsTest
{
	private static Me.Announcement note(String id, String message, Boolean dismissible)
	{
		Me.Announcement a = new Me.Announcement();
		a.id = id;
		a.message = message;
		a.level = "info";
		a.dismissible = dismissible;
		return a;
	}

	@Test
	public void closedOnesAreHiddenButStickyOnesStay()
	{
		List<Me.Announcement> server = Arrays.asList(
			note("a", "closable", true),
			note("b", "sticky", false),
			note("c", "old server", null),
			note("d", "", true));
		List<Me.Announcement> shown = Announcements.visible(server, new HashSet<>(Arrays.asList("a", "b", "c")));
		assertEquals(1, shown.size());
		assertEquals("sticky", shown.get(0).message);
	}

	@Test
	public void showsAtMostThree()
	{
		List<Me.Announcement> server = Arrays.asList(
			note("1", "one", true), note("2", "two", true), note("3", "three", true), note("4", "four", true));
		assertEquals(Announcements.MAX_SHOWN, Announcements.visible(server, Collections.emptySet()).size());
		assertTrue(Announcements.visible(null, Collections.emptySet()).isEmpty());
	}

	@Test
	public void oldServersWithoutAnIdCannotBeClosed()
	{
		assertFalse(note(null, "x", true).canDismiss());
		assertTrue(note("x", "x", null).canDismiss());
	}

	@Test
	public void linksOpenOnlyOnTheServer()
	{
		String server = "https://gpgenius.example";
		assertTrue(Announcements.opensOn("https://gpgenius.example/status#incident-1", server));
		assertFalse(Announcements.opensOn("https://gpgenius.example.evil.test/x", server));
		assertFalse(Announcements.opensOn("https://discord.gg/abc", server));
		assertFalse(Announcements.opensOn(null, server));
	}
}
