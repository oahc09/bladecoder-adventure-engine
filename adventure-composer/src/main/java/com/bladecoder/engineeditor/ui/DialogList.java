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

import org.w3c.dom.Element;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.model.BaseDocument;
import com.bladecoder.engineeditor.ui.components.CellRenderer;
import com.bladecoder.engineeditor.ui.components.EditElementDialog;
import com.bladecoder.engineeditor.ui.components.ElementList;

public class DialogList extends ElementList {	
	
    private DialogOptionTree options;

	@Override
	protected EditElementDialog getEditElementDialogInstance(Element e) {
		return new EditDialogDialog(skin, doc, parent, e);
	}
	
    public DialogList(Skin skin) {
    	super(skin, true);
    	
    	options = new DialogOptionTree(skin);
    	row();
    	add(options).expand().fill();

		list.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				int pos = list.getSelectedIndex();
				
				Element v = null;
				
				if(pos != -1) {
					v = list.getItems().get(pos);
					options.addOptions(doc, parent, v);
				} else {
					options.addOptions(doc, parent, null);
				}
				
				toolbar.disableEdit(pos == -1);
			}
		});
		
		list.setCellRenderer(listCellRenderer);
		listCellRenderer.layout(list.getStyle());
		container.minHeight(listCellRenderer.getItemHeight() * 5);
		container.maxHeight(listCellRenderer.getItemHeight() * 5);
    }
    
    
	@Override
	public void addElements(BaseDocument doc, Element parent, String tag) {
		options.addOptions(doc, null, null);
		super.addElements(doc, parent, tag);
    }	


	// -------------------------------------------------------------------------
	// ListCellRenderer
	// -------------------------------------------------------------------------
	private final CellRenderer<Element> listCellRenderer = new CellRenderer<Element>() {

		@Override
		protected String getCellTitle(Element e) {
			return e.getAttribute("id");
		}

		@Override
		public TextureRegion getCellImage(Element e) {
			return Ctx.assetManager.getIcon("ic_talkto");
		}
		
		@Override
		protected boolean hasSubtitle() {
			return false;
		}
		
		@Override
		protected boolean hasImage() {
			return true;
		}
	};

}
