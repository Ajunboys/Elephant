package com.pinktwins.elephant;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Image;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.pinktwins.elephant.data.Vault;
import com.pinktwins.elephant.util.Images;

public class Toolbar extends BackgroundPanel {

	private static final long serialVersionUID = -8186087241529191436L;

	ElephantWindow window;

	private static Image toolbarBg, toolbarBgInactive;

	SearchTextField search;

	public static boolean skipNextFocusLost = false;
	
	static {
		Iterator<Image> i = Images.iterator(new String[]{ "toolbarBg", "toolbarBgInactive" });
		toolbarBg = i.next();
		toolbarBgInactive = i.next();
	}

	public Toolbar(ElephantWindow w) {
		super(toolbarBg);
		window = w;

		createComponents();
	}

	private void createComponents() {
		final int searchWidth = 360;

		search = new SearchTextField("Search notes");
		search.setPreferredSize(new Dimension(searchWidth, 26));
		search.setBorder(BorderFactory.createEmptyBorder(0, 22, 0, 20));
		search.setFocusable(false);

		JPanel p = new JPanel(new FlowLayout());
		p.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 10));
		p.add(search);

		add(p, BorderLayout.EAST);

		search.getDocument().addDocumentListener(new DocumentListener() {

			Timer t = new Timer();

			class PendingSearch extends TimerTask {
				private final String text;

				public PendingSearch(String text) {
					this.text = text;
				}

				@Override
				public void run() {
					EventQueue.invokeLater(new Runnable() {
						@Override
						public void run() {
							if (search.isFocusable()) {
								window.search(text);
							}
						}
					});
				}
			}

			PendingSearch pending = null;

			private void doSearch(String text) {
				if (pending != null) {
					pending.cancel();
					pending = null;
				}

				pending = new PendingSearch(text);
				t.schedule(pending, 250);
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				doSearch(search.getText());
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				doSearch(search.getText());
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				doSearch(search.getText());
			}
		});
	}

	public void focusGained() {
		setImage(toolbarBg);
		search.windowFocusGained();

		search.setVisible(Vault.getInstance().hasLocation());
	}

	public void focusLost() {
		if (skipNextFocusLost) {
			skipNextFocusLost = false;
			return;
		}

		setImage(toolbarBgInactive);
		search.windowFocusLost();
	}

	public boolean isEditing() {
		return search.isFocusOwner();
	}

	public void focusSearch() {
		search.setFocusable(true);
		search.requestFocusInWindow();
	}

	public void clearSearch() {
		search.setFocusable(false);
		search.setText("");
	}
}
