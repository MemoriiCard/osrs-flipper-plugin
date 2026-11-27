package com.gpgenius.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gpgenius.api.ErrorLog;
import com.gpgenius.api.FlipperApiClient;
import com.gpgenius.auth.AuthSession;
import com.gpgenius.auth.AuthState;
import com.gpgenius.auth.TokenFile;
import com.gpgenius.setup.Onboarding;
import com.sun.net.httpserver.HttpServer;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.io.File;
import java.io.OutputStream;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import okhttp3.OkHttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Clicks through the side panel the way a player does: the real FlipperPanel in a window, real
 * buttons and dialogs, and the real AuthSession and FlipperApiClient talking HTTP to a stand-in GP
 * Genius server on localhost. The panel's actions are wired as GpGeniusPlugin wires them (the game
 * client itself can't run here, so the client-thread hop is a direct call).
 *
 * <p>Covers the three flows a tester meets first: the server-down notice (and it clearing), Report a
 * problem (sent, refused while the server is down, and Get help when signed out), and the first-run
 * guide from its first question to the first suggestion. Needs a display: CI runs it under xvfb, and
 * it is skipped on a headless JVM.</p>
 */
public class PanelClickThroughTest
{
	private static final long WAIT_MS = 10_000;

	private final Gson gson = new Gson();
	private HttpServer server;
	private String baseUrl;
	/** 0: answer normally. Otherwise answer every request with this status. */
	private final AtomicInteger failWith = new AtomicInteger();
	private final ConcurrentLinkedQueue<String> reports = new ConcurrentLinkedQueue<>();
	private final AtomicInteger refreshes = new AtomicInteger();

	private ScheduledExecutorService executor;
	private AuthSession auth;
	private FlipperApiClient api;
	private FlipperPanel panel;
	private JFrame frame;
	private File tokenDir;

	private final List<String> calls = new ArrayList<>();
	private final AtomicReference<Onboarding.Choices> finished = new AtomicReference<>();
	private Onboarding.Detected detected;

	@Before
	public void setUp() throws Exception
	{
		assumeFalse("needs a display (run under xvfb)", GraphicsEnvironment.isHeadless());
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/", exchange ->
		{
			String path = exchange.getRequestURI().getPath();
			String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
			int status = failWith.get();
			String reply;
			if (status != 0)
			{
				reply = "<html>Bad gateway</html>";
			}
			else if (path.equals("/v1/auth/refresh"))
			{
				refreshes.incrementAndGet();
				status = 200;
				reply = "{\"access_token\":\"access-1\",\"refresh_token\":\"refresh-2\",\"expires_in\":900}";
			}
			else if (path.equals("/v1/plugin/report"))
			{
				reports.add(body);
				status = 200;
				reply = "{\"reference\":\"GP-41\",\"message\":\"Thanks, we've got it (GP-41). If you open a ticket"
					+ " in Discord, staff will already see it.\"}";
			}
			else
			{
				status = 200;
				reply = "{}";
			}
			byte[] bytes = reply.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(status, bytes.length);
			try (OutputStream out = exchange.getResponseBody())
			{
				out.write(bytes);
			}
		});
		server.start();
		baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();

		tokenDir = Files.createTempDirectory("gpgenius-harness").toFile();
		TokenFile tokens = new TokenFile(gson, tokenDir);
		tokens.save("refresh-1", baseUrl);
		executor = Executors.newSingleThreadScheduledExecutor();
		auth = new AuthSession(new OkHttpClient(), gson, executor);
		auth.start(tokens, () -> baseUrl, () -> { });
		api = new FlipperApiClient(auth, gson, new ErrorLog());

		detected = new Onboarding.Detected();
		detected.inventoryCoins = 12_000_000;
		detected.members = true;
		detected.slotsInUse = 1;

		onEdt(() ->
		{
			panel = new FlipperPanel(actions());
			frame = new JFrame("GP Genius harness");
			frame.add(panel);
			frame.setSize(260, 900);
			frame.setVisible(true);
		});
	}

	@After
	public void tearDown()
	{
		if (frame != null)
		{
			onEdt(frame::dispose);
		}
		for (Window w : Window.getWindows())
		{
			w.dispose();
		}
		if (server != null)
		{
			server.stop(0);
		}
		if (auth != null)
		{
			auth.stop();
		}
		if (executor != null)
		{
			executor.shutdownNow();
		}
	}

	/** The panel's actions, as GpGeniusPlugin.Actions does them for these flows; anything else is recorded. */
	private PanelActions actions()
	{
		return (PanelActions) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{PanelActions.class},
			(proxy, method, args) ->
			{
				synchronized (calls)
				{
					calls.add(method.getName());
				}
				switch (method.getName())
				{
					case "reportProblem":
						api.reportProblem(null, "test", (String) args[0], (Boolean) args[1],
							message -> SwingUtilities.invokeLater(() -> panel.showProblemReply(message, true)),
							message -> SwingUtilities.invokeLater(() -> panel.showProblemReply(message, false)));
						return null;
					case "startOnboarding":
						SwingUtilities.invokeLater(() -> panel.startOnboarding(detected, "MEDIUM", 30));
						return null;
					case "finishOnboarding":
						finished.set((Onboarding.Choices) args[0]);
						return null;
					case "hashCode":
						return System.identityHashCode(proxy);
					case "equals":
						return proxy == args[0];
					case "toString":
						return "harness actions";
					default:
						return method.isDefault() ? null : defaultValue(method.getReturnType());
				}
			});
	}

	private static Object defaultValue(Class<?> type)
	{
		if (type == boolean.class)
		{
			return false;
		}
		if (type == int.class || type == long.class || type == double.class)
		{
			return 0;
		}
		return null;
	}

	// The server-down notice

	@Test
	public void serverDownNoticeShowsAndClears() throws Exception
	{
		signIn();
		showSuggestionPanel();
		assertTrue(visibleText().contains("Finding your next flip..."));

		// The proxy in front of the server answers 502: two failed requests put the notice up.
		failWith.set(502);
		request();
		assertNull("one failure is not an outage", auth.health().notice(auth.server()));
		request();
		showSuggestionPanel();
		String text = visibleText();
		assertTrue(text, text.contains("The GP Genius server is down or restarting"));
		assertTrue(text, text.contains("Status: " + baseUrl + "/status."));
		assertTrue(text, text.contains("nothing is lost"));
		assertTrue("automatic requests back off", auth.health().backoffMillis() > 0);

		// It answers again: the notice goes and the suggestion area is back to waiting for a flip.
		failWith.set(0);
		request();
		showSuggestionPanel();
		text = visibleText();
		assertFalse(text, text.contains("down or restarting"));
		assertTrue(text, text.contains("Finding your next flip..."));
	}

	@Test
	public void unreachableServerSaysCheckYourInternet() throws Exception
	{
		signIn();
		server.stop(0);
		request();
		request();
		showSuggestionPanel();
		String text = visibleText();
		assertTrue(text, text.contains("Can't reach the GP Genius server since"));
		assertTrue(text, text.contains("Check your internet connection"));
	}

	// Report a problem

	@Test
	public void reportAProblemSendsAndThanks() throws Exception
	{
		signIn();
		AbstractButton report = button("Report a problem");
		assertTrue(report.isShowing());
		SwingUtilities.invokeLater(report::doClick);

		JDialog form = dialog("Report a problem", d -> find(d, JTextArea.class) != null);
		JTextArea what = find(form, JTextArea.class);
		onEdt(() -> what.setText("The highlight stayed on slot 3 after the offer finished."));
		click(form, "OK");

		JDialog thanks = dialog("Report a problem", d -> text(d).contains("GP-41"));
		assertTrue(text(thanks).contains("Thanks, we've got it (GP-41)"));
		click(thanks, "OK");

		assertEquals(1, reports.size());
		JsonObject sent = new JsonParser().parse(reports.peek()).getAsJsonObject();
		assertEquals("The highlight stayed on slot 3 after the offer finished.", sent.get("what").getAsString());
		assertEquals("test", sent.get("plugin_version").getAsString());
		assertTrue(sent.get("errors").isJsonArray());
		waitFor(() -> report.isEnabled() && report.getText().equals("Report a problem"));
	}

	@Test
	public void reportAProblemWhileTheServerIsDownSaysWhy() throws Exception
	{
		signIn();
		failWith.set(503);
		AbstractButton report = button("Report a problem");
		SwingUtilities.invokeLater(report::doClick);
		JDialog form = dialog("Report a problem", d -> find(d, JTextArea.class) != null);
		onEdt(() -> find(form, JTextArea.class).setText("Prices look stuck."));
		click(form, "OK");

		JDialog warning = dialog("Report a problem", d -> find(d, JTextArea.class) == null);
		String message = text(warning);
		assertTrue(message, message.contains("down or restarting"));
		assertFalse("a server error never reads as sent: " + message, message.contains("Thanks"));
		assertFalse("no raw HTML from the proxy: " + message, message.contains("Bad gateway"));
		click(warning, "OK");
		assertTrue(reports.isEmpty());
		waitFor(() -> report.isEnabled() && report.getText().equals("Report a problem"));
	}

	@Test
	public void emptyReportIsNotSent() throws Exception
	{
		signIn();
		SwingUtilities.invokeLater(button("Report a problem")::doClick);
		JDialog form = dialog("Report a problem", d -> find(d, JTextArea.class) != null);
		click(form, "OK");
		waitFor(() -> !form.isShowing());
		Thread.sleep(300);
		synchronized (calls)
		{
			assertFalse(calls.contains("reportProblem"));
		}
		assertTrue(button("Report a problem").isEnabled());
	}

	@Test
	public void signedOutTheButtonIsGetHelp() throws Exception
	{
		onEdt(() -> panel.showAccount(AuthState.SIGNED_OUT, null, null, null, true));
		AbstractButton help = button("Get help");
		onEdt(help::doClick);
		synchronized (calls)
		{
			assertTrue(calls.contains("openHelp"));
			assertFalse(calls.contains("reportProblem"));
		}
	}

	// The first-run guide

	@Test
	public void firstRunGuideToTheFirstSuggestion() throws Exception
	{
		signIn();
		// What the plugin does at start-up when setup isn't done yet.
		onEdt(() -> panel.showSetup(true));
		waitFor(() -> visibleText().contains("Step 1 of 4"));
		String text = visibleText();
		assertTrue(text, text.contains("Your cash stack"));
		assertTrue(text, text.contains("12m") || text.contains("12,000,000"));

		// Step 1: a cap instead of all the coins carried.
		onEdt(() ->
		{
			for (AbstractButton b : all(panel, AbstractButton.class))
			{
				if (b.isShowing() && b.getText() != null && b.getText().startsWith("Use all the coins"))
				{
					b.doClick();
				}
			}
			JTextField cash = showing(panel, JTextField.class);
			cash.setText("5m");
		});
		next();
		waitFor(() -> visibleText().contains("Step 2 of 4"));

		// Step 2: risk.
		onEdt(() -> button("High").doClick());
		next();
		waitFor(() -> visibleText().contains("Step 3 of 4"));

		// Step 3: check-in time, the second-longest choice.
		onEdt(() ->
		{
			JComboBox<?> box = showing(panel, JComboBox.class);
			box.setSelectedIndex(box.getItemCount() - 2);
		});
		next();
		waitFor(() -> visibleText().contains("Step 4 of 4"));
		assertTrue(visibleText().contains("members world"));

		// Step 4: keep two of eight slots for the player's own trades.
		onEdt(() -> showing(panel, JComboBox.class).setSelectedItem(6));
		next();
		waitFor(() -> visibleText().contains("All set"));
		assertTrue(visibleText().contains("Ready"));

		onEdt(() -> button("Get my first suggestion").doClick());
		waitFor(() -> finished.get() != null);
		Map<String, Object> settings = Onboarding.settings(finished.get());
		assertEquals(5_000_000, settings.get("coinCap"));
		assertEquals("HIGH", settings.get("riskLevel"));
		assertEquals(2, settings.get("reservedSlots"));
		assertEquals(true, settings.get("setupDone"));
		assertNotNull(settings.get("checkIn"));
		waitFor(() -> !panel.onboardingOpen());
	}

	@Test
	public void firstRunGuideRefusesAnEmptyCashAmountAndCanBeSkipped() throws Exception
	{
		signIn();
		onEdt(() -> panel.showSetup(true));
		waitFor(() -> visibleText().contains("Step 1 of 4"));
		onEdt(() ->
		{
			for (AbstractButton b : all(panel, AbstractButton.class))
			{
				if (b.isShowing() && b.getText() != null && b.getText().startsWith("Use all the coins"))
				{
					b.doClick();
				}
			}
			showing(panel, JTextField.class).setText("");
		});
		next();
		waitFor(() -> visibleText().contains("Type an amount such as 25m"));
		assertTrue(visibleText().contains("Step 1 of 4"));

		onEdt(() -> button("Skip").doClick());
		waitFor(() -> !panel.onboardingOpen());
		synchronized (calls)
		{
			assertTrue(calls.contains("skipSetup"));
		}
		assertNull(finished.get());
	}

	@Test
	public void firstRunGuideNoticesTheBankOpening() throws Exception
	{
		signIn();
		onEdt(() -> panel.showSetup(true));
		waitFor(() -> visibleText().contains("Open your bank to count those coins too"));
		Onboarding.Detected withBank = new Onboarding.Detected();
		withBank.inventoryCoins = 12_000_000;
		withBank.bankCoins = 80_000_000;
		withBank.members = true;
		onEdt(() -> panel.onboardingDetected(withBank));
		waitFor(() -> visibleText().contains("in your bank"));
	}

	// Helpers

	private void signIn()
	{
		onEdt(() -> panel.showAccount(AuthState.SIGNED_IN, null, null, null, true));
		assertEquals(AuthState.SIGNED_IN, auth.getState());
	}

	/** What the plugin's updateSuggestionPanel shows for a signed-in player with no suggestion yet. */
	private void showSuggestionPanel()
	{
		String outage = auth.getState() == AuthState.SIGNED_IN ? auth.health().notice(auth.server()) : null;
		onEdt(() -> panel.showSuggestion(true, null, outage));
	}

	/** One authenticated request, as the plugin's own polling makes, waited for. */
	private void request() throws InterruptedException
	{
		java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(1);
		auth.send("/v1/me", null, response ->
		{
			response.close();
			done.countDown();
		}, message -> done.countDown());
		assertTrue("request finished", done.await(WAIT_MS, TimeUnit.MILLISECONDS));
	}

	private void next()
	{
		onEdt(() -> button("Next").doClick());
	}

	private AbstractButton button(String label)
	{
		AtomicReference<AbstractButton> found = new AtomicReference<>();
		onEdt(() ->
		{
			for (AbstractButton b : all(panel, AbstractButton.class))
			{
				if (label.equals(b.getText()) && b.isShowing())
				{
					found.set(b);
					return;
				}
			}
		});
		assertNotNull("no button \"" + label + "\" on show; showing: " + visibleText(), found.get());
		return found.get();
	}

	private JDialog dialog(String title, Predicate<JDialog> matches) throws InterruptedException
	{
		long until = System.currentTimeMillis() + WAIT_MS;
		while (System.currentTimeMillis() < until)
		{
			AtomicReference<JDialog> found = new AtomicReference<>();
			onEdt(() ->
			{
				for (Window w : Window.getWindows())
				{
					if (w instanceof JDialog && w.isShowing() && title.equals(((JDialog) w).getTitle())
						&& matches.test((JDialog) w))
					{
						found.set((JDialog) w);
					}
				}
			});
			if (found.get() != null)
			{
				return found.get();
			}
			Thread.sleep(50);
		}
		throw new AssertionError("no \"" + title + "\" dialog came up");
	}

	private void click(Container root, String label)
	{
		onEdt(() ->
		{
			for (AbstractButton b : all(root, AbstractButton.class))
			{
				if (label.equals(b.getText()))
				{
					b.doClick();
					return;
				}
			}
			throw new AssertionError("no " + label + " button in " + text(root));
		});
	}

	private String visibleText()
	{
		AtomicReference<String> out = new AtomicReference<>();
		onEdt(() -> out.set(text(panel)));
		return out.get();
	}

	/** Every showing label, button and text box's text, tags stripped. */
	private static String text(Container root)
	{
		StringBuilder s = new StringBuilder();
		for (Component c : all(root, Component.class))
		{
			if (!c.isShowing())
			{
				continue;
			}
			String t = null;
			if (c instanceof JLabel)
			{
				t = ((JLabel) c).getText();
			}
			else if (c instanceof AbstractButton)
			{
				t = ((AbstractButton) c).getText();
			}
			else if (c instanceof JTextComponent)
			{
				t = ((JTextComponent) c).getText();
			}
			if (t != null && !t.isEmpty())
			{
				s.append(t.replaceAll("<[^>]+>", " ").replace("&nbsp;", " ").replace("&amp;", "&")).append('\n');
			}
		}
		return s.toString();
	}

	private static <T> T find(Container root, Class<T> type)
	{
		List<T> found = all(root, type);
		return found.isEmpty() ? null : found.get(0);
	}

	private static <T> T showing(Container root, Class<T> type)
	{
		for (T c : all(root, type))
		{
			if (((Component) c).isShowing())
			{
				return c;
			}
		}
		throw new AssertionError("no " + type.getSimpleName() + " on show");
	}

	private static <T> List<T> all(Container root, Class<T> type)
	{
		List<T> out = new ArrayList<>();
		collect(root, type, out);
		return out;
	}

	private static <T> void collect(Container root, Class<T> type, List<T> out)
	{
		for (Component c : root.getComponents())
		{
			if (type.isInstance(c))
			{
				out.add(type.cast(c));
			}
			if (c instanceof Container)
			{
				collect((Container) c, type, out);
			}
		}
	}

	private static void waitFor(java.util.function.BooleanSupplier condition) throws InterruptedException
	{
		long until = System.currentTimeMillis() + WAIT_MS;
		while (System.currentTimeMillis() < until)
		{
			AtomicReference<Boolean> ok = new AtomicReference<>(false);
			onEdt(() -> ok.set(condition.getAsBoolean()));
			if (ok.get())
			{
				return;
			}
			Thread.sleep(50);
		}
		throw new AssertionError("timed out waiting");
	}

	private static void onEdt(Runnable r)
	{
		if (SwingUtilities.isEventDispatchThread())
		{
			r.run();
			return;
		}
		try
		{
			SwingUtilities.invokeAndWait(r);
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
		catch (java.lang.reflect.InvocationTargetException e)
		{
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeException)
			{
				throw (RuntimeException) cause;
			}
			if (cause instanceof Error)
			{
				throw (Error) cause;
			}
			throw new IllegalStateException(cause);
		}
	}
}
