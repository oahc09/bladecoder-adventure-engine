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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.assets.AssetConsumer;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.pathfinder.NavNode;
import com.bladecoder.engine.polygonalpathfinder.NavNodePolygonal;
import com.bladecoder.engine.polygonalpathfinder.PolygonalNavGraph;
import com.bladecoder.engine.util.EngineLogger;

public class Scene implements Serializable,
		AssetConsumer {
	
	public static final Color ACTOR_BBOX_COLOR = new Color(0.2f, 0.2f, 0.8f,
			1f);
	public static final Color WALKZONE_COLOR = Color.GREEN;
	public static final Color OBSTACLE_COLOR = Color.RED;

	/** 
	 * All actors in the scene
	 */
	private HashMap<String, BaseActor> actors = new HashMap<String, BaseActor>();

	/**
	 * BaseActor layers
	 */
	private List<SceneLayer> layers = new ArrayList<SceneLayer>();
	
	private SceneCamera camera = new SceneCamera();
	
	private Array<AtlasRegion> background;
	private Array<AtlasRegion> lightMap;
	private String backgroundAtlas;
	private String backgroundRegionId;
	private String lightMapAtlas;
	private String lightMapRegionId;
	
	/** For polygonal PathFinding */
	private PolygonalNavGraph polygonalNavGraph;
	
	/** depth vector. X: the actor 'y' position for a 0.0 scale, Y: the actor 'y' position for a 1.0 scale. */
	private Vector2 depthVector;

	private String player;
	
	/** The actor the camera will follow */
	private SpriteActor followActor;

	private Music music = null;
	private boolean loopMusic = false;
	private float repeatMusicDelay = 0;
	private float initialMusicDelay = 0;
	private float currentMusicDelay = 0;

	private String musicFilename;
	private boolean isPlayingSer = false;
	private float musicPosSer = 0;

	transient private boolean isMusicPaused = false;
	
	private String id;
	
	/** internal state. Can be used for actions to maintain a state machine */
	private String state;
	
	private VerbManager verbs = new VerbManager();
	
	/**
	 * Add support for the use of global custom properties/variables in the game
	 * logic
	 */
	private HashMap<String, String> customProperties;

	public Scene() {	
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public String getState() {
		return state;
	}
	
	public void setState(String s) {
		state = s;
	}
	
	public List<SceneLayer> getLayers() {
		return layers;
	}
	
	public SceneLayer getLayer(String name) {
		for(SceneLayer l: layers) {
			if(name.equals(l.getName()))
				return l;
		}
		
		return null;
	}
	
	public void addLayer(SceneLayer layer) {
		layers.add(layer);
	}

	
	public void setCustomProperty(String name, String value) {
		if(customProperties == null)
			customProperties = new HashMap<String, String>();
		
		customProperties.put(name, value);
	}
	
	public String getCustomProperty(String name) {
		return customProperties.get(name);
	}

	public void playMusic() {
		if (music != null && !music.isPlaying()) {
			music.play();
			music.setLooping(loopMusic);
		}
	}

	public void pauseMusic() {
		if (music != null && music.isPlaying()) {
			music.pause();
			isMusicPaused = true;
		}
	}

	public void resumeMusic() {
		if (music != null && isMusicPaused) {
			music.play();
			isMusicPaused = false;
		}
	}

	public void stopMusic() {
		if (music != null)
			music.stop();
	}
	
	public float getFakeDepthScale(float y) {
		if(depthVector==null)
			return 1.0f;
		
		float worldScale = EngineAssetManager.getInstance().getScale();
		
		return Math.max(0, (y - depthVector.x * worldScale) / ((depthVector.y - depthVector.x) * worldScale));
	}

	public void setMusic(String filename, boolean loop, float initialDelay,
			float repeatDelay) {
		loopMusic = loop;
		musicFilename = filename;
		initialMusicDelay = initialDelay;
		repeatMusicDelay = repeatDelay;
	}
	
	public VerbManager getVerbManager() {
		return verbs;
	}

	public Verb getVerb(String id) {
		return verbs.getVerb(id, state, null);
	}
	
	public void runVerb(String id) {
		verbs.runVerb(id, state, null);
	}

	public void update(float delta) {
		// We draw the elements in order: from top to bottom.
		// so we need to order the array list
		for(SceneLayer layer:layers)
			layer.update();

		// music delay update
		if (music != null && !music.isPlaying()) {
			boolean initialTime = false;

			if (currentMusicDelay <= initialMusicDelay)
				initialTime = true;

			currentMusicDelay += delta;

			if (initialTime) {
				if (currentMusicDelay > initialMusicDelay)
					playMusic();
			} else {
				if (repeatMusicDelay >= 0
						&& currentMusicDelay > repeatMusicDelay
								+ initialMusicDelay) {
					currentMusicDelay = initialMusicDelay;
					playMusic();
				}
			}
		}

		for (BaseActor a:actors.values()) {
			a.update(delta);
		}
		
		camera.update(delta);
		
		if(followActor != null) {
			camera.updatePos(followActor);
		}
	}

	public void draw(SpriteBatch spriteBatch) {
		
		if (background != null) {
			spriteBatch.disableBlending();

			float x = 0;

			for (AtlasRegion tile : background) {
				spriteBatch.draw(tile, x, 0f);
				x += tile.getRegionWidth();
			}

			spriteBatch.enableBlending();
		}
		
		// draw layers from bottom to top
		for(int i = layers.size() - 1; i >= 0; i--) {
			SceneLayer layer = layers.get(i);
			layer.draw(spriteBatch);			
		}

		// Draw the light map
		if (lightMap != null) {
			// Multiplicative blending for light maps
			spriteBatch.setBlendFunction(GL20.GL_DST_COLOR, GL20.GL_ZERO);

			float x = 0;

			for (AtlasRegion tile : lightMap) {
				spriteBatch.draw(tile, x, 0f);
				x += tile.getRegionWidth();
			}

			spriteBatch.setBlendFunction(GL20.GL_SRC_ALPHA,
					GL20.GL_ONE_MINUS_SRC_ALPHA);
		}
	}

	public void drawBBoxLines(ShapeRenderer renderer) {
		// renderer.begin(ShapeType.Rectangle);
		renderer.begin(ShapeType.Line);
		renderer.setColor(ACTOR_BBOX_COLOR);

		for (BaseActor a : actors.values()) {
			Polygon p = a.getBBox();

			if (p == null) {
				EngineLogger.error("ERROR DRAWING BBOX FOR: " + a.getId());
			}
			
			Rectangle r = a.getBBox().getBoundingRectangle();

			renderer.polygon(p.getTransformedVertices());
			renderer.rect(r.getX(), r.getY(), r.getWidth(), r.getHeight());
		}
		
		if(polygonalNavGraph != null) {
			renderer.setColor(WALKZONE_COLOR);
			renderer.polygon(polygonalNavGraph.getWalkZone().getTransformedVertices());
			
			ArrayList<Polygon> obstacles = polygonalNavGraph.getObstacles();
			
			renderer.setColor(OBSTACLE_COLOR);
			for(Polygon p: obstacles) {
				renderer.polygon(p.getTransformedVertices());
			}
			
			// DRAW LINEs OF SIGHT
			renderer.setColor(Color.WHITE);
			ArrayList<NavNodePolygonal> nodes = polygonalNavGraph.getGraphNodes();
			for(NavNodePolygonal n:nodes) {
				for(NavNode n2:n.neighbors) {
					renderer.line(n.x, n.y, ((NavNodePolygonal)n2).x, ((NavNodePolygonal)n2).y);
				}
			}
		}

		renderer.end();
	}

	public BaseActor getActor(String id, boolean searchInventory) {
		BaseActor a = actors.get(id);

		if (a == null && searchInventory) {
			a = World.getInstance().getInventory().getItem(id);
		}

		return a;
	}

	public HashMap<String, BaseActor> getActors() {
		return actors;
	}

	public void addActor(BaseActor actor) {
		actors.put(actor.getId(), actor);
		actor.setScene(this);
		
		SceneLayer layer = getLayer(actor.getLayer());
		
		if(layer == null) { // fallback for compatibility
			layer = new SceneLayer();
			layer.setName(actor.getLayer());
			layers.add(layer);			
		}
		
		layer.add(actor);
	}

	public void setBackground(String bgAtlas, String bgId, String lightMapAtlas, String lightMapId) {
		this.backgroundAtlas = bgAtlas;
		this.backgroundRegionId = bgId;
		this.lightMapAtlas = lightMapAtlas;
		this.lightMapRegionId = lightMapId;
	}

	public BaseActor getActorAt(float x, float y) {
		
		for(SceneLayer layer:layers) {
			
			if(!layer.isVisible())
				continue;
			
			// Obtain actors in reverse (close to camera)
			for (int i = layer.getActors().size() - 1; i >= 0; i--) {
				BaseActor a = layer.getActors().get(i);

				if (a.hasInteraction() && a.hit(x, y)) {
					return a;
				}
			}
		}

		return null;
	}

	public void setPlayer(SpriteActor a) {
		if (a != null) {
			player = a.getId();
			a.setInteraction(false);
		} else {
			player = null;
		}
	}

	public SpriteActor getPlayer() {
		return (SpriteActor) actors.get(player);
	}

	public Vector2 getDepthVector() {
		return depthVector;
	}
	
	public void setDepthVector(Vector2 v) {
		depthVector = v;
	}

	public void removeActor(BaseActor a) {

		if(player != null && a.getId().equals(player)) {
			player = null;
		}

		BaseActor r = actors.remove(a.getId());
		
		if(r == null) {
			EngineLogger.error("Removing actor from scene: Actor not found");
			return;
		}
		
		SceneLayer layer = getLayer(a.getLayer());
		layer.getActors().remove(a);
		
		if(a.isWalkObstacle() && polygonalNavGraph != null)
			polygonalNavGraph.removeDinamicObstacle(a.getBBox());
		
		a.setScene(null);
			
	}

	public Array<AtlasRegion> getBackground() {
		return background;
	}
	
	public SceneCamera getCamera() {
		return camera;
	}
	
	public void resetCamera(float worldWidth, float worldHeight) {
		camera.create(worldWidth, worldHeight);

		if (getPlayer() != null)
			setCameraFollowActor(getPlayer());
	}
	
	public void setCameraFollowActor(SpriteActor a) {
		followActor = a;
		
		if(a != null)
			camera.updatePos(a);
	}
	
	public SpriteActor getCameraFollowActor() {
		return followActor;
	}

	@Override
	public void loadAssets() {

		if (backgroundAtlas != null && !backgroundAtlas.isEmpty()) {			
			EngineAssetManager.getInstance().loadAtlas(backgroundAtlas);
		}

		// LOAD LIGHT MAP
		if (lightMapAtlas != null && !lightMapAtlas.isEmpty()) {
			EngineAssetManager.getInstance().loadAtlas(lightMapAtlas);
		}

		if (musicFilename != null)
			EngineAssetManager.getInstance().loadMusic(musicFilename);

		for (BaseActor a : actors.values()) {
			a.loadAssets();
		}
	}

	@Override
	public void retrieveAssets() {

		// RETRIEVE BACKGROUND
		if (backgroundAtlas != null && !backgroundAtlas.isEmpty()) {
			background = EngineAssetManager.getInstance().getRegions(backgroundAtlas, backgroundRegionId);

			int width = 0;

			for (int i = 0; i < background.size; i++) {
				width += background.get(i).getRegionWidth();
			}

			int height = background.get(0).getRegionHeight();
			
			// Sets the scrolling dimensions. It must be done here because 
			// the background must be loaded to calculate the bbox
			camera.setScrollingDimensions(width,
						height);
			
//			if(followActor != null)
//				camera.updatePos(followActor);
		}

		// RETRIEVE LIGHT MAP
		if (lightMapAtlas != null && !lightMapAtlas.isEmpty()) {
			lightMap = EngineAssetManager.getInstance().getRegions(lightMapAtlas, lightMapRegionId);
		}
		
		// CALC WALK GRAPH
		if(polygonalNavGraph != null) {
			polygonalNavGraph.createInitialGraph();
		}

		// RETRIEVE ACTORS
		for (BaseActor a : actors.values()) {
			a.retrieveAssets();

			// Add to navGraph if obstacle and visible
			if(a.isWalkObstacle() && getPolygonalNavGraph() != null && a.isVisible()) {
				getPolygonalNavGraph().addDinamicObstacle(a.getBBox());
			}			
		}

		if (musicFilename != null) {
			music = EngineAssetManager.getInstance().getMusic(musicFilename);
			if (isPlayingSer) { // TODO must be in World???
				if(music != null) {
					music.setPosition(musicPosSer);
					musicPosSer = 0f;
				}
				
				playMusic();
				isPlayingSer = false;
			}
		}
	}

	@Override
	public void dispose() {

		if (backgroundAtlas != null && !backgroundAtlas.isEmpty()) {			
			EngineAssetManager.getInstance().disposeAtlas(backgroundAtlas);
		}

		// LOAD LIGHT MAP
		if (lightMapAtlas != null && !lightMapAtlas.isEmpty()) {
			EngineAssetManager.getInstance().disposeAtlas(lightMapAtlas);
		}

		// orderedActors.clear();

		for (BaseActor a : actors.values()) {
			a.dispose();
		}

		if (musicFilename != null && music != null) {
			EngineAssetManager.getInstance().disposeMusic(musicFilename);
			music = null;
		}
	}
	

	public void orderLayersByZIndex() {
		for(SceneLayer l:layers) {
			l.orderByZIndex();
		}
	}	

	public PolygonalNavGraph getPolygonalNavGraph() {
		return polygonalNavGraph;
	}

	public void setPolygonalNavGraph(PolygonalNavGraph polygonalNavGraph) {
		this.polygonalNavGraph = polygonalNavGraph;
	}


	// TODO SAVE BG WIDTH AND HEIGHT + WALKZONE
	@Override
	public void write(Json json) {
		json.writeValue("layers", layers);
		json.writeValue("id", id);
		json.writeValue("state", state, state == null ? null : state.getClass());
		json.writeValue("verbs", verbs);
		
		json.writeValue("actors", actors);
		json.writeValue("player", player);

		json.writeValue("backgroundAtlas", backgroundAtlas);
		json.writeValue("backgroundRegionId",backgroundRegionId);

		json.writeValue("lightMapAtlas", lightMapAtlas);
		
		json.writeValue("lightMapRegionId", lightMapRegionId);

		json.writeValue("musicFilename", musicFilename);
		json.writeValue("loopMusic", loopMusic);
		json.writeValue("initialMusicDelay", initialMusicDelay);
		json.writeValue("repeatMusicDelay", repeatMusicDelay);

		json.writeValue("isPlaying", music != null && music.isPlaying());
		json.writeValue("musicPos", music != null && music.isPlaying()?music.getPosition():0f);
		
		json.writeValue("camera", camera);
		
		json.writeValue("followActor", followActor == null ? null : followActor.getId(),
				followActor == null ? null : String.class);
		
		json.writeValue("customProperties", customProperties, customProperties == null ? null : customProperties.getClass());
		
		json.writeValue("depthVector", depthVector);
		
		json.writeValue("polygonalNavGraph", polygonalNavGraph, polygonalNavGraph == null ? null : PolygonalNavGraph.class);
		
	}

	@SuppressWarnings("unchecked")
	@Override
	public void read(Json json, JsonValue jsonData) {
		layers = json.readValue("layers", ArrayList.class, SceneLayer.class,
				jsonData);
		id = json.readValue("id", String.class, jsonData);
		state = json.readValue("state", String.class, jsonData);
		verbs = json.readValue("verbs", VerbManager.class, jsonData);

		actors = json.readValue("actors", HashMap.class, BaseActor.class,
				jsonData);
		player = json.readValue("player", String.class, jsonData);

		for (BaseActor actor: actors.values()) {			
			actor.setScene(this);
			
			SceneLayer layer = getLayer(actor.getLayer());
			layer.add(actor);
		}
		
		orderLayersByZIndex();

		backgroundAtlas = json.readValue("backgroundAtlas", String.class,
				jsonData);
		backgroundRegionId = json.readValue("backgroundRegionId", String.class,
				jsonData);
		lightMapAtlas = json.readValue("lightMapAtlas", String.class, jsonData);
		lightMapRegionId = json.readValue("lightMapRegionId", String.class, jsonData);

		musicFilename = json.readValue("musicFilename", String.class, jsonData);
		loopMusic = json.readValue("loopMusic", Boolean.class, jsonData);
		initialMusicDelay = json.readValue("initialMusicDelay", Float.class,
				jsonData);
		repeatMusicDelay = json.readValue("repeatMusicDelay", Float.class,
				jsonData);

		isPlayingSer = json.readValue("isPlaying", Boolean.class, jsonData);
		musicPosSer = json.readValue("musicPos", Float.class, jsonData);
		
		camera = json.readValue("camera", SceneCamera.class, jsonData);
		String followActorId = json.readValue("followActor", String.class,
				jsonData);
		
		setCameraFollowActor((SpriteActor)actors.get(followActorId));
		
		customProperties = json.readValue("customProperties", HashMap.class, String.class, jsonData);
		
		depthVector = json.readValue("depthVector", Vector2.class, jsonData);
		polygonalNavGraph = json.readValue("polygonalNavGraph", PolygonalNavGraph.class, jsonData);
	}
}
