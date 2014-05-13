package org.bladecoder.engineeditor.ui.scene2d;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;

import org.bladecoder.engine.actions.Param;
import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.glcanvas.FACanvas2;
import org.bladecoder.engineeditor.model.BaseDocument;
import org.bladecoder.engineeditor.model.ChapterDocument;
import org.bladecoder.engineeditor.model.Project;
import org.bladecoder.engineeditor.ui.components.scene2d.EditElementDialog;
import org.bladecoder.engineeditor.ui.components.scene2d.InputPanel;
import org.bladecoder.engineeditor.utils.EditorLogger;
import org.w3c.dom.Element;

import com.badlogic.gdx.backends.lwjgl.LwjglAWTCanvas;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;

public class EditFADialog extends EditElementDialog {
	public static final String INFO = "Define sprites and frame animations";
	
	private InputPanel[] inputs;

	InputPanel typePanel = inputs[2];

	String attrs[] = { "source", "id", "animation_type", "speed",  "delay", "count", "inD",
			"outD", "sound", "preload", "disposed_when_played"};
	
	FACanvas2 faCanvas = new FACanvas2(this);

	@SuppressWarnings("unchecked")
	public EditFADialog(Skin skin, BaseDocument doc, Element p, Element e) {
		super(skin);

		setInfo(INFO);
		
		inputs = new InputPanel[11];
		
		inputs[0] = new InputPanel(skin, "Source", "<html>Select the source where the sprite or animation is defined</html>", new String[0]);
		inputs[1] = new InputPanel(skin, "ID", "<html>Select the id of the animation</html>", new String[0]);
		inputs[2] = new InputPanel(skin, "Animation type", "<html>Select the type of the animation</html>",
							ChapterDocument.ANIMATION_TYPES);
		inputs[3] = new InputPanel(skin, "Speed", "<html>Select the speed of the animation in secods</html>",
							Param.Type.FLOAT, true);
		inputs[4] = new InputPanel(skin, "Delay", "<html>Select the delay between repeats in seconds</html>",
							Param.Type.FLOAT, false);
		inputs[5] = new InputPanel(skin, "Count", "<html>Select the repeat times</html>", Param.Type.INTEGER, false);
		inputs[6] = new InputPanel(skin, "In Dist",
							"<html>Select the distance in pixels to add to the actor position when the sprite is displayed</html>",
							Param.Type.VECTOR2, false);
		inputs[7] = new InputPanel(skin, "Out Dist",
									"<html>Select the distance in pixels to add to the actor position when the sprite is changed</html>",
									Param.Type.VECTOR2, false);				
		inputs[8] = new InputPanel(skin, "Sound",
							"<html>Select the sound ID that will be play when displayed</html>");
		inputs[9] = new InputPanel(skin, "Preload",
									"<html>Preload the animation when the scene is loaded</html>", Param.Type.BOOLEAN, true, "true", null);
		inputs[10] = new InputPanel(skin, "Dispose When Played",
											"<html>Dispose de animation when the animation is played</html>",Param.Type.BOOLEAN,  true, "false", null);							


		((SelectBox<String>) typePanel.getField()).addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				String type = typePanel.getText();

				if (type.equals("repeat") || type.equals("yoyo")) {
					inputs[4].setVisible(true);
					inputs[5].setVisible(true);
				} else {
					inputs[4].setVisible(false);
					inputs[5].setVisible(false);
				}
			}
		});
		
		((SelectBox<String>) inputs[0].getField()).addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				EditorLogger.debug("CreateEditFADialog.setSource():" +  inputs[0].getText());
				
				faCanvas.setSource(parent.getAttribute("type"), inputs[0].getText());
			}
		});
		
		((SelectBox<String>) inputs[1].getField()).addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				setCanvasFA();
			}
		});
		
		((TextField) inputs[3].getField()).addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				setCanvasFA();
			}
		});

		inputs[4].setVisible(false);
		inputs[5].setVisible(false);


		init(inputs, attrs, doc, p, "frame_animation", e);
		
		LwjglAWTCanvas canvas = new LwjglAWTCanvas(faCanvas);
		try{
			setInfoComponent(canvas.getCanvas());
		} catch(Exception ex) {
			EditorLogger.error("ERROR ADDING LIBGDX/OPENGL CANVAS");
		}
		
		addSources();
	}
	
	private void setCanvasFA() {
		String id = inputs[1].getText();
		String type = typePanel.getText();
		String speed =  inputs[3].getText();
		
		@SuppressWarnings("unchecked")
		SelectBox<String> cb = (SelectBox<String>) inputs[1].getField();

		
		if (e != null || cb.getSelectedIndex() != 0)
			faCanvas.setFrameAnimation(id, speed, type);		
	}
	
	public void fillAnimations(String []ids) {
		EditorLogger.debug("CreateEditFADialog.fillAnimations()");
		
		@SuppressWarnings("unchecked")
		SelectBox<String> cb = (SelectBox<String>) inputs[1].getField();
		cb.getItems().clear();
		
		// When creating, give option to add all elements
		if(e == null)
			cb.getItems().add("<ADD ALL>");
		
		for(String s:ids)
			cb.getItems().add(s);
		
		
		setCanvasFA();
	}
	
	String ext;

	private void addSources() {
		@SuppressWarnings("unchecked")
		SelectBox<String> cb = (SelectBox<String>) inputs[0].getField();
		String[] src = getSources();
		cb.getItems().clear();
		
		for(String s:src)
			cb.getItems().add(s);

	}
	
	
	private String[] getSources() {
		String path = null;
		String type = parent.getAttribute("type");
		
		if(type.equals(ChapterDocument.FOREGROUND_ACTOR_TYPE) || type.equals(ChapterDocument.ATLAS_ACTOR_TYPE)) {
			path = Ctx.project.getProjectPath() + Project.ATLASES_PATH + "/"
				+ Ctx.project.getResDir();
			ext = ".atlas";
		} else if(type.equals(ChapterDocument.SPRITE3D_ACTOR_TYPE)) {
			path = Ctx.project.getProjectPath() + Project.SPRITE3D_PATH;
			ext = ".g3db";
		} else if(type.equals(ChapterDocument.SPINE_ACTOR_TYPE)) {
			path = Ctx.project.getProjectPath() + Project.SPINE_PATH;
			ext = ".json";
		}
			

		File f = new File(path);

		String sources[] = f.list(new FilenameFilter() {

			@Override
			public boolean accept(File arg0, String arg1) {
				if (arg1.endsWith(ext))
					return true;

				return false;
			}
		});

		Arrays.sort(sources);
		
		for(int i=0; i < sources.length; i++)
			sources[i] = sources[i].substring(0, sources[i].length() - ext.length());

		return sources;
	}
	
	/**
	 * Override to append all animations if selected.
	 */
	@Override
	protected void ok() {
		@SuppressWarnings("unchecked")
		SelectBox<String> cb = (SelectBox<String>) inputs[1].getField();

		
		if (e == null && cb.getSelectedIndex() == 0) {
			for(int i = 1; i<cb.getItems().size; i++) {
				create();
				fill();
				doc.setId(e, cb.getItems().get(i));
			}
			
			if(listener != null)
				listener.changed(new ChangeEvent(), this);
		} else {
			super.ok();
		}
	}

}
