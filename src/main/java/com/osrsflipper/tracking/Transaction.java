package com.osrsflipper.tracking;

import java.util.UUID;

/**
 * A fill of some quantity at one GE slot, derived from the change between two slot snapshots.
 */
public class Transaction
{
	public int itemId;
	public String itemName;
	public boolean buy;
	public int quantity;
	/** Coins per item before tax. */
	public int pricePerItem;
	/** Total coins spent (buy) or received before tax (sell). */
	public long totalCoins;
	public long timestampMillis;
	public int slot;
	/** Random ID so the server can ignore a trade it already has when an upload is retried. */
	public String clientId;
	/** Whether the server has acknowledged this trade. */
	public boolean uploaded;

	public Transaction()
	{
	}

	public Transaction(int itemId, String itemName, boolean buy, int quantity, long totalCoins, long timestampMillis, int slot)
	{
		this.itemId = itemId;
		this.itemName = itemName;
		this.buy = buy;
		this.quantity = quantity;
		this.totalCoins = totalCoins;
		this.pricePerItem = quantity == 0 ? 0 : (int) (totalCoins / quantity);
		this.timestampMillis = timestampMillis;
		this.slot = slot;
		this.clientId = UUID.randomUUID().toString();
	}
}
