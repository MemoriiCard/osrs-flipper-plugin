package com.gpgenius.ui;

import java.awt.Component;
import java.io.File;
import java.time.LocalDate;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

/** Where an exported trades CSV goes. */
public final class TradeDashboardFiles
{
	/** "All time" for an export from the side panel. */
	public static final int ALL_DAYS = 3650;

	private TradeDashboardFiles()
	{
	}

	/** gp-genius-trades-2026-11-22.csv */
	public static String defaultName(LocalDate day)
	{
		return "gp-genius-trades-" + day + ".csv";
	}

	/** The chosen file with ".csv" added if the player left it off. */
	public static File withCsv(File file)
	{
		if (file == null || file.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".csv"))
		{
			return file;
		}
		return new File(file.getParentFile(), file.getName() + ".csv");
	}

	/** Trades in a CSV: lines after the header, ignoring a trailing blank line. */
	public static int rows(String csv)
	{
		if (csv == null || csv.isEmpty())
		{
			return 0;
		}
		String[] lines = csv.split("\r?\n");
		int n = 0;
		for (int i = 1; i < lines.length; i++)
		{
			if (!lines[i].isEmpty())
			{
				n++;
			}
		}
		return n;
	}

	/** Asks where to save, starting in {@code dir}. Null if the player cancelled. Swing thread. */
	public static File chooseSaveFile(Component parent, File dir, LocalDate today)
	{
		if (!dir.isDirectory())
		{
			dir.mkdirs();
		}
		JFileChooser chooser = new JFileChooser(dir);
		chooser.setDialogTitle("Save your trades");
		chooser.setFileFilter(new FileNameExtensionFilter("CSV spreadsheet (*.csv)", "csv"));
		chooser.setSelectedFile(new File(dir, defaultName(today)));
		if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION)
		{
			return null;
		}
		File file = withCsv(chooser.getSelectedFile());
		if (file.exists() && JOptionPane.showConfirmDialog(parent, file.getName() + " already exists. Replace it?",
			"Save your trades", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
		{
			return null;
		}
		return file;
	}
}
