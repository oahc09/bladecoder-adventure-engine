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
package com.bladecoder.engineeditor.ui.components;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.bladecoder.engineeditor.Ctx;

public class SceneInputPanel extends OptionsInputPanel {	

	SceneInputPanel(Skin skin, String title, String desc, boolean mandatory, String defaultValue) {	
		NodeList scenes = Ctx.project.getSelectedChapter().getScenes();
		int l = scenes.getLength();
		if(!mandatory) l++;
		String values[] = new String[l];
		
		if(!mandatory) {
			values[0] = "";
		}
		
		for(int i = 0; i < scenes.getLength(); i++) {
			if(mandatory)
				values[i] = ((Element)scenes.item(i)).getAttribute("id");
			else
				values[i+1] = ((Element)scenes.item(i)).getAttribute("id");
		}
		
		input = new SelectBox<String>(skin);
		init(skin, title, desc, input, mandatory, defaultValue);
		input.setItems(values);
       	if(defaultValue != null)
    		setText(defaultValue);
	}
}
