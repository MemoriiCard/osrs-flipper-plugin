package com.osrsflipper.market;

import com.osrsflipper.api.FlipperApiClient;
import com.osrsflipper.api.PriceHistory;
import java.util.function.Consumer;
import lombok.Getter;

/**
 * Keeps the price chart's history for one item and period. Refetches when either changes, and every
 * few minutes so the latest bucket appears. Call {@link #tick} from the client thread.
 */
public class ChartController
{
	static final long REFRESH_MILLIS = 5 * 60_000;

	private final FlipperApiClient api;
	private final Consumer<Runnable> onClientThread;
	private Runnable onChange = () -> { };

	@Getter
	private PriceHistory history;
	@Getter
	private String period = "day";
	@Getter
	private String error;
	private int itemId = -1;
	private String fetchedPeriod;
	private long fetchedAt;
	private boolean inFlight;
	private int generation;

	public ChartController(FlipperApiClient api, Consumer<Runnable> onClientThread)
	{
		this.api = api;
		this.onClientThread = onClientThread;
	}

	public void setOnChange(Runnable onChange)
	{
		this.onChange = onChange;
	}

	public void setPeriod(String period)
	{
		this.period = period;
	}

	/** @param wanted the item to chart, or null to show nothing */
	public void tick(Integer wanted, long now)
	{
		if (wanted == null)
		{
			if (history != null || itemId != -1)
			{
				itemId = -1;
				history = null;
				error = null;
				generation++;
				inFlight = false;
				onChange.run();
			}
			return;
		}
		boolean changed = wanted != itemId || !period.equals(fetchedPeriod);
		if (inFlight && !changed)
		{
			return;
		}
		if (!changed && now - fetchedAt < REFRESH_MILLIS)
		{
			return;
		}
		if (changed && wanted != itemId)
		{
			history = null;
		}
		itemId = wanted;
		fetchedPeriod = period;
		fetchedAt = now;
		inFlight = true;
		final int requestGeneration = ++generation;
		api.priceHistory(wanted, period,
			h -> onClientThread.accept(() -> finish(requestGeneration, h, null)),
			message -> onClientThread.accept(() -> finish(requestGeneration, null, message)));
	}

	private void finish(int requestGeneration, PriceHistory h, String message)
	{
		if (requestGeneration != generation)
		{
			return;
		}
		inFlight = false;
		if (h != null)
		{
			history = h;
		}
		error = message;
		onChange.run();
	}
}
