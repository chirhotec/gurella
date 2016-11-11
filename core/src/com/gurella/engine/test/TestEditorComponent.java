package com.gurella.engine.test;

import com.gurella.engine.base.model.PropertyDescriptor;
import com.gurella.engine.editor.property.PropertyEditorDescriptor;
import com.gurella.engine.scene.SceneNodeComponent2;

public class TestEditorComponent extends SceneNodeComponent2 {
	public int testInt;
	public int intGroup;
	
	public TestObj testObj = new TestObj();
	public TestObj testObj2 = new TestObj();
	
	public static class TestObj {
		public int int1;
		@PropertyEditorDescriptor(group = "TestObjGroup")
		public int int2;
	}
}
