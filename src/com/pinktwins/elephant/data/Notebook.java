package com.pinktwins.elephant.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.pinktwins.elephant.Elephant;
import com.pinktwins.elephant.data.NotebookEvent.Kind;

public class Notebook {
	private String name = "";
	private File folder;

	public boolean equals(File f) {
		return folder.equals(f);
	}

	public ArrayList<Note> notes = new ArrayList<Note>();

	public Notebook(File folder) {
		name = folder.getName();
		this.folder = folder;

		populate();
	}

	private void populate() {
		notes.clear();
		for (File f : folder.listFiles()) {
			String name = f.getName();
			if (name.charAt(0) != '.' && !name.endsWith("~")) {
				notes.add(new Note(f));
			}
		}

		sortNotes();
	}

	public void sortNotes() {
		Collections.sort(notes, new Comparator<Note>() {
			@Override
			public int compare(Note o1, Note o2) {
				return o1.lastModified() > o2.lastModified() ? -1 : 1;
			}
		});
	}

	public List<Note> getNotes() {
		return notes;
	}

	public String name() {
		return name;
	}

	public int count() {
		return notes.size();
	}

	public Note newNote() throws IOException {
		String fullPath = this.folder.getAbsolutePath() + File.separator + Long.toString(System.currentTimeMillis(), 36);
		File f = new File(fullPath);

		f.createNewFile();
		Note n = new Note(f);
		n.getMeta().title("Untitled");

		notes.add(0, n);

		Elephant.eventBus.post(new NotebookEvent(Kind.noteCreated));
		
		return n;
	}

	// XXX before moving to trash, make filename unique
	public void deleteNote(Note note) {
		notes.remove(note);
		note.moveTo(Vault.getInstance().getTrash());
	}

	public Note find(String name) {
		File note = new File(folder + File.separator + name);
		for (Note n : notes) {
			if (n.equals(note)) {
				return n;
			}
		}
		return null;
	}

	public void refresh() {
		populate();
	}
}
