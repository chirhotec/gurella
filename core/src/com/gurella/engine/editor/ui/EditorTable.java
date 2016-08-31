package com.gurella.engine.editor.ui;

public interface EditorTable extends EditorBaseComposite {
	void clear(int index);

	void clear(int[] indices);

	void clear(int start, int end);

	void clearAll();

	void deselect(int index);

	void deselect(int[] indices);

	void deselect(int start, int end);

	void deselectAll();

	EditorTableColumn getColumn(int index);

	int getColumnCount();

	int[] getColumnOrder();

	EditorTableColumn[] getColumns();

	int getGridLineWidth();

	int getHeaderHeight();

	boolean getHeaderVisible();

	EditorTableItem getItem(int index);

	EditorTableItem getItem(int x, int y);

	int getItemCount();

	int getItemHeight();

	EditorTableItem[] getItems();

	boolean getLinesVisible();

	EditorTableItem[] getSelection();

	int getSelectionCount();

	int getSelectionIndex();

	int getSortDirection();

	int getTopIndex();

	int indexOf(EditorTableColumn column);

	int indexOf(EditorTableItem item);

	boolean isSelected(int index);

	void remove(int index);

	void remove(int[] indices);

	void remove(int start, int end);

	void removeAll();

	void select(int index);

	void select(int[] indices);

	void select(int start, int end);

	void selectAll();

	void setColumnOrder(int[] order);

	void setHeaderVisible(boolean show);

	void setItemCount(int count);

	void setLinesVisible(boolean show);

	void setSelection(int index);

	void setSelection(int[] indices);

	void setSelection(int start, int end);

	void setSelection(EditorTableItem item);

	void setSelection(EditorTableItem[] items);

	void setSortColumn(EditorTableColumn column);

	void setSortDirection(int direction);

	void setTopIndex(int index);

	void showColumn(EditorTableColumn column);

	void showItem(EditorTableItem item);

	void showSelection();

	EditorTableColumn createColumn();

	EditorTableColumn createColumn(int index);

	EditorTableColumn createColumn(Alignment alignment);

	EditorTableColumn createColumn(int index, Alignment alignment);

	EditorTableItem createItem();

	EditorTableItem createItem(int index);

	public static class TableStyle extends ScrollableStyle<TableStyle> {
		public boolean check;
		public boolean multiSelection;
		public boolean fullSelection;
		public boolean hideSelection;
		public boolean virtual;
		public boolean noScroll;

		public TableStyle check(boolean check) {
			this.check = check;
			return cast();
		}

		public TableStyle multiSelection(boolean multiSelection) {
			this.multiSelection = multiSelection;
			return cast();
		}

		public TableStyle fullSelection(boolean fullSelection) {
			this.fullSelection = fullSelection;
			return cast();
		}

		public TableStyle hideSelection(boolean hideSelection) {
			this.hideSelection = hideSelection;
			return cast();
		}

		public TableStyle virtual(boolean virtual) {
			this.virtual = virtual;
			return cast();
		}

		public TableStyle noScroll(boolean noScroll) {
			this.noScroll = noScroll;
			return cast();
		}
	}
}
