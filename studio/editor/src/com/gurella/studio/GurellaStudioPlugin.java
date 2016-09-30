package com.gurella.studio;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ColorDescriptor;
import org.eclipse.jface.resource.DeviceResourceDescriptor;
import org.eclipse.jface.resource.DeviceResourceManager;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.RGBA;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.gurella.engine.editor.ui.EditorLogLevel;
import com.gurella.studio.editor.utils.RGBAColorDescriptor;

public class GurellaStudioPlugin extends AbstractUIPlugin {
	public static final String PLUGIN_ID = "com.gurella.studio"; //$NON-NLS-1$
	public static final Object glMutex = new Object();

	private static GurellaStudioPlugin plugin;

	private static Map<DeviceResourceDescriptor, Object> pluginResources = new HashMap<>();
	private static DeviceResourceManager resourceManager;
	private static GurellaFormToolkit toolkit;

	public GurellaStudioPlugin() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		Display display = getDisplay();
		// TODO http://www.eclipsezone.com/eclipse/forums/t61092.html colors...
		toolkit = new GurellaFormToolkit(display);
		resourceManager = new DeviceResourceManager(display);
	}

	private static Display getDisplay() {
		Display display = Display.getCurrent();
		return display == null ? PlatformUI.getWorkbench().getDisplay() : display;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
		toolkit.disposePrivate();
		resourceManager.dispose();
		pluginResources.entrySet().forEach(e -> e.getKey().destroyResource(e.getValue()));
	}

	/**
	 * Returns the shared instance
	 */
	public static GurellaStudioPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in relative path
	 *
	 * @param path
	 *            the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	public static DeviceResourceManager getResourceManager() {
		return resourceManager;
	}

	public static LocalResourceManager newResourceManager(Control owner) {
		return new LocalResourceManager(resourceManager, owner);
	}

	public static Image createImage(String path) {
		return resourceManager.createImage(getImageDescriptor(path));
	}

	public static Image createImageWithDefault(String path) {
		return resourceManager.createImageWithDefault(getImageDescriptor(path));
	}

	public static Image createImage(ImageDescriptor descriptor) {
		return resourceManager.createImage(descriptor);
	}

	public static Image createImageWithDefault(ImageDescriptor descriptor) {
		return resourceManager.createImageWithDefault(descriptor);
	}

	public static void destroyImage(String path) {
		resourceManager.destroyImage(getImageDescriptor(path));
	}

	public static void destroyImage(ImageDescriptor descriptor) {
		resourceManager.destroyImage(descriptor);
	}

	public static Color createColor(ColorDescriptor descriptor) {
		return resourceManager.createColor(descriptor);
	}

	public static Color createColor(RGB rgb) {
		return resourceManager.createColor(rgb);
	}

	public static Color createColor(int red, int green, int blue) {
		return resourceManager.createColor(new RGB(red, green, blue));
	}

	public static Color createColor(RGBA rgba) {
		RGB rgb = rgba.rgb;
		return resourceManager.createColor(new RGBAColorDescriptor(rgb.red, rgb.green, rgb.blue, rgba.alpha));
	}

	public static Color createColor(int red, int green, int blue, int alpha) {
		return resourceManager.createColor(new RGBAColorDescriptor(red, green, blue, alpha));
	}

	public static Color createColor(com.badlogic.gdx.graphics.Color color) {
		return createColor((int) color.r * 255, (int) color.g * 255, (int) color.b * 255, (int) color.a * 255);
	}

	public static void destroyColor(Color color) {
		resourceManager.destroyColor(new RGBAColorDescriptor(color.getRGBA()));
	}

	public static void destroyColor(RGB rgb) {
		resourceManager.destroyColor(rgb);
	}

	public static void destroyColor(int red, int green, int blue) {
		resourceManager.destroyColor(new RGB(red, green, blue));
	}

	public static void destroyColor(RGBA rgba) {
		resourceManager.destroyColor(new RGBAColorDescriptor(rgba));
	}

	public static void destroyColor(int red, int green, int blue, int alpha) {
		resourceManager.destroyColor(new RGBAColorDescriptor(red, green, blue, alpha));
	}

	public static void destroyColor(ColorDescriptor descriptor) {
		resourceManager.destroyColor(descriptor);
	}

	public static Font createFont(FontDescriptor descriptor) {
		return resourceManager.createFont(descriptor);
	}

	public static Font createFont(Control control, int newStyle) {
		return resourceManager.createFont(FontDescriptor.createFrom(control.getFont()).setStyle(newStyle));
	}

	public static Font createFontWithStyle(Control control, int additionalStyle) {
		return resourceManager.createFont(FontDescriptor.createFrom(control.getFont()).withStyle(additionalStyle));
	}

	public static Font createFont(Control control, int newHeight, int newStyle) {
		return resourceManager
				.createFont(FontDescriptor.createFrom(control.getFont()).setHeight(newHeight).setStyle(newStyle));
	}

	public static Font createFontWithStyle(Control control, int newHeight, int additionalStyle) {
		return resourceManager.createFont(
				FontDescriptor.createFrom(control.getFont()).setHeight(newHeight).withStyle(additionalStyle));
	}

	public static void destroyFont(FontDescriptor descriptor) {
		resourceManager.destroyFont(descriptor);
	}

	public static void destroyFont(Font font) {
		resourceManager.destroyFont(FontDescriptor.createFrom(font));
	}

	private static <T> T getResource(DeviceResourceDescriptor descriptor) {
		Object resource = pluginResources.get(descriptor);
		if (resource == null) {
			resource = descriptor.createResource(getDisplay());
			pluginResources.put(descriptor, resource);
		}
		@SuppressWarnings("unchecked")
		T casted = (T) resource;
		return casted;
	}

	public static Image getImage(String path) {
		return getResource(getImageDescriptor(path));
	}

	public static Image getImage(ImageDescriptor descriptor) {
		return getResource(descriptor);
	}

	public static Color getColor(ColorDescriptor descriptor) {
		return getResource(descriptor);
	}

	public static Color getColor(RGB rgb) {
		return getResource(ColorDescriptor.createFrom(rgb));
	}

	public static Color getColor(int red, int green, int blue) {
		return getColor(new RGB(red, green, blue));
	}

	public static Color getColor(RGBA rgba) {
		RGB rgb = rgba.rgb;
		return getResource(new RGBAColorDescriptor(rgb.red, rgb.green, rgb.blue, rgba.alpha));
	}

	public static Color getColor(int red, int green, int blue, int alpha) {
		return getResource(new RGBAColorDescriptor(red, green, blue, alpha));
	}

	public static Color getColor(com.badlogic.gdx.graphics.Color color) {
		return getColor((int) color.r * 255, (int) color.g * 255, (int) color.b * 255, (int) color.a * 255);
	}

	public static Font getFont(FontDescriptor descriptor) {
		return getResource(descriptor);
	}

	public static Font getFont(Control control, int newStyle) {
		return getResource(FontDescriptor.createFrom(control.getFont()).setStyle(newStyle));
	}

	public static Font getFontWithStyle(Control control, int additionalStyle) {
		return getResource(FontDescriptor.createFrom(control.getFont()).withStyle(additionalStyle));
	}

	public static Font getFont(Control control, int newHeight, int newStyle) {
		return getResource(FontDescriptor.createFrom(control.getFont()).setHeight(newHeight).setStyle(newStyle));
	}

	public static Font getFontWithStyle(Control control, int newHeight, int additionalStyle) {
		return getResource(
				FontDescriptor.createFrom(control.getFont()).setHeight(newHeight).withStyle(additionalStyle));
	}

	public static FormToolkit getToolkit() {
		return toolkit;
	}

	public static void log(IStatus status) {
		getDefault().getLog().log(status);
	}

	public static void log(EditorLogLevel level, String message) {
		int swtLevel;
		switch (level) {
		case OK:
			swtLevel = IStatus.OK;
			break;
		case INFO:
			swtLevel = IStatus.INFO;
			break;
		case WARNING:
			swtLevel = IStatus.WARNING;
			break;
		case ERROR:
			swtLevel = IStatus.ERROR;
			break;
		case CANCEL:
			swtLevel = IStatus.CANCEL;
			break;
		default:
			throw new IllegalArgumentException();
		}
		log(new Status(swtLevel, PLUGIN_ID, message));
	}

	public static IStatus log(Throwable t, String message) {
		MultiStatus status = createErrorStatus(t, message);
		getDefault().getLog().log(status);
		return status;
	}

	public static MultiStatus createErrorStatus(Throwable t, String message) {
		StackTraceElement[] stackTraces = Thread.currentThread().getStackTrace();
		Status[] childStatuses = Arrays.stream(stackTraces)
				.map(st -> new Status(IStatus.ERROR, PLUGIN_ID, st.toString())).toArray(i -> new Status[i]);
		return new MultiStatus(PLUGIN_ID, IStatus.ERROR, childStatuses, message, t);
	}

	private static final class GurellaFormToolkit extends FormToolkit {
		private GurellaFormToolkit(Display display) {
			super(new GurellaFormColors(display));
		}

		public void disposePrivate() {
			super.dispose();
		}

		@Override
		public void setOrientation(int orientation) {
		}

		@Override
		public void setBackground(Color bg) {
		}

		@Override
		public void setBorderStyle(int style) {
		}

		@Override
		public void dispose() {
			throw new UnsupportedOperationException("");
		}
	}

	private static final class GurellaFormColors extends FormColors {
		public GurellaFormColors(Display display) {
			super(display);
		}

		@Override
		protected void updateBorderColor() {
			this.border = display.getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW);
		}
	}
}
