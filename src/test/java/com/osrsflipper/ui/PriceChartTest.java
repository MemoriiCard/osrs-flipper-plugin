package com.osrsflipper.ui;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import com.osrsflipper.api.PriceHistory;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class PriceChartTest
{
	private static PriceHistory.Point point(Long high, Long low)
	{
		PriceHistory.Point p = new PriceHistory.Point();
		p.high = high;
		p.low = low;
		return p;
	}

	@Test
	public void rangeCoversBothLinesAndSkipsGaps()
	{
		List<PriceHistory.Point> points = Arrays.asList(point(110L, 100L), point(null, 95L), point(120L, null));
		assertArrayEquals(new long[]{95, 120}, PriceChart.range(points));
		assertNull(PriceChart.range(Collections.singletonList(point(null, null))));
	}

	@Test
	public void scalesToThePlot()
	{
		long[] range = {100, 200};
		assertEquals(0.0, PriceChart.y(200, range, 80), 0.001);
		assertEquals(80.0, PriceChart.y(100, range, 80), 0.001);
		assertEquals(40.0, PriceChart.y(5, new long[]{5, 5}, 80), 0.001);
		assertEquals(0, PriceChart.x(0, 3, 201));
		assertEquals(200, PriceChart.x(2, 3, 201));
	}

	@Test
	public void hoverPicksTheNearestPoint()
	{
		List<PriceHistory.Point> points = Arrays.asList(point(1L, 1L), point(2L, 2L), point(3L, 3L));
		assertEquals(0, PriceChart.nearest(points, 10, 201));
		assertEquals(1, PriceChart.nearest(points, 95, 201));
		assertEquals(2, PriceChart.nearest(points, 500, 201));
		assertEquals(-1, PriceChart.nearest(Collections.emptyList(), 5, 201));
	}
}
