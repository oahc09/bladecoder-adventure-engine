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
package com.bladecoder.engine.model;

import java.util.HashMap;

import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.assets.AssetConsumer;
import com.bladecoder.engine.assets.EngineAssetManager;

/**
 * A BaseActor is any object in a scene or in the inventory.
 * 
 * @author rgarcia
 */
public class BaseActor implements Comparable<BaseActor>, Serializable, AssetConsumer {
	protected String id;
	protected String desc;
	protected Scene scene = null;
	protected float zIndex;
	
	/** visibility and interaction activation */
	private boolean interaction = true;
	private boolean visible = true;

	/** internal state. Can be used for actions to maintain a state machine */
	protected String state;

	protected VerbManager verbs = new VerbManager();
	protected HashMap<String, SoundFX> sounds;
	protected HashMap<String, String> customProperties;
	
	private String playingSound;

	protected Polygon bbox;
	
	private HashMap<String, Dialog> dialogs;
	
	private boolean isWalkObstacle = false;
	
	private String layer;
	
	/** State to know when the player is inside this actor to trigger the enter/exit verbs */ 
	private boolean playerInside = false;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Polygon getBBox() {
		return bbox;
	}
	
	public void setLayer(String layer) {
		this.layer = layer;
	}
	
	public String getLayer() {
		return layer;
	}
	
	public boolean hit(float x, float y) {
		return getBBox().contains(x, y);
	}

	public boolean hasInteraction() {
		return interaction && visible;
	}

	public void setInteraction(boolean interaction) {
		this.interaction = interaction;
	}

	public boolean isVisible() {			
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
		
		if(isWalkObstacle() && scene!= null && scene.getPolygonalNavGraph() != null) {
			if(visible)
				scene.getPolygonalNavGraph().addDinamicObstacle(getBBox());
			else
				scene.getPolygonalNavGraph().removeDinamicObstacle(getBBox());
		}
	}

	public void setBbox(Polygon bbox) {
		this.bbox = bbox;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public VerbManager getVerbManager() {
		return verbs;
	}
	
	public void setScene(Scene s) {
		scene = s;
	}
	
	public Scene getScene() {
		return scene;
	}
	
	public void update(float delta) {
		BaseActor player = scene.getPlayer();
		if(isVisible() && player != null) {
			boolean hit = hit(player.getX(), player.getY());
			if(!hit && playerInside) {
				// the player leaves
				playerInside = false;
				
				Verb v = getVerb("exit");
				if(v!=null)
					v.run();
			} else if(hit && !playerInside){
				// the player enters
				playerInside = true;
				
				Verb v = getVerb("enter");
				if(v!=null)
					v.run();				
			}
		}
	}

	public Verb getVerb(String id) {
		return verbs.getVerb(id, state, null);
	}

	public Verb getVerb(String id, String target) {
		return verbs.getVerb(id, state, target);
	}
	
	public void runVerb(String id) {
		verbs.runVerb(id, state, null);
	}
	
	public void runVerb(String id, String target) {
		verbs.runVerb(id, state, target);
	}

	public void addSound(String id, String filename, boolean loop, float volume) {
		if (sounds == null)
			sounds = new HashMap<String, SoundFX>();

		sounds.put(id, new SoundFX(filename, loop, volume));
	}

	public void playSound(String id) {
		if(sounds == null) return;
		
		SoundFX s = sounds.get(id);

		if (s != null) {
			if(playingSound != null) {
				SoundFX s2 = sounds.get(playingSound);
				s2.stop();
			}
			
			s.play();
			playingSound = id;
		}
	}
	
	public void stopSound(String id) {
		if(sounds == null) return;
		
		SoundFX s = sounds.get(id);

		if (s != null) {
			s.stop();
		}
		
		playingSound = null;
	}
	
	public void setCustomProperty(String name, String value) {
		if(customProperties == null)
			customProperties = new HashMap<String, String>();
		
		customProperties.put(name, value);
	}
	
	public String getCustomProperty(String name) {
		return customProperties.get(name);
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();

		sb.append("\nObject: ").append(id).append(", state: ").append(state);
		sb.append("\n  Desc: ").append(desc);
		sb.append("\n  BBox: ").append(getBBox().toString());
		sb.append("\n  Verbs:");

		for (String v : verbs.getVerbs().keySet()) {
			sb.append(" ").append(v);
		}

		sb.append("\n");

		return sb.toString();
	}

	@Override
	public int compareTo(BaseActor o) {
		return (int) (o.getBBox().getY() - this.getBBox().getY());
	}
	
	public float getZIndex() {
		return zIndex;
	}
	
	public void setZIndex(float z) {
		zIndex = z;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}
	
	public Dialog getDialog(String dialog) {
		return dialogs.get(dialog);
	}
	
	public void addDialog(String id, Dialog d) {
		if(dialogs == null)
			dialogs = new HashMap<String, Dialog> ();
		
		dialogs.put(id, d);
	}
	
	public float getX() {
		return bbox.getX();
	}
	
	public float getY() {
		return bbox.getY();
	}
	
	public boolean isWalkObstacle() {
		return isWalkObstacle;
	}

	public void setWalkObstacle(boolean isWalkObstacle) {
		this.isWalkObstacle = isWalkObstacle;
	}

	public void setPosition(float x, float y) {
		boolean inNavGraph = false;
		
		if(isWalkObstacle() && scene != null && scene.getPolygonalNavGraph() != null) {
			inNavGraph = scene.getPolygonalNavGraph().removeDinamicObstacle(bbox);
		}
		
		bbox.setPosition(x, y);
		
		if(inNavGraph) {
			scene.getPolygonalNavGraph().addDinamicObstacle(bbox);
		}
	}
	
	@Override
	public void loadAssets() {
		if (sounds != null) {
			for (SoundFX s : sounds.values()) {
				s.loadAssets();
			}
		}
	}

	@Override
	public void retrieveAssets() {
		if (sounds != null) {
			for (SoundFX s : sounds.values()) {
				s.retrieveAssets();
			}
			
			if(playingSound != null && sounds.get(playingSound).isLooping() == true) {
				playSound(playingSound);
			}
		}
	}	

	@Override
	public void dispose() {
		if (sounds != null) {
			for (SoundFX s : sounds.values()) {
				s.dispose();
			}

			sounds.clear();
			sounds = null;
			playingSound = null;
		}
	}

	@Override
	public void write(Json json) {
		json.writeValue("id", id);
		json.writeValue("interaction", interaction);
		json.writeValue("visible", visible);
		json.writeValue("desc", desc);
		json.writeValue("verbs", verbs);

		float worldScale = EngineAssetManager.getInstance().getScale();
		Vector2 scaledPos = new Vector2(bbox.getX() / worldScale, bbox.getY() / worldScale);
		json.writeValue("pos", scaledPos);	
		json.writeValue("bbox", bbox.getVertices());
		json.writeValue("state", state);
		json.writeValue("sounds", sounds, sounds == null ? null : sounds.getClass(), SoundFX.class);
		json.writeValue("playingSound", playingSound, playingSound == null ? null : playingSound.getClass());
		
		json.writeValue("customProperties", customProperties, customProperties == null ? null : customProperties.getClass(), String.class);
		json.writeValue("dialogs", dialogs, dialogs == null ? null : dialogs.getClass(), Dialog.class);
		
		json.writeValue("isWalkObstacle", isWalkObstacle);
		json.writeValue("layer", layer);
		json.writeValue("playerInside", playerInside);
		json.writeValue("zIndex", zIndex);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void read (Json json, JsonValue jsonData) {
		id = json.readValue("id", String.class, jsonData);
		interaction = json.readValue("interaction", Boolean.class, jsonData);
		visible = json.readValue("visible", Boolean.class, jsonData);
		desc = json.readValue("desc", String.class, jsonData);
		verbs = json.readValue("verbs", VerbManager.class, jsonData);

		Vector2 pos = json.readValue("pos", Vector2.class, jsonData);

		float worldScale = EngineAssetManager.getInstance().getScale();
		bbox = new Polygon();
		bbox.setPosition(pos.x * worldScale, pos.y * worldScale);
		
		float[] verts = json.readValue("bbox", float[].class, jsonData);
		
		if(verts.length > 0)
			bbox.setVertices(verts);
		
		bbox.setScale(worldScale, worldScale);
		
		state = json.readValue("state", String.class, jsonData);
		sounds = json.readValue("sounds", HashMap.class, SoundFX.class, jsonData);
		playingSound = json.readValue("playingSound", String.class, jsonData);
		customProperties = json.readValue("customProperties", HashMap.class, String.class, jsonData);
		dialogs = json.readValue("dialogs", HashMap.class, Dialog.class, jsonData);
		
		isWalkObstacle = json.readValue("isWalkObstacle", Boolean.class, jsonData);
		layer = json.readValue("layer", String.class, jsonData);
		playerInside = json.readValue("playerInside", Boolean.class, jsonData);
		zIndex = json.readValue("zIndex", Float.class, jsonData);
	}

}
