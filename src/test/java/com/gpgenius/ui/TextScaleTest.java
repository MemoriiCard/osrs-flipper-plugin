package com.gpgenius.ui;

import static org.junit.Assert.assertEquals;
import java.awt.Font;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.junit.Test;

public class TextScaleTest
{
	private static final Font BASE = new Font(Font.SANS_SERIF, Font.PLAIN, 12);

	@Test
	public void scalesExistingAndLaterRows()
	{
		JPanel root = new JPanel();
		JLabel existing = label();
		root.add(existing);
		TextScale scale = new TextScale();
		scale.install(root);
		scale.setScale(root, TextScale.Size.LARGEST.scale);
		assertEquals(18f, existing.getFont().getSize2D(), 0.01);

		// A row built later, inside a nested panel, picks up the size when added.
		JPanel row = new JPanel();
		JLabel later = label();
		row.add(later);
		root.add(row);
		assertEquals(18f, later.getFont().getSize2D(), 0.01);
	}

	@Test
	public void aFontSetLaterBecomesTheNewBase()
	{
		JPanel root = new JPanel();
		JLabel label = label();
		root.add(label);
		TextScale scale = new TextScale();
		scale.install(root);
		scale.setScale(root, 1.5f);
		label.setFont(BASE.deriveFont(Font.BOLD, 10f));
		assertEquals(15f, label.getFont().getSize2D(), 0.01);
		assertEquals(Font.BOLD, label.getFont().getStyle());
	}

	@Test
	public void backToNormalRestoresTheOriginalSize()
	{
		JPanel root = new JPanel();
		JLabel label = label();
		root.add(label);
		TextScale scale = new TextScale();
		scale.install(root);
		scale.setScale(root, 1.25f);
		scale.setScale(root, 1f);
		assertEquals(12f, label.getFont().getSize2D(), 0.01);
	}

	private static JLabel label()
	{
		JLabel l = new JLabel("text");
		l.setFont(BASE);
		return l;
	}
}
