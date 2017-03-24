package com.gurella.engine.graphics.render.shader.generator;

import com.badlogic.gdx.utils.ObjectIntMap;
import com.badlogic.gdx.utils.Pool.Poolable;
import com.gurella.engine.graphics.GraphicsService;
import com.gurella.engine.graphics.render.shader.template.PieceNode;
import com.gurella.engine.graphics.render.shader.template.ShaderTemplate;
import com.gurella.engine.utils.Values;

public class ShaderGeneratorContext implements Poolable {
	private StringBuilder builder = new StringBuilder();

	private final ObjectIntMap<String> values = new ObjectIntMap<String>();

	private ShaderTemplate root;

	public void init(ShaderTemplate template) {
		this.root = template;
		builder.setLength(0);
		define("GL20");
		if (GraphicsService.isGl30Available()) {
			define("GL30");
		}
		for (String extension : GraphicsService.getGlExtensions()) {
			define(extension);
		}
	}

	public void define(String propertyName) {
		values.put(propertyName, 0);
	}

	public void undefine(String propertyName) {
		values.remove(propertyName, 0);
	}

	public boolean isDefined(String propertyName) {
		return values.containsKey(propertyName);
	}

	public PieceNode getPiece(String pieceName) {
		return root.getPiece(pieceName);
	}

	public void append(CharSequence sequence) {
		builder.append(sequence);
	}

	public boolean isValueSet(String valueName) {
		return values.containsKey(valueName);
	}

	public int getValue(String valueName) {
		return values.get(valueName, 0);
	}

	public void setValue(String valueName, int value) {
		values.put(valueName, value);
	}

	public void unsetValue(String valueName) {
		values.remove(valueName, 0);
	}

	public String getShaderSource(boolean format) {
		if (format) {
			format();
		}
		return builder.toString();
	}

	private void format() {
		int index = 0;
		int length = builder.length();
		while (index < length && Values.isWhitespace(builder.charAt(index))) {
			index++;
		}
		builder.delete(0, index);

		while ((index = builder.indexOf("\r\n\r\n\r\n", index)) > -1) {
			builder.delete(index, index + 2);
		}

		index = 0;
		while ((index = builder.indexOf("\n\n\n", index)) > -1) {
			builder.delete(index, index + 1);
		}

		index = 0;
		while ((index = builder.indexOf("\r\r\n", index)) > -1) {
			builder.delete(index, index + 1);
		}
	}

	@Override
	public void reset() {
		builder.setLength(0);
		values.clear();
		root = null;
	}
}
