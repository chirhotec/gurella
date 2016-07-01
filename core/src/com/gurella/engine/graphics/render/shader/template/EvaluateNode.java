package com.gurella.engine.graphics.render.shader.template;

import com.badlogic.gdx.utils.GdxRuntimeException;
import com.gurella.engine.graphics.render.shader.generator.ShaderGeneratorContext;

public abstract class EvaluateNode extends ShaderTemplateNode {
	private String firstProperty;
	private String secondProperty;
	private Integer constant;

	public EvaluateNode(String expression) {
		String[] params = expression.split(",");
		if (params.length != 2) {
			throw new GdxRuntimeException(
					"Invalid expression: " + expression + " Correct form: '@expType (variableName, variableNameOrIntLiteral)'.");
		}

		firstProperty = params[0].trim();
		secondProperty = params[1].trim();
		try {
			constant = Integer.valueOf(secondProperty);
		} catch (Exception e) {
		}
	}

	@Override
	protected void generate(ShaderGeneratorContext context) {
		int first = context.getValue(firstProperty);
		int second = constant == null ? context.getValue(secondProperty) : constant.intValue();
		context.setValue(firstProperty, evaluate(first, second));
	}

	protected abstract int evaluate(int first, int second);

	@Override
	protected String toStringValue() {
		return firstProperty + getOperatorString() + secondProperty;
	}

	protected abstract String getOperatorString();
}
