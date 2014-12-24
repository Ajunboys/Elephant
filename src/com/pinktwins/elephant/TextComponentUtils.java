package com.pinktwins.elephant;

import java.awt.EventQueue;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

public class TextComponentUtils {

	public static void insertListenerForHintText(final JTextComponent text, final String hintText) {

		// When inserting first character, remove 'hintText' from document
		text.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				if (text.getCaretPosition() == 0) {
					String s = text.getText();
					if (s.length() == hintText.length() + 1 && s.indexOf(hintText) == 1) {
						EventQueue.invokeLater(new Runnable() {
							@Override
							public void run() {
								try {
									text.getDocument().remove(1, hintText.length());
								} catch (BadLocationException e1) {
									e1.printStackTrace();
								}
							}
						});
					}
				}
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
			}
		});
	}
}
