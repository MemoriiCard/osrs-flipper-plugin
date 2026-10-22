package com.osrsflipper.tracking;

/**
 * A matched buy and sell of the same item.
 */
public class Flip
{
	public int itemId;
	public String itemName;
	public int quantity;
	/** Average coins per item paid. */
	public long buyPrice;
	/** Coins per item received before tax. */
	public long sellPrice;
	public long buyTimeMillis;
	public long sellTimeMillis;
	public long tax;
	public long profit;

	public Flip()
	{
	}

	public Flip(int itemId, String itemName, int quantity, long buyCost, long sellPrice, long buyTimeMillis, long sellTimeMillis)
	{
		this.itemId = itemId;
		this.itemName = itemName;
		this.quantity = quantity;
		this.buyPrice = quantity == 0 ? 0 : buyCost / quantity;
		this.sellPrice = sellPrice;
		this.buyTimeMillis = buyTimeMillis;
		this.sellTimeMillis = sellTimeMillis;
		this.tax = GeTax.total(itemId, sellPrice, quantity);
		this.profit = sellPrice * quantity - tax - buyCost;
	}
}
