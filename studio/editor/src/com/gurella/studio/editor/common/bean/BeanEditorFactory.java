package com.gurella.studio.editor.common.bean;

import static com.gurella.engine.utils.Values.cast;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.swt.widgets.Composite;

import com.gurella.engine.editor.model.ModelEditorDescriptor;
import com.gurella.engine.test.TestEditorComponent;
import com.gurella.engine.utils.Reflection;
import com.gurella.engine.utils.Values;
import com.gurella.studio.editor.SceneEditorContext;
import com.gurella.studio.editor.common.bean.custom.TestCustomizableBeanEditor;
import com.gurella.studio.editor.engine.bean.CustomBeanEditor;
import com.gurella.studio.editor.engine.bean.CustomBeanEditorContextAdapter;

public class BeanEditorFactory {
	private static final Class<?>[] customBeanEditorParameterTypes = { Composite.class, BeanEditorContext.class };
	private static final Map<Class<?>, Class<?>> defaultFactories = new HashMap<>();
	private static final Map<Class<?>, String> customFactories = new HashMap<>();

	static {
		defaultFactories.put(TestEditorComponent.class, TestCustomizableBeanEditor.class);
	}

	public static <T> BeanEditor<T> createEditor(Composite parent, SceneEditorContext context, T instance) {
		Class<CustomBeanEditor<T>> beanEditorType = Values.cast(defaultFactories.get(instance.getClass()));
		if (beanEditorType != null) {
			BeanEditorContext<T> editorContext = new BeanEditorContext<>(context, instance);
			return Reflection.newInstance(beanEditorType, customBeanEditorParameterTypes, parent, editorContext);
		}

		com.gurella.engine.editor.model.ModelEditorFactory<T> factory = getCustomFactory(instance, context);
		if (factory == null) {
			return new DefaultBeanEditor<T>(parent, context, instance);
		} else {
			return new CustomBeanEditor<>(parent, new CustomBeanEditorContextAdapter<>(context, instance, factory));
		}
	}

	public static <T> BeanEditor<T> createEditor(Composite parent, BeanEditorContext<?> parentContext, T instance) {
		SceneEditorContext sceneContext = parentContext.sceneEditorContext;
		Class<CustomBeanEditor<T>> beanEditorType = Values.cast(defaultFactories.get(instance.getClass()));
		if (beanEditorType != null) {
			BeanEditorContext<T> editorContext = new BeanEditorContext<>(parentContext, instance);
			return Reflection.newInstance(beanEditorType, customBeanEditorParameterTypes, parent, editorContext);
		}

		com.gurella.engine.editor.model.ModelEditorFactory<T> factory = getCustomFactory(instance, sceneContext);
		if (factory == null) {
			return new DefaultBeanEditor<T>(parent, new BeanEditorContext<>(parentContext, instance));
		} else {
			return new CustomBeanEditor<>(parent,
					new CustomBeanEditorContextAdapter<>(parentContext, instance, factory));
		}
	}

	private static <T> com.gurella.engine.editor.model.ModelEditorFactory<T> getCustomFactory(T modelInstance,
			SceneEditorContext sceneContext) {
		try {
			String customFactoryClass = getCustomFactoryClass(modelInstance, sceneContext.javaProject);
			if (customFactoryClass == null) {
				return null;
			}

			Class<?> factoryClass = sceneContext.classLoader.loadClass(customFactoryClass);
			Constructor<?> constructor = factoryClass.getDeclaredConstructor(new Class[0]);
			constructor.setAccessible(true);
			return cast(constructor.newInstance(new Object[0]));
		} catch (Exception e) {
			customFactories.put(modelInstance.getClass(), null);
			return null;
		}
	}

	private static <T> String getCustomFactoryClass(T modelInstance, IJavaProject javaProject) throws Exception {
		Class<?> modelClass = modelInstance.getClass();
		if (customFactories.containsKey(modelClass)) {
			return customFactories.get(modelClass);
		}

		IType type = javaProject.findType(modelClass.getName());
		for (IAnnotation annotation : type.getAnnotations()) {
			if (annotation.getElementName().equals(ModelEditorDescriptor.class.getSimpleName())) {
				String modelFactoryClass = parseAnnotation(type, annotation);
				customFactories.put(modelClass, modelFactoryClass);
				return modelFactoryClass;
			}
		}

		IAnnotation annotation = type.getAnnotation(ModelEditorDescriptor.class.getName());
		String modelFactoryClass = annotation == null ? null : parseAnnotation(type, annotation);
		customFactories.put(modelClass, modelFactoryClass);
		return modelFactoryClass;
	}

	private static String parseAnnotation(IType type, IAnnotation annotation) throws JavaModelException {
		IMemberValuePair[] memberValuePairs = annotation.getMemberValuePairs();
		for (IMemberValuePair memberValuePair : memberValuePairs) {
			if ("factory".equals(memberValuePair.getMemberName())) {
				String[][] resolveType = type.resolveType((String) memberValuePair.getValue());
				if (resolveType.length != 1) {
					return null;
				}
				String[] path = resolveType[0];
				int last = path.length - 1;
				path[last] = path[last].replaceAll("\\.", "\\$");
				StringBuilder builder = new StringBuilder();
				for (String part : path) {
					if (builder.length() > 0) {
						builder.append(".");
					}
					builder.append(part);
				}
				return builder.toString();
			}
		}

		return null;
	}
}