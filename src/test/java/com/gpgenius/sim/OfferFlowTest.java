package com.gpgenius.sim;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.gpgenius.api.SuggestionRequest;
import com.gpgenius.suggestion.RequestBuilder;
import com.gpgenius.sync.TradeSync;
import com.gpgenius.tracking.Flip;
import com.gpgenius.tracking.Transaction;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.Test;

/** Whole offer flows through the plugin's real tracking, sync and request code, on a simulated GE. */
public class OfferFlowTest
{
	private static final int WHIP = 4151;
	private static final int SHARK = 385;

	@Test
	public void buyThenSellIsOneFlipWithTheRealProfit()
	{
		SimulatedGe ge = new SimulatedGe(10_000_000).name(WHIP, "Abyssal whip");
		int slot = ge.buy(WHIP, 1_500_000, 2);
		ge.fill(slot, 2, 1_490_000); // filled below the offer price
		ge.collect(slot);
		int sell = ge.sell(WHIP, 1_560_000, 2);
		ge.fill(sell, 2, 1_560_000);
		ge.collect(sell);

		assertEquals(1, ge.flips.size());
		Flip flip = ge.flips.get(0);
		assertEquals(2, flip.quantity);
		assertEquals(1_490_000, flip.buyPrice);
		assertEquals(ge.realReceived - ge.realSpent, flip.profit);
		assertEquals(ge.realTax, flip.tax);
		assertEquals(10_000_000 + flip.profit, ge.coins());
		assertTrue(ge.trades.getMatcher().getHoldings().isEmpty());
	}

	@Test
	public void partialFillsAndACancelRecordOnlyWhatFilled()
	{
		SimulatedGe ge = new SimulatedGe(5_000_000);
		int slot = ge.buy(SHARK, 900, 5_000);
		ge.fill(slot, 1_200, 900);
		ge.fill(slot, 0, 900);
		ge.fill(slot, 800, 880);
		ge.cancel(slot);
		ge.collect(slot);

		List<Transaction> all = ge.trades.getTransactions();
		assertEquals(2, all.size());
		assertEquals(2_000, all.stream().mapToInt(t -> t.quantity).sum());
		assertEquals(1_200 * 900 + 800 * 880, all.stream().mapToLong(t -> t.totalCoins).sum());
		assertEquals(Integer.valueOf(2_000), ge.trades.getMatcher().getHoldings().get(SHARK));
		assertEquals(5_000_000 - (1_200 * 900 + 800 * 880), ge.coins());
		assertEquals(2_000, ge.held(SHARK));
	}

	@Test
	public void fillsWhileLoggedOutAreCountedOnceAtLogin()
	{
		SimulatedGe ge = new SimulatedGe(5_000_000);
		int slot = ge.buy(SHARK, 900, 3_000);
		ge.fill(slot, 1_000, 900);
		ge.logout();
		ge.fill(slot, 1_500, 900);
		ge.pass(3_600_000);
		ge.login();
		// The EMPTY burst at login didn't wipe the slot, so only the 1,500 bought while away are new.
		assertEquals(2, ge.trades.getTransactions().size());
		assertEquals(1_500, ge.trades.getTransactions().get(1).quantity);
		ge.login(); // a second login (world hop) adds nothing
		assertEquals(2, ge.trades.getTransactions().size());
		ge.fill(slot, 500, 900);
		assertEquals(3_000, ge.trades.getTransactions().stream().mapToInt(t -> t.quantity).sum());
	}

	@Test
	public void aSlotReusedForTheSameItemIsANewOffer()
	{
		SimulatedGe ge = new SimulatedGe(5_000_000);
		int slot = ge.buy(SHARK, 900, 1_000);
		ge.fill(slot, 1_000, 900);
		ge.collect(slot);
		int again = ge.buy(SHARK, 900, 1_000);
		assertEquals(slot, again);
		ge.fill(again, 400, 900);
		assertEquals(1_400, ge.trades.getTransactions().stream().mapToInt(t -> t.quantity).sum());
	}

	@Test
	public void tradesMadeWhileTheServerIsDownUploadOnceWhenItsBack()
	{
		SimulatedGe ge = new SimulatedGe(5_000_000);
		FakeServer server = new FakeServer();
		server.up = false;
		int slot = ge.buy(SHARK, 900, 2_000);
		for (int i = 0; i < 4; i++)
		{
			ge.fill(slot, 500, 900);
			sync(ge, server);
		}
		assertTrue(server.stored.isEmpty());
		assertEquals(4, TradeSync.pending(ge.trades.getTransactions(), 100).size());

		server.up = true;
		server.loseNextReply = true; // stored, but the plugin never hears back
		sync(ge, server);
		assertEquals(4, server.stored.size());
		assertEquals(4, TradeSync.pending(ge.trades.getTransactions(), 100).size());
		sync(ge, server); // the resend changes nothing on the server
		assertEquals(4, server.stored.size());
		assertTrue(TradeSync.pending(ge.trades.getTransactions(), 100).isEmpty());
		assertEquals(2_000, server.stored.values().stream().mapToInt(t -> t.quantity).sum());
	}

	@Test
	public void onlyItemsTheTrackerSawBoughtAreOfferedForSale()
	{
		SimulatedGe ge = new SimulatedGe(5_000_000);
		ge.inventory.put(WHIP, 1L); // the player's own weapon
		int slot = ge.buy(SHARK, 900, 1_000);
		ge.fill(slot, 600, 900);
		ge.cancel(slot);
		ge.collect(slot);

		SuggestionRequest request = RequestBuilder.build(new RequestBuilder.Settings(),
			Collections.<SuggestionRequest.Offer>emptyList(), ge.inventory, ge.trades.getMatcher().getHoldings(),
			true, Collections.<Integer>emptyList());
		assertEquals(Integer.valueOf(600), request.account.held.get(SHARK));
		assertFalse(request.account.held.containsKey(WHIP));
		assertEquals(ge.coins(), request.account.coins);
	}

	/** Random sessions: whatever happens, the tracker agrees with what really traded. */
	@Test
	public void randomSessionsKeepTheBooksStraight()
	{
		for (int seed = 0; seed < 200; seed++)
		{
			Random rng = new Random(seed);
			SimulatedGe ge = new SimulatedGe(50_000_000);
			FakeServer server = new FakeServer();
			int[] items = {SHARK, WHIP, 561, 2};
			long bought = 0;
			long sold = 0;
			for (int step = 0; step < 120; step++)
			{
				int roll = rng.nextInt(100);
				int slot = rng.nextInt(SimulatedGe.SLOTS);
				int item = items[rng.nextInt(items.length)];
				long price = item == WHIP ? 1_500_000 : 100 + rng.nextInt(900);
				if (roll < 25 && ge.freeSlot() >= 0 && ge.coins() > price * 50)
				{
					ge.buy(item, price, 1 + rng.nextInt(50));
				}
				else if (roll < 40 && ge.freeSlot() >= 0)
				{
					for (int it : items)
					{
						if (ge.held(it) > 0)
						{
							ge.sell(it, it == WHIP ? 1_520_000 : 150 + rng.nextInt(900), 1 + rng.nextInt(ge.held(it)));
							break;
						}
					}
				}
				else if (roll < 75 && ge.slot(slot) != null && ge.slot(slot).status.isActive())
				{
					boolean buy = ge.slot(slot).status.isBuy();
					long at = buy ? ge.slot(slot).price - rng.nextInt(3) : ge.slot(slot).price + rng.nextInt(3);
					int before = ge.slot(slot).quantityFilled;
					ge.fill(slot, 1 + rng.nextInt(30), at);
					int took = ge.slot(slot).quantityFilled - before;
					if (buy)
					{
						bought += took;
					}
					else
					{
						sold += took;
					}
				}
				else if (roll < 82 && ge.slot(slot) != null && ge.slot(slot).status.isActive())
				{
					ge.cancel(slot);
				}
				else if (roll < 92 && ge.slot(slot) != null && !ge.slot(slot).status.isActive())
				{
					ge.collect(slot);
				}
				else if (roll < 95)
				{
					ge.logout();
					ge.pass(rng.nextInt(7_200_000));
					// Offers keep filling while away.
					for (int s = 0; s < SimulatedGe.SLOTS; s++)
					{
						if (ge.slot(s) != null && ge.slot(s).status.isActive() && rng.nextBoolean())
						{
							int before = ge.slot(s).quantityFilled;
							ge.fill(s, 1 + rng.nextInt(10), ge.slot(s).price);
							int took = ge.slot(s).quantityFilled - before;
							if (ge.slot(s).status.isBuy())
							{
								bought += took;
							}
							else
							{
								sold += took;
							}
						}
					}
					ge.login();
				}
				else
				{
					server.up = rng.nextInt(3) > 0;
					server.loseNextReply = rng.nextInt(4) == 0;
					sync(ge, server);
				}
			}
			server.up = true;
			server.loseNextReply = false;
			sync(ge, server);

			List<Transaction> all = ge.trades.getTransactions();
			String at = "seed " + seed;
			assertEquals(at, bought, all.stream().filter(t -> t.buy).mapToLong(t -> t.quantity).sum());
			assertEquals(at, sold, all.stream().filter(t -> !t.buy).mapToLong(t -> t.quantity).sum());
			Set<String> ids = new HashSet<>();
			all.forEach(t -> assertTrue(at, ids.add(t.clientId)));
			assertEquals(at, all.size(), server.stored.size());
			assertTrue(at, TradeSync.pending(all, 10_000).isEmpty());
			long matched = ge.flips.stream().mapToLong(f -> f.quantity).sum();
			assertTrue(at, matched <= Math.min(bought, sold));
			long open = 0;
			for (Map.Entry<Integer, Integer> e : ge.trades.getMatcher().getHoldings().entrySet())
			{
				open += e.getValue();
			}
			assertEquals(at, bought - matched, open);
		}
	}

	private static void sync(SimulatedGe ge, FakeServer server)
	{
		List<Transaction> batch = TradeSync.pending(ge.trades.getTransactions(), TradeSync.BATCH);
		if (batch.isEmpty())
		{
			return;
		}
		if (server.upload(TradeSync.build(-1L, "Tester", batch)) >= 0)
		{
			TradeSync.markUploaded(batch);
		}
	}
}
