package com.pinktwins.elephant;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.SwingWorker;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import com.pinktwins.elephant.NoteEditor.EditorController;
import com.pinktwins.elephant.util.CustomMouseListener;
import com.pinktwins.elephant.util.Factory;
import com.pinktwins.elephant.util.Images;
import com.pinktwins.elephant.util.PdfUtil;

public class FileAttachment extends JPanel {
	private static final long serialVersionUID = 5444731416148596756L;

	private static JFileChooser chooser = new JFileChooser();

	private static Image quickLook, openFolder;
	private static boolean qlExists;
	private static String qlPath = "/usr/bin/qlmanage";

	interface PreviewPageProvider {
		Image getPage();

		File getFile();
	}

	static {
		Iterator<Image> i = Images.iterator(new String[] { "quickLook", "openFolder" });
		quickLook = i.next();
		openFolder = i.next();

		qlExists = new File(qlPath).exists();
	}

	private JPanel iconArea, left, text, right;
	private JLabel label, size;
	private JButton icon, show, open;
	private ImageScaler scaler;
	private EditorController editor;
	private String labelStr, sizeStr;

	public FileAttachment(final File f, ImageScaler scaler, EditorController editor) {
		super();

		this.scaler = scaler;
		this.editor = editor;

		labelStr = f.getName();
		long fileLen = 0;

		if (f.exists()) {
			fileLen = f.length();
		}

		setLayout(new BorderLayout());
		setBackground(Color.WHITE);
		setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));

		iconArea = new JPanel(new BorderLayout());
		iconArea.setBackground(Color.WHITE);

		left = new JPanel(new FlowLayout(FlowLayout.LEFT));
		left.setOpaque(false);

		text = new JPanel(new BorderLayout());
		text.setOpaque(false);

		right = new JPanel(new FlowLayout());
		right.setOpaque(false);

		label = new JLabel(labelStr);
		label.setFont(ElephantWindow.fontBoldEditor);
		label.setForeground(ElephantWindow.colorGray5);

		sizeStr = FileUtils.byteCountToDisplaySize(fileLen);
		size = new JLabel(sizeStr);
		size.setFont(ElephantWindow.fontMedium);
		size.setForeground(Color.DARK_GRAY);

		icon = new JButton("");
		Icon i = chooser.getIcon(f);
		BufferedImage image = new BufferedImage(i.getIconWidth(), i.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
		i.paintIcon(new JPanel(), image.getGraphics(), 0, 0);
		Image scaled = image.getScaledInstance(32, 32, Image.SCALE_SMOOTH);
		icon.setIcon(new ImageIcon(scaled));
		icon.setBorder(ElephantWindow.emptyBorder);
		icon.setBorderPainted(false);

		show = new JButton("");
		show.setIcon(new ImageIcon(quickLook));
		show.setBorderPainted(false);
		show.setContentAreaFilled(false);
		show.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 4));

		open = new JButton("");
		open.setIcon(new ImageIcon(openFolder));
		open.setBorderPainted(false);
		open.setContentAreaFilled(false);
		open.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 14));

		text.add(label, BorderLayout.NORTH);
		text.add(size, BorderLayout.CENTER);
		left.add(icon);
		left.add(text);
		if (qlExists) {
			right.add(show);
		} else {
			open.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
		}
		right.add(open);

		iconArea.add(left, BorderLayout.WEST);
		iconArea.add(right, BorderLayout.EAST);

		add(iconArea, BorderLayout.NORTH);

		setMaximumSize(new Dimension(294, 38));

		addPreview(f);

		icon.addMouseListener(new CustomMouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					try {
						java.awt.Desktop.getDesktop().open(f);
					} catch (IOException e1) {
						e1.printStackTrace();
					}

				}
			}
		});

		show.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (qlExists) {
					try {
						String[] cmd = new String[3];
						cmd[0] = qlPath;
						cmd[1] = "-p";
						cmd[2] = f.getAbsolutePath();

						Runtime.getRuntime().exec(cmd, new String[0], null);
					} catch (IOException e2) {
						e2.printStackTrace();
					}
				} else {
					try {
						java.awt.Desktop.getDesktop().open(f.getParentFile());
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
		});

		open.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					java.awt.Desktop.getDesktop().open(f.getParentFile());
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
	}

	long loadingStartTs;

	private void updateInfoStr(String info) {
		if (System.currentTimeMillis() - loadingStartTs > 1000) {
			size.setText(sizeStr + info);
		}
	}

	static public File getPreviewDirectory(File f) {
		String path = ImageScalingCache.getImageCacheDir() + File.separator + f.getName();
		path += "_" + f.length() + ".preview";
		return new File(path);
	}

	// get file for page#. arg File F is a directory previously got with
	// getPreviewDirectory()
	static public File getPreviewFileForPage(File f, int page) {
		return new File(String.format("%s%spage_%05d.png", f.getAbsolutePath(), File.separator, page));
	}

	class FilePageProvider implements PreviewPageProvider {
		File page;

		public FilePageProvider(File page) {
			this.page = page;
		}

		@Override
		public Image getPage() {
			try {
				Image img = null;
				try {
					img = ImageIO.read(page);
				} catch (IndexOutOfBoundsException e) {
					// XXX suspect method of resilience
					try {
						Thread.sleep(100);
					} catch (InterruptedException e1) {
					}
					try {
						img = ImageIO.read(page);
					} catch (IndexOutOfBoundsException e2) {
						System.out.println("Failed reading pdf page file " + page.getAbsolutePath());
					}
				}
				if (img != null) {
					img = scaler.scale(img, page);
				}
				return img;
			} catch (IOException e) {
				e.printStackTrace();
			} catch (IndexOutOfBoundsException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		public File getFile() {
			return page;
		}
	}

	class PdfPageProvider implements PreviewPageProvider {
		PdfUtil pdf;
		int page;
		File outPath;

		public PdfPageProvider(PdfUtil pdf, int page, File outPath) {
			this.pdf = pdf;
			this.page = page;
			this.outPath = outPath;
		}

		@Override
		public Image getPage() {
			Image img = pdf.writePage(page, outPath);
			if (img != null) {
				img = scaler.scale(img, outPath);
			}
			return img;
		}

		@Override
		public File getFile() {
			getPage();
			return outPath;
		}
	}

	class PdfHolder {
		PdfUtil pdf;
	}

	private List<PreviewPageProvider> getPreviewPages(File f, PdfHolder pdfHolder) {
		ArrayList<PreviewPageProvider> pages = Factory.newArrayList();

		int pagesAdded = 0;
		File[] files = previewFiles(f);
		if (files.length > 0) {
			for (File file : files) {

				// If this is not the real next page (page missing in the middle), ignore remaining files and rerender.
				String s = file.getName().replace("page_", "");
				s = s.replace("." + FilenameUtils.getExtension(s), "");
				int n = Integer.parseInt(s);

				if (n - 1 != pagesAdded) {
					break;
				}

				pages.add(new FilePageProvider(file));
				pagesAdded++;
			}
		}

		int gotPages = pages.size();

		if (FilenameUtils.getExtension(f.getName()).toLowerCase().equals("pdf")) {
			PdfUtil pdf = new PdfUtil(f);

			if (pdf.numPages() > gotPages) {
				File outPath = getPreviewDirectory(f);
				outPath.mkdirs();
				if (outPath.exists()) {
					for (int n = gotPages; n < pdf.numPages(); n++) {
						pages.add(new PdfPageProvider(pdf, n + 1, getPreviewFileForPage(outPath, n + 1)));
					}
				}
			}

			pdfHolder.pdf = pdf;
		}

		return pages;
	}

	private void addPreview(File f) {

		final PdfHolder pdfHolder = new PdfHolder();
		final List<PreviewPageProvider> pages = getPreviewPages(f, pdfHolder);

		if (pages.size() > 0) {
			final JTextPane tp = new JTextPane();
			tp.setBackground(Color.WHITE);
			tp.setOpaque(true);
			tp.setFocusable(false);

			final Style style = tp.addStyle("nada", null);
			StyleConstants.setFontSize(style, 0);

			final int noteHash = editor.noteHash();

			class Workers {
				private final ArrayList<SwingWorker<Image, Void>> workers = Factory.newArrayList();

				public void add(SwingWorker<Image, Void> w) {
					workers.add(w);
				}

				public void next() {
					if (workers.size() > 0) {
						// editor.lockScrolling(true);
						SwingWorker<Image, Void> w = workers.get(0);
						workers.remove(0);
						w.execute();
					}
				}

				public void last() {
					if (workers.size() > 0) {
						SwingWorker<Image, Void> w = workers.get(workers.size() - 1);
						workers.clear();
						w.execute();
					}
				}

				public boolean isEmpty() {
					return workers.isEmpty();
				}

				public int size() {
					return workers.size();
				}
			}

			// pageIcons - one icon per page
			final ArrayList<ImageIcon> pageIcons = Factory.newArrayList();
			Image page1 = pages.get(0).getPage();
			int w = page1.getWidth(null);
			int h = page1.getHeight(null);

			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			Image blank = null;

			GraphicsDevice gs = null;
			GraphicsConfiguration gc = null;
			try {
				gs = ge.getDefaultScreenDevice();
				gc = gs.getDefaultConfiguration();
				blank = gc.createCompatibleImage(w, h, Transparency.BITMASK);
			} catch (HeadlessException e) {
			}

			// Insert blank pages as placeholders
			editor.scrollTo(0);

			int pageNum = 0;
			for (@SuppressWarnings("unused")
			PreviewPageProvider ppp : pages) {
				pageNum++;

				/*
				 * If pdf, check if aspect ratio changed. Must add a placeholder with correct aspect ratio, or cropping
				 * will occur.
				 */
				if (pdfHolder.pdf != null) {
					Dimension d = pdfHolder.pdf.pageSize(pageNum);
					if (d.width * 100 / d.height != w * 100 / h) {
						blank = gc.createCompatibleImage(d.width, d.height, Transparency.BITMASK);
						blank = scaler.scale(blank, new File(ImageScalingCache.getImageCacheDir() + File.separator + "blank.png"));
						w = blank.getWidth(null);
						h = blank.getHeight(null);
					}
				}

				ImageIcon ii = new ImageIcon(blank);
				pageIcons.add(ii);

				addPageBreak(tp, style);
				tp.insertIcon(ii);
				try {
					tp.getDocument().insertString(tp.getCaretPosition(), "\n", style);
				} catch (BadLocationException e) {
					e.printStackTrace();
				}
			}

			final Workers workers = new Workers();

			for (PreviewPageProvider ppp : pages) {
				final PreviewPageProvider page = ppp;

				workers.add(new SwingWorker<Image, Void>() {
					@Override
					protected Image doInBackground() throws Exception {
						return page.getPage();
					}

					@Override
					protected void done() {
						try {
							Image img = get();
							if (img != null) {
								int num = pages.size() - workers.size();

								if (num >= 0 && num < pageIcons.size()) {
									pageIcons.get(num).setImage(img);
									tp.repaint();
									// editor.lockScrolling(false);
								}
							}

							int f = (int) ((1.0f - workers.size() / (float) (pages.size())) * 100f);
							String s = "   " + ProgressBars.getCharacterBar(f);

							updateInfoStr(s);

							// abort if editor has changed note
							if (noteHash == editor.noteHash()) {
								workers.next();
							} else {
								workers.last();
							}
						} catch (ExecutionException e) {
							e.printStackTrace();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}

					}
				});
			}

			if (!workers.isEmpty()) {
				// One more worker to mark done + cleanup
				workers.add(new SwingWorker<Image, Void>() {
					@Override
					protected Image doInBackground() throws Exception {
						return null;
					}

					@Override
					protected void done() {
						// editor.lockScrolling(false);
						updateInfoStr("");
						if (pdfHolder.pdf != null) {
							pdfHolder.pdf.close();
						}
					}
				});

				loadingStartTs = System.currentTimeMillis();
				workers.next();
			}

			add(tp, BorderLayout.CENTER);
		}
	}

	static public File[] previewFiles(File f) {
		File pf = getPreviewDirectory(f);
		if (pf.exists() && pf.isDirectory()) {
			File[] files = pf.listFiles(new FileFilter() {
				@Override
				public boolean accept(File pathname) {
					String ext = FilenameUtils.getExtension(pathname.getName()).toLowerCase();
					return ("jpg".equals(ext) || "jpeg".equals(ext) || "png".equals(ext) || "gif".equals(ext));
				}
			});
			Arrays.sort(files);
			return files;
		} else {
			return new File[0];
		}
	}

	final static Color pageBreakColor = Color.decode("#c0c0c0");

	void addPageBreak(JTextPane tp, Style style) {
		JPanel p = new JPanel(null);
		p.setBounds(0, 0, ElephantWindow.bigWidth, 1);
		p.setBackground(pageBreakColor);
		p.setBorder(ElephantWindow.emptyBorder);
		tp.insertComponent(p);

		try {
			tp.getDocument().insertString(tp.getCaretPosition(), "\n", style);
		} catch (BadLocationException e1) {
			e1.printStackTrace();
		}
	}

	public void focusQuickLook() {
		if (qlExists) {
			show.requestFocusInWindow();
		}
	}
}
