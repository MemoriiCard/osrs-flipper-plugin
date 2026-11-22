package com.gpgenius.notify;

/** Short in-game sounds the player can turn on. Each one only tells the player something happened. */
public enum SoundCue
{
	NEXT_STEP("New next step"),
	OFFER_DONE("Offer finished"),
	OUTBID("Offer outbid");

	private final String label;

	SoundCue(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
