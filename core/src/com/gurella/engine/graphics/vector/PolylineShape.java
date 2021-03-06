package com.gurella.engine.graphics.vector;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.FloatArray;

public class PolylineShape extends Shape {
	private final FloatArray points = new FloatArray();
	
	public PolylineShape(FloatArray points) {
		this.points.addAll(points);
	}

	public PolylineShape(Vector2... points) {
		for(int i = 0; i < points.length; i++) {
			Vector2 point = points[i];
			this.points.add(point.x);
			this.points.add(point.y);
		}
	}
	
	public PolylineShape(float... points) {
		this.points.addAll(points);
	}
	
	public void addPoint(Vector2 point) {
		this.points.add(point.x);
		this.points.add(point.y);
		markDataChanged();
	}
	
	public void addPoint(float x, float y) {
		this.points.add(x);
		this.points.add(y);
		markDataChanged();
	}
	
	public void addPoints(FloatArray points) {
		this.points.addAll(points);
		markDataChanged();
	}

	public void addPoints(Vector2... points) {
		for(int i = 0; i < points.length; i++) {
			Vector2 point = points[i];
			this.points.add(point.x);
			this.points.add(point.y);
		}
		markDataChanged();
	}
	
	public void addPoints(float... points) {
		this.points.addAll(points);
		markDataChanged();
	}
	
	public void setPoint(int index, Vector2 point) {
		setPoint(index, point.x, point.y);
	}
	
	public void setPoint(int index, float x, float y) {
		int pointStart = index * 2;
		float oldX = points.get(pointStart);
		float oldY = points.get(pointStart + 1);
		boolean changed = false;
		
		if (oldX != x) {
			points.set(pointStart, x);
			changed = true;
		}
		
		if (oldY != y) {
			points.set(pointStart + 1, y);
			changed = true;
		}
		
		if(changed) {
			markDataChanged();
		}
	}
	
	public float getX(int index) {
		return points.get(index * 2);
	}
	
	public float getY(int index) {
		return points.get(index * 2 + 1);
	}
	
	public Vector2 getPoint(int index, Vector2 out) {
		int pointStart = index * 2;
		out.x = points.get(pointStart);
		out.y = points.get(pointStart + 1);
		return out;
	}
	
	public int getSize() {
		return points.size / 2;
	}
	
	@Override
	protected void initPath(Path path) {
		if(points.size < 4) {
			return;
		}
		
		path.moveTo(points.get(0), points.get(1));
        for (int i = 2; i < points.size; i += 2) {
        	path.lineTo(points.get(i), points.get(i + 1));
        }
	}
	
	@Override
	public void reset() {
		super.reset();
		points.clear();
	}
}
