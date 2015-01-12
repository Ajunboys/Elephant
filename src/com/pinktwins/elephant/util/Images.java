package com.pinktwins.elephant.util;

import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import javax.imageio.ImageIO;

import org.apache.commons.io.FilenameUtils;

public class Images {

	public static Iterator<Image> iterator(String[] names) {
		ArrayList<Image> list = new ArrayList<Image>();
		for (int n = 0; n < names.length; n++) {
			Image img = null;
			try {
				img = ImageIO.read(Images.class.getClass().getResourceAsStream("/images/" + names[n] + ".png"));
			} catch (IOException e) {
				e.printStackTrace();
			}
			list.add(img);
		}
		return list.iterator();
	}

	public static boolean isImage(File f) {
		if (!f.exists()) {
			return false;
		}

		String s = FilenameUtils.getExtension(f.getName()).toLowerCase();
		return "png tif jpg jpeg bmp gif".indexOf(s) >= 0;
	}
}
