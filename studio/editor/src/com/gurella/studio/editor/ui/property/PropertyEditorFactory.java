package com.gurella.studio.editor.ui.property;

import static com.gurella.engine.utils.Values.cast;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Date;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.swt.widgets.Composite;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.GridPoint3;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Bits;
import com.gurella.engine.asset.AssetReference;
import com.gurella.engine.asset.descriptor.AssetDescriptors;
import com.gurella.engine.metatype.DefaultMetaType.SimpleMetaType;
import com.gurella.engine.metatype.MetaTypes;
import com.gurella.engine.metatype.Property;
import com.gurella.engine.utils.BitsExt;
import com.gurella.engine.utils.ImmutableArray;
import com.gurella.engine.utils.Reflection;
import com.gurella.engine.utils.Values;
import com.gurella.studio.GurellaStudioPlugin;
import com.gurella.studio.editor.engine.property.CustomCompositePropertyEditor;
import com.gurella.studio.editor.engine.property.CustomPropertyEditor;
import com.gurella.studio.editor.engine.property.CustomSimplePropertyEditor;

public class PropertyEditorFactory {
	public static boolean hasReflectionEditor(PropertyEditorContext<?, ?> context) {
		IJavaProject javaProject = context.javaProject;
		Class<?> beanType = context.bean.getClass();
		Property<?> property = context.property;
		PropertyEditorData data = PropertyEditorData.get(javaProject, beanType, property);
		if (data != null && data.isValidFactoryClass()) {
			return false;
		}

		Class<?> propertyType = context.property.getType();
		return getEditorType(context, propertyType) == ReflectionPropertyEditor.class;
	}

	public static Class<? extends PropertyEditor<?>> getEditorType(PropertyEditorContext<?, ?> context,
			Class<?> propertyType) {
		if (propertyType == Boolean.class || propertyType == boolean.class) {
			return BooleanPropertyEditor.class;
		} else if (propertyType == Integer.class || propertyType == int.class) {
			return IntegerPropertyEditor.class;
		} else if (propertyType == Long.class || propertyType == long.class) {
			return LongPropertyEditor.class;
		} else if (propertyType == Float.class || propertyType == float.class) {
			return FloatPropertyEditor.class;
		} else if (propertyType == Byte.class || propertyType == byte.class) {
			return BytePropertyEditor.class;
		} else if (propertyType == Short.class || propertyType == short.class) {
			return ShortPropertyEditor.class;
		} else if (propertyType == Character.class || propertyType == char.class) {
			return CharacterPropertyEditor.class;
		} else if (propertyType == Double.class || propertyType == double.class) {
			return DoublePropertyEditor.class;
		} else if (propertyType == String.class) {
			return StringPropertyEditor.class;
		} else if (propertyType == Date.class) {
			return DatePropertyEditor.class;
		} else if (propertyType == Vector3.class) {
			return Vector3PropertyEditor.class;
		} else if (propertyType == Vector2.class) {
			return Vector2PropertyEditor.class;
		} else if (propertyType == Quaternion.class) {
			return QuaternionPropertyEditor.class;
		} else if (propertyType == GridPoint2.class) {
			return GridPoint2PropertyEditor.class;
		} else if (propertyType == GridPoint3.class) {
			return GridPoint3PropertyEditor.class;
		} else if (propertyType == Matrix3.class) {
			return Matrix3PropertyEditor.class;
		} else if (propertyType == Matrix4.class) {
			return Matrix4PropertyEditor.class;
		} else if (propertyType == Color.class) {
			return ColorPropertyEditor.class;
		} else if (propertyType == Bits.class || propertyType == BitsExt.class) {
			return BitsPropertyEditor.class;
		} else if (propertyType == BigInteger.class) {
			return BigIntegerPropertyEditor.class;
		} else if (propertyType == BigDecimal.class) {
			return BigDecimalPropertyEditor.class;
		} else if (propertyType == AssetReference.class) {
			return cast(AssetReferencePropertyEditor.class);
		} else if (propertyType.isArray()) {
			return cast(ArrayPropertyEditor.class);
		} else if (propertyType.isEnum()) {
			return cast(EnumPropertyEditor.class);
		} else if (AssetDescriptors.isAssetType(propertyType)) {
			return cast(AssetPropertyEditor.class);
		} else if (context.property.isFinal() && context.bean != null && isSimpleProperty(propertyType)) {
			return cast(SimpleObjectPropertyEditor.class);
		}

		///// custom editors for maps...

		else if (Array.class.isAssignableFrom(propertyType)) {
			return cast(GdxArrayPropertyEditor.class);
		} else if (Collection.class.isAssignableFrom(propertyType)) {
			return cast(CollectionPropertyEditor.class);
		} else {
			return cast(ReflectionPropertyEditor.class);
		}
	}

	public static <T> PropertyEditor<T> createEditor(Composite parent, PropertyEditorContext<?, T> context) {
		PropertyEditor<T> customEditor = createCustomEditor(parent, context);
		if (customEditor == null) {
			Class<T> propertyType = context.property.getType();
			return createEditor(parent, context, propertyType);
		} else {
			return customEditor;
		}
	}

	private static <T> PropertyEditor<T> createCustomEditor(Composite parent, PropertyEditorContext<?, T> context) {
		try {
			IJavaProject javaProject = context.javaProject;
			Class<?> beanType = context.bean.getClass();
			Property<?> property = context.property;
			PropertyEditorData data = PropertyEditorData.get(javaProject, beanType, property);
			if (data == null || !data.isValidFactoryClass()) {
				return null;
			}

			Class<?> factoryClass = Reflection.forName(data.customFactoryClass);
			Constructor<?> constructor = factoryClass.getDeclaredConstructor(new Class[0]);
			constructor.setAccessible(true);
			Object factory = constructor.newInstance(new Object[0]);
			switch (data.type) {
			case composite:
				return new CustomCompositePropertyEditor<>(parent, context, cast(factory));
			case simple:
				return new CustomSimplePropertyEditor<>(parent, context, cast(factory));
			case custom:
				return new CustomPropertyEditor<>(parent, context, cast(factory));
			default:
				return new CustomCompositePropertyEditor<>(parent, context, cast(factory));
			}
		} catch (Exception e) {
			GurellaStudioPlugin.log(e, "Error creating editor.");
			return null;
		}
	}

	public static <T> PropertyEditor<T> createEditor(Composite parent, PropertyEditorContext<?, T> context,
			Class<T> propertyType) {
		if (propertyType == Boolean.class || propertyType == boolean.class) {
			return Values.cast(new BooleanPropertyEditor(parent, cast(context)));
		} else if (propertyType == Integer.class || propertyType == int.class) {
			return cast(new IntegerPropertyEditor(parent, cast(context)));
		} else if (propertyType == Long.class || propertyType == long.class) {
			return cast(new LongPropertyEditor(parent, cast(context)));
		} else if (propertyType == Float.class || propertyType == float.class) {
			return cast(new FloatPropertyEditor(parent, cast(context)));
		} else if (propertyType == Byte.class || propertyType == byte.class) {
			return cast(new BytePropertyEditor(parent, cast(context)));
		} else if (propertyType == Short.class || propertyType == short.class) {
			return cast(new ShortPropertyEditor(parent, cast(context)));
		} else if (propertyType == Character.class || propertyType == char.class) {
			return cast(new CharacterPropertyEditor(parent, cast(context)));
		} else if (propertyType == Double.class || propertyType == double.class) {
			return cast(new DoublePropertyEditor(parent, cast(context)));
		} else if (propertyType == String.class) {
			return cast(new StringPropertyEditor(parent, cast(context)));
		} else if (propertyType == Date.class) {
			return cast(new DatePropertyEditor(parent, cast(context)));
		} else if (propertyType == Vector3.class) {
			return cast(new Vector3PropertyEditor(parent, cast(context)));
		} else if (propertyType == Vector2.class) {
			return cast(new Vector2PropertyEditor(parent, cast(context)));
		} else if (propertyType == Quaternion.class) {
			return cast(new QuaternionPropertyEditor(parent, cast(context)));
		} else if (propertyType == GridPoint2.class) {
			return cast(new GridPoint2PropertyEditor(parent, cast(context)));
		} else if (propertyType == GridPoint3.class) {
			return cast(new GridPoint3PropertyEditor(parent, cast(context)));
		} else if (propertyType == Matrix3.class) {
			return cast(new Matrix3PropertyEditor(parent, cast(context)));
		} else if (propertyType == Matrix4.class) {
			return cast(new Matrix4PropertyEditor(parent, cast(context)));
		} else if (propertyType == Color.class) {
			return cast(new ColorPropertyEditor(parent, cast(context)));
		} else if (propertyType == Bits.class || propertyType == BitsExt.class) {
			return cast(new BitsPropertyEditor(parent, cast(context)));
		} else if (propertyType == BigInteger.class) {
			return cast(new BigIntegerPropertyEditor(parent, cast(context)));
		} else if (propertyType == BigDecimal.class) {
			return cast(new BigDecimalPropertyEditor(parent, cast(context)));
		} else if (propertyType == AssetReference.class) {
			return cast(new AssetReferencePropertyEditor<Object>(parent, cast(context)));
		} else if (propertyType.isArray()) {
			return cast(new ArrayPropertyEditor<>(parent, context));
		} else if (propertyType.isEnum()) {
			return cast(new EnumPropertyEditor<>(parent, cast(context)));
		} else if (AssetDescriptors.isAssetType(propertyType)) {
			return cast(new AssetPropertyEditor<>(parent, context, propertyType));
		} else if (context.property.isFinal() && context.bean != null && isSimpleProperty(propertyType)) {
			// TODO handle in ReflectionPropertyEditor
			return cast(new SimpleObjectPropertyEditor<>(parent, context));
		}

		///// TODO custom editors for maps...

		else if (Array.class.isAssignableFrom(propertyType)) {
			return cast(new GdxArrayPropertyEditor<>(parent, cast(context)));
		} else if (Collection.class.isAssignableFrom(propertyType)) {
			return cast(new CollectionPropertyEditor<>(parent, cast(context)));
		} else {
			return new ReflectionPropertyEditor<T>(parent, context);
		}
	}

	private static boolean isSimpleProperty(Class<?> propertyType) {
		ImmutableArray<Property<?>> properties = MetaTypes.getMetaType(propertyType).getProperties();
		Property<?> editableProperty = null;
		for (Property<?> property : properties) {
			if (property.isEditable()) {
				if (editableProperty == null) {
					editableProperty = property;
				} else {
					return false;
				}
			}
		}

		return editableProperty != null && MetaTypes.getMetaType(editableProperty.getType()) instanceof SimpleMetaType;
	}
}
