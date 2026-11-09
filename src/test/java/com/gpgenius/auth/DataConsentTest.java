package com.gpgenius.auth;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DataConsentTest
{
	@Test
	public void onlyTheCurrentVersionCounts()
	{
		assertTrue(DataConsent.agreed(DataConsent.agreedValue()));
		assertFalse(DataConsent.agreed(""));
		assertFalse(DataConsent.agreed(null));
		assertFalse(DataConsent.agreed("agreed:0"));
		assertFalse(DataConsent.agreed("declined"));
	}

	@Test
	public void noticeNamesWhatIsAndIsNotSent()
	{
		assertTrue(DataConsent.NOTICE.contains("Grand Exchange offers"));
		assertTrue(DataConsent.NOTICE.contains("never sends your Jagex login"));
	}
}
