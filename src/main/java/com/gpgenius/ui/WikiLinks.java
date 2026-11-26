package com.gpgenius.ui;

import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 * Opens an item's Grand Exchange price chart on the OSRS Wiki: a left click on the item, or
 * "Open price chart on the OSRS Wiki" from its right-click menu. Only the item id leaves the plugin,
 * to the wiki's public price page.
 */
public final class WikiLinks
{
	public static final String PRICES = "https://prices.runescape.wiki/osrs/item/";
	public static final String MENU_TEXT = "Open price chart on the OSRS Wiki";

	private WikiLinks()
	{
	}

	/** The wiki's GE price chart for an item, or null for anything that isn't an item id. */
	public static String priceChartUrl(int itemId)
	{
		return itemId > 0 && itemId < 100_000 ? PRICES + itemId : null;
	}

	/**
	 * Makes a component open the chart for whatever item it shows now. itemId returns 0 or less
	 * when it shows no item, and then clicks do nothing.
	 */
	public static void install(JComponent target, IntSupplier itemId, Consumer<String> open)
	{
		JPopupMenu menu = new JPopupMenu();
		JMenuItem item = new JMenuItem(MENU_TEXT);
		item.addActionListener(e ->
		{
			String url = priceChartUrl(itemId.getAsInt());
			if (url != null)
			{
				open.accept(url);
			}
		});
		menu.add(item);
		target.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				popup(e);
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				popup(e);
			}

			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (e.isPopupTrigger() || e.getButton() != MouseEvent.BUTTON1)
				{
					return;
				}
				String url = priceChartUrl(itemId.getAsInt());
				if (url != null)
				{
					open.accept(url);
				}
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				boolean live = priceChartUrl(itemId.getAsInt()) != null;
				target.setCursor(live ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
				target.setToolTipText(live ? "Click to open the price chart on the OSRS Wiki" : null);
			}

			private void popup(MouseEvent e)
			{
				if (e.isPopupTrigger() && priceChartUrl(itemId.getAsInt()) != null)
				{
					menu.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		});
	}
}
