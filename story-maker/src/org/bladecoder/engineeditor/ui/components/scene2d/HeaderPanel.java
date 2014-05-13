package org.bladecoder.engineeditor.ui.components.scene2d;

import org.bladecoder.engineeditor.glcanvas.Assets;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;

public class HeaderPanel extends Table {
	private Actor content;
	private HorizontalGroup north;
	private Label titleLbl;
	private boolean collapsable = true;
	private Image collapseImg;

	public HeaderPanel(Skin skin, String title) {
		titleLbl = new Label(title, skin);
		north = new HorizontalGroup();
		collapseImg = new Image();
		collapseImg.setScaling(Scaling.none);
		
		if(collapsable) {
			north.addActor(collapseImg);
			collapseImg.setDrawable(new TextureRegionDrawable(new TextureRegion(Assets.inst().get("res/images/ic_open.png", Texture.class))));
		}
		
		north.addActor(titleLbl);
		north.align(Align.left);
		north.fill();
		add(north).expandX().fillX();
		row();
		Image image = new Image(skin.getDrawable("white_pixel"));
		image.setColor(skin.getColor("holo-blue"));
		add(image).height(2).padBottom(4f).expandX().fill();
		

		if (collapsable)
			titleLbl.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					toggleCollapse();
				}
			});
	}

	public void setTile(String title) {
		titleLbl.setText(title);
	}

	public void setContent(Actor center) {
		removeActor(this.content);
		
		this.content = center;
		row();
		add(center).expand().fill();
		invalidateHierarchy();
	}
	

	public void setCollapsable(boolean c) {
		collapsable = c;

		if (c) {
			collapseImg.setDrawable(new TextureRegionDrawable(new TextureRegion(Assets.inst().get("res/images/ic_open.png", Texture.class))));
		} else
			collapseImg.setDrawable(null);
	}

	public void toggleCollapse() {
		if (collapsable) {
			if (content.isVisible()) {
				content.setVisible(false);

				collapseImg.setDrawable(new TextureRegionDrawable(new TextureRegion(Assets.inst().get("res/images/ic_closed.png", Texture.class))));
			} else {
				content.setVisible(true);

				collapseImg.setDrawable(new TextureRegionDrawable(new TextureRegion(Assets.inst().get("res/images/ic_open.png", Texture.class))));
			}
		}
	}

}
