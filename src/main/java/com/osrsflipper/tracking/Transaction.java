package com.osrsflipper.tracking;

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
	}
}
