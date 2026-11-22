package com.gpgenius.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.beans.PropertyChangeListener;
import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.text.JTextComponent;

/**
 * Large-text mode for the side panel. Every text component under the root keeps its own font as a
 * base and is shown at {@code base x scale}. Rows the panel builds later (flips, plan steps,
 * alerts) are picked up as they're added, and a font the panel sets later becomes the new base, so
 * the panel code doesn't need to know about the setting.
 *
 * <p>Swing thread only.
 */
public final class TextScale
{
	static final String BASE_FONT = "gpgenius.baseFont";
	private static final String HOOKED = "gpgenius.textScaleHooked";

	/** Panel text sizes. */
	public enum Size
	{
		NORMAL("Normal", 1f),
		LARGE("Large", 1.25f),
		LARGEST("Largest", 1.5f);

		private final String label;
		public final float scale;

		Size(String label, float scale)
		{
			this.label = label;
			this.scale = scale;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	private float scale = 1f;
	private boolean applying;

	private final PropertyChangeListener fontListener = e ->
	{
		if (applying || !(e.getSource() instanceof JComponent))
		{
			return;
		}
		JComponent c = (JComponent) e.getSource();
		c.putClientProperty(BASE_FONT, e.getNewValue());
		applyOne(c);
	};

	private final ContainerListener containerListener = new ContainerListener()
	{
		@Override
		public void componentAdded(ContainerEvent e)
		{
			hook(e.getChild());
		}

		@Override
		public void componentRemoved(ContainerEvent e)
		{
		}
	};

	public float getScale()
	{
		return scale;
	}

	/** Starts following everything under the root and applies the current scale. */
	public void install(Component root)
	{
		hook(root);
	}

	public void setScale(Component root, float newScale)
	{
		float s = Math.max(0.5f, Math.min(3f, newScale));
		if (s == scale)
		{
			return;
		}
		scale = s;
		applyTree(root);
	}

	private void hook(Component c)
	{
		if (c instanceof JComponent && ((JComponent) c).getClientProperty(HOOKED) == null)
		{
			JComponent jc = (JComponent) c;
			jc.putClientProperty(HOOKED, Boolean.TRUE);
			if (scales(jc))
			{
				jc.putClientProperty(BASE_FONT, jc.getFont());
				jc.addPropertyChangeListener("font", fontListener);
			}
			if (c instanceof Container)
			{
				((Container) c).addContainerListener(containerListener);
			}
		}
		else if (c instanceof Container && !(c instanceof JComponent))
		{
			((Container) c).addContainerListener(containerListener);
		}
		if (c instanceof JComponent)
		{
			applyOne((JComponent) c);
		}
		if (c instanceof Container)
		{
			for (Component child : ((Container) c).getComponents())
			{
				hook(child);
			}
		}
	}

	private void applyTree(Component c)
	{
		if (c instanceof JComponent)
		{
			applyOne((JComponent) c);
		}
		if (c instanceof Container)
		{
			for (Component child : ((Container) c).getComponents())
			{
				applyTree(child);
			}
		}
	}

	private void applyOne(JComponent c)
	{
		Object base = c.getClientProperty(BASE_FONT);
		if (!(base instanceof Font))
		{
			return;
		}
		Font font = (Font) base;
		Font target = scale == 1f ? font : font.deriveFont(font.getSize2D() * scale);
		if (target.equals(c.getFont()))
		{
			return;
		}
		applying = true;
		try
		{
			c.setFont(target);
		}
		finally
		{
			applying = false;
		}
		c.revalidate();
	}

	/**
	 * Only components that show text. Panels are skipped: their children inherit nothing from them
	 * here, and scaling both would scale twice.
	 */
	private static boolean scales(JComponent c)
	{
		return c instanceof JLabel || c instanceof AbstractButton || c instanceof JTextComponent
			|| c instanceof JComboBox || c instanceof JList || c instanceof Pill;
	}
}
