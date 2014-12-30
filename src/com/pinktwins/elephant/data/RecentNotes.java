package com.pinktwins.elephant.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.eventbus.Subscribe;
import com.pinktwins.elephant.Elephant;
import com.pinktwins.elephant.eventbus.NoteChangedEvent;
import com.pinktwins.elephant.eventbus.NotebookEvent;
import com.pinktwins.elephant.eventbus.RecentNotesChangedEvent;
import com.pinktwins.elephant.util.Factory;
import com.pinktwins.elephant.util.IOUtil;

public class RecentNotes {

	private static final int MAX_NOTES = 5;

	private ArrayList<Note> recent = Factory.newArrayList();

	public RecentNotes() {
		Elephant.eventBus.register(this);
		loadHistory();
	}

	public List<Note> list() {
		return recent;
	}

	private File historyFile() {
		return new File(Vault.getInstance().getHome() + File.separator + ".recent");
	}

	private void loadHistory() {
		JSONObject o = IOUtil.loadJson(historyFile());
		if (o.has("history")) {
			try {
				JSONArray arr = o.getJSONArray("history");
				for (int n = 0, len = arr.length(); n < len; n++) {
					String notePathHomeBased = arr.getString(n);
					File f = new File(Vault.getInstance().getHome() + File.separator + notePathHomeBased);
					Note note = new Note(f);
					recent.add(note);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}

	private void saveHistory() {
		JSONArray arr = new JSONArray();

		String home = Vault.getInstance().getHome().getAbsolutePath() + File.separator;

		for (Note note : recent) {
			String path = note.file().getAbsolutePath();
			path = path.replace(home, "");
			arr.put(path);
		}

		JSONObject o = new JSONObject();
		try {
			o.put("history", arr);
			IOUtil.writeFile(historyFile(), o.toString());
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void addRecentNote(Note n) {
		if (recent.contains(n)) {
			recent.remove(n);
		}

		recent.add(0, n);

		while (recent.size() > MAX_NOTES) {
			recent.remove(MAX_NOTES - 1);
		}
	}

	@Subscribe
	public void handleNoteChange(NoteChangedEvent event) {
		addRecentNote(event.note);

		Elephant.eventBus.post(new RecentNotesChangedEvent());
		saveHistory();
	}

	@Subscribe
	public void handleNotebookEvent(NotebookEvent event) {
		if (event.source != null && event.dest != null) {
			Note old = new Note(event.source);

			if (recent.contains(old)) {
				recent.remove(old);
			}

			addRecentNote(new Note(event.dest));

			Elephant.eventBus.post(new RecentNotesChangedEvent());
			saveHistory();
		}
	}

}
