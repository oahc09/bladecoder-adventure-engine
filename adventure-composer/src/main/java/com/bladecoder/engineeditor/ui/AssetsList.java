/*******************************************************************************
 * Copyright 2014 Rafael Garcia Moreno.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.bladecoder.engineeditor.ui;

import java.awt.Desktop;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.model.Project;
import com.bladecoder.engineeditor.ui.components.CustomList;
import com.bladecoder.engineeditor.ui.components.EditToolbar;
import com.bladecoder.engineeditor.utils.ImageUtils;

public class AssetsList extends Table {
	private static final String[] ASSET_TYPES = { "3d models", 
			"atlases", "music", "sounds", "images", "spine" };

	private SelectBox<String> assetTypes;
	protected EditToolbar toolbar;
    protected CustomList<String> list;
    protected Skin skin;
    protected Container<ScrollPane> container;

	private File lastDir;

	public AssetsList(Skin skin) {
		super(skin);
		
		assetTypes = new SelectBox<String>(skin);
		assetTypes.setItems(ASSET_TYPES);
		
		this.skin = skin;	
		
		list = new CustomList<String>(skin);
		
		Array<String> items = new Array<String>();
		list.setItems(items);
			
		ScrollPane scrollPane = new ScrollPane(list, skin);
		container = new Container<ScrollPane>(scrollPane);
		container.fill();
		container.prefHeight(1000);
		
		toolbar = new EditToolbar(skin);
//		debug();
		add(assetTypes).expandX().fillX();
		row();
		add(toolbar).expandX().fillX();
		row();
		add(container).expand().fill();
		
		toolbar.addCreateListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				create();
			}
		});
		
		toolbar.addEditListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				edit();
			}
		});
		
		toolbar.addDeleteListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				delete();
			}
		});
				

		list.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				toolbar.disableEdit(false);
			}
		});


		Ctx.project.addPropertyChangeListener(Project.NOTIFY_PROJECT_LOADED,
				new PropertyChangeListener() {
					@Override
					public void propertyChange(PropertyChangeEvent arg0) {
						toolbar.disableCreate(Ctx.project.getProjectDir() == null);
						addAssets();
					}
				});

		assetTypes.addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				addAssets();
			}
		});
	}

	private void addAssets() {
		list.getItems().clear();

		if (Ctx.project.getProjectDir() != null) {
			String type = assetTypes.getSelected();
			String dir = getAssetDir(type);

			if (type.equals("images") || type.equals("atlases"))
				dir += "/1";

			String[] files = new File(dir).list(new FilenameFilter() {
				@Override
				public boolean accept(File arg0, String arg1) {
					String type = assetTypes.getSelected();

					if (type.equals("atlases") && !arg1.endsWith(".atlas"))
						return false;

					return true;
				}
			});

			if (files != null)
				for (String f : files)
					list.getItems().add(f);

			if (list.getItems().size > 0) {
				list.setSelectedIndex(0);
			}
		}

		toolbar.disableCreate(Ctx.project.getProjectDir() == null);
		list.invalidateHierarchy();
	}

	private String getAssetDir(String type) {
		String dir;

		if (type.equals("atlases")) {
			dir = Ctx.project.getProjectPath() + "/" + Project.ATLASES_PATH;
		} else if (type.equals("music")) {
			dir = Ctx.project.getProjectPath() + "/" + Project.MUSIC_PATH;
		} else if (type.equals("sounds")) {
			dir = Ctx.project.getProjectPath() + "/" + Project.SOUND_PATH;
		} else if (type.equals("images")) {
			dir = Ctx.project.getProjectPath() + "/" + Project.IMAGE_PATH;
		} else if (type.equals("3d models")) {
			dir = Ctx.project.getProjectPath() + "/" + Project.SPRITE3D_PATH;
		} else if (type.equals("spine")) {
			dir = Ctx.project.getProjectPath() + "/" + Project.SPINE_PATH;			
		} else {
			dir = Ctx.project.getProjectPath() + Project.ASSETS_PATH;
		}

		return dir;
	}

	private void create() {
		String type = assetTypes.getSelected();

		if (type.equals("atlases")) {
			new CreateAtlasDialog(skin).setVisible(true);
		} else {

			JFileChooser chooser = new JFileChooser(lastDir);
			chooser.setDialogTitle("Select the '" + type + "' asset files");
			chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			chooser.setMultiSelectionEnabled(true);

			FileNameExtensionFilter filter = null;

			if (type.equals("images"))
				filter = new FileNameExtensionFilter("Images", "jpg", "png",
						"etc1");
			else if (type.equals("music") || type.equals("sounds"))
				filter = new FileNameExtensionFilter("Music", "wav", "mp3",
						"ogg");
			else if (type.equals("3d models"))
				filter = new FileNameExtensionFilter("3D Models", "g3db", "png");
			else if (type.equals("spine"))
				filter = new FileNameExtensionFilter("Spine", "skel", "json");			

			chooser.removeChoosableFileFilter(chooser.getChoosableFileFilters()[0]);
			chooser.addChoosableFileFilter(filter);

			if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
				try {
					File[] files = chooser.getSelectedFiles();
					String dir = getAssetDir(type);
					lastDir = chooser.getSelectedFile();

					for (File f : files) {
						if ( type.equals("images")) {
							List<String> res = Ctx.project.getResolutions();

							for (String r : res) {
								File destFile = new File(dir + "/" + r
										+ "/" + f.getName());
								float scale = Float.parseFloat(r);

								if (scale != 1.0f) {

									ImageUtils.scaleImageFile(f, destFile,
											scale);
								} else {
									Files.copy(f.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
								}
							}
						} else {
							File destFile = new File(dir + "/" + f.getName());
							Files.copy(f.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
						}
						
					}

				} catch (Exception ex) {
					String msg = "Something went wrong while getting the assets.\n\n"
							+ ex.getClass().getSimpleName()
							+ " - "
							+ ex.getMessage();
					Ctx.msg.show(getStage(), msg, 4);
					ex.printStackTrace();
				}
			}
		}
			
		addAssets();
	}

	private void edit() {
		if (Desktop.isDesktopSupported()) {
			try {
				Desktop.getDesktop().open(
						new File(Ctx.project.getProjectDir().getAbsoluteFile()
								+ "/assets"));
			} catch (IOException e1) {
				String msg = "Something went wrong while opening assets folder.\n\n"
						+ e1.getClass().getSimpleName()
						+ " - "
						+ e1.getMessage();
				Ctx.msg.show(getStage(), msg, 4);
			}
		}
	}

	private void delete() {
		String type =  assetTypes.getSelected();
		String dir = getAssetDir(type);

		String name = list.getSelected();
		try {
			if (type.equals("images")
					|| type.equals("atlases")) {
				List<String> res = Ctx.project.getResolutions();

				for (String r : res) {
					File file = new File(dir + "/" + r + "/" + name);

					file.delete();

					// delete pages on atlases
					if (type.equals("atlases")) {
						File atlasDir = new File(dir + "/" + r);

						File[] files = atlasDir.listFiles();

						if (files != null)
							for (File f : files) {
								String destName = f.getName();
								String nameWithoutExt = name.substring(0,
										name.lastIndexOf('.'));
								String destNameWithoutExt = destName.substring(0,
										destName.lastIndexOf('.'));
								
								if(destNameWithoutExt.length() < nameWithoutExt.length())
									continue;
								
								String suffix = destNameWithoutExt.substring( nameWithoutExt.length());
								
								if(!suffix.isEmpty() && !suffix.matches("[0-9]+"))
									continue;

								if (destName.startsWith(nameWithoutExt)
										&& destName.toLowerCase().endsWith(".png"))
									Files.delete(f.toPath());
							}
					}
				}
			} else {
				File file = new File(dir + "/" + name);
				file.delete();
			}

			addAssets();
		} catch (Exception ex) {
			String msg = "Something went wrong while deleting the asset.\n\n"
					+ ex.getClass().getSimpleName() + " - " + ex.getMessage();
			Ctx.msg.show(getStage(), msg, 4);
			ex.printStackTrace();
		}
	}
}
