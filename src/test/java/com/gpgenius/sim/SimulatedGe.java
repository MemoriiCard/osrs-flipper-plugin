package com.gpgenius.sim;

import com.gpgenius.tracking.AccountTrades;
import com.gpgenius.tracking.Flip;
import com.gpgenius.tracking.GeTax;
import com.gpgenius.tracking.OfferSnapshot;
import com.gpgenius.tracking.OfferStatus;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A pretend Grand Exchange and game client for tests: eight slots, a coin pouch and an inventory, and
 * the same slot-changed events RuneLite sends (including the burst of EMPTY slots at login). It drives
 * the plugin's real tracking code, so offer flows can be tested end to end without the game.
 *
 * <p>Fills happen at the offer price or better, the way the real GE does: a buy can fill below its
 * price and a sell above it, and the difference shows in the coins spent.</p>
 */
public class SimulatedGe
{
	public static final int SLOTS = 8;

	public final AccountTrades trades = new AccountTrades();
	public final Map<Integer, Long> inventory = new HashMap<>();
	public final List<Flip> flips = new ArrayList<>();
	private final OfferSnapshot[] slots = new OfferSnapshot[SLOTS];
	private final Map<Integer, String> names = new HashMap<>();
	private boolean loggedIn = true;
	private boolean trustEmpty = true;
	public long now = 1_700_000_000_000L;
	/** What the player really paid and got, to check the tracker against. */
	public long realSpent;
	public long realReceived;
	public long realTax;

	public SimulatedGe(long coins)
	{
		inventory.put(995, coins);
	}

	public SimulatedGe name(int itemId, String name)
	{
		names.put(itemId, name);
		return this;
	}

	public long coins()
	{
		return inventory.getOrDefault(995, 0L);
	}

	public int held(int itemId)
	{
		return (int) (long) inventory.getOrDefault(itemId, 0L);
	}

	public OfferSnapshot slot(int slot)
	{
		return slots[slot];
	}

	public int freeSlot()
	{
		for (int i = 0; i < SLOTS; i++)
		{
			if (slots[i] == null)
			{
				return i;
			}
		}
		return -1;
	}

	/** Places a buy: the coins leave the pouch straight away, as in game. */
	public int buy(int itemId, long price, int quantity)
	{
		int slot = freeSlot();
		if (slot < 0 || coins() < price * quantity)
		{
			throw new IllegalStateException("no free slot or not enough coins");
		}
		inventory.merge(995, -price * quantity, Long::sum);
		slots[slot] = new OfferSnapshot(slot, itemId, OfferStatus.BUYING, price, quantity, 0, 0);
		emit(slot);
		return slot;
	}

	/** Places a sell: the items leave the inventory straight away. */
	public int sell(int itemId, long price, int quantity)
	{
		int slot = freeSlot();
		if (slot < 0 || held(itemId) < quantity)
		{
			throw new IllegalStateException("no free slot or not enough items");
		}
		inventory.merge(itemId, (long) -quantity, Long::sum);
		slots[slot] = new OfferSnapshot(slot, itemId, OfferStatus.SELLING, price, quantity, 0, 0);
		emit(slot);
		return slot;
	}

	/**
	 * Fills part of an offer at a given price per item (at or better than the offer's). Logged out, the
	 * fill still happens but the client hears about it only at the next login.
	 */
	public void fill(int slot, int quantity, long atPrice)
	{
		OfferSnapshot o = slots[slot];
		boolean buy = o.status.isBuy();
		if (buy ? atPrice > o.price : atPrice < o.price)
		{
			throw new IllegalArgumentException("the GE never fills worse than the offer price");
		}
		int take = Math.min(quantity, o.totalQuantity - o.quantityFilled);
		if (take <= 0)
		{
			return;
		}
		long coins = take * atPrice;
		OfferSnapshot next = copy(o);
		next.quantityFilled += take;
		next.spent += coins;
		if (!buy)
		{
			long tax = GeTax.total(o.itemId, atPrice, take);
			realTax += tax;
			realReceived += coins - tax;
			sellTax.merge(slot, tax, Long::sum);
		}
		else
		{
			realSpent += coins;
		}
		if (next.quantityFilled == next.totalQuantity)
		{
			next.status = buy ? OfferStatus.BOUGHT : OfferStatus.SOLD;
		}
		slots[slot] = next;
		if (loggedIn)
		{
			emit(slot);
		}
	}

	/** Cancels what's left of an offer. The filled part stays to collect. */
	public void cancel(int slot)
	{
		OfferSnapshot next = copy(slots[slot]);
		next.status = next.status.isBuy() ? OfferStatus.CANCELLED_BUY : OfferStatus.CANCELLED_SELL;
		slots[slot] = next;
		emit(slot);
	}

	/** Collects a finished or cancelled offer into the inventory and empties the slot. */
	public void collect(int slot)
	{
		OfferSnapshot o = slots[slot];
		if (o.status.isActive())
		{
			throw new IllegalStateException("cancel it first, or wait until it's done");
		}
		if (o.status.isBuy())
		{
			inventory.merge(o.itemId, (long) o.quantityFilled, Long::sum);
			// Unspent coins come back: the rest of the order, and the savings from filling below the price.
			inventory.merge(995, o.price * o.totalQuantity - o.spent, Long::sum);
		}
		else
		{
			inventory.merge(o.itemId, (long) (o.totalQuantity - o.quantityFilled), Long::sum);
			inventory.merge(995, o.spent - taxOn(o), Long::sum);
		}
		slots[slot] = null;
		emitEmpty(slot);
	}

	/** Tax the GE took from a sell offer's fills, as charged per fill. */
	private final Map<Integer, Long> sellTax = new HashMap<>();

	private long taxOn(OfferSnapshot o)
	{
		Long t = sellTax.remove(o.slot);
		return t == null ? 0 : t;
	}

	public void logout()
	{
		loggedIn = false;
	}

	/**
	 * Logs in: RuneLite first reports every slot as EMPTY, then the real state of each, and only
	 * after the burst can an EMPTY be trusted as a cleared slot.
	 */
	public void login()
	{
		loggedIn = true;
		trustEmpty = false;
		for (int i = 0; i < SLOTS; i++)
		{
			emitEmpty(i);
		}
		for (int i = 0; i < SLOTS; i++)
		{
			if (slots[i] != null)
			{
				emit(i);
			}
		}
		trustEmpty = true;
	}

	public void pass(long millis)
	{
		now += millis;
	}

	private void emit(int slot)
	{
		now += 600; // a game tick
		OfferSnapshot o = slots[slot];
		Flip flip = trades.onOffer(copy(o), names.getOrDefault(o.itemId, "Item " + o.itemId), now, trustEmpty);
		if (flip != null)
		{
			flips.add(flip);
		}
	}

	private void emitEmpty(int slot)
	{
		now += 600;
		OfferSnapshot empty = new OfferSnapshot(slot, 0, OfferStatus.EMPTY, 0, 0, 0, 0);
		trades.onOffer(empty, null, now, trustEmpty);
	}

	private static OfferSnapshot copy(OfferSnapshot o)
	{
		return new OfferSnapshot(o.slot, o.itemId, o.status, o.price, o.totalQuantity, o.quantityFilled, o.spent);
	}
}
