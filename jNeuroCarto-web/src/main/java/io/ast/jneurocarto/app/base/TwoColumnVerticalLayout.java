package io.ast.jneurocarto.app.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jspecify.annotations.NullMarked;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.Element;

@NullMarked
public class TwoColumnVerticalLayout extends VerticalLayout {

    record Row(HorizontalLayout layout, List<Component> components) implements HasElement {
//        public Row {
//
//        }

        public Row() {
            this(new HorizontalLayout(), new ArrayList<>());
        }

        @Override
        public Element getElement() {
            return layout.getElement();
        }

        boolean isEmpty() {
            return size() == 0;
        }

        int size() {
            return components().size();
        }

        void add(Component component) {
            layout.add(component);
            components.add(component);
        }

        boolean remove(Component component) {
            if (components.remove(component)) {
                layout.remove(components);
                return true;
            }
            return false;
        }

        void removeAll() {
            for (var component : components) {
                layout.remove(component);
            }
            components.clear();
        }
    }

    private final List<Row> rows = new ArrayList<>();

    @Override
    public void add(Component... components) {
        for (var component : components) {
            addComponent(component);
        }
    }

    @Override
    public void add(Collection<Component> components) {
        for (var component : components) {
            addComponent(component);
        }
    }

    @Override
    public void remove(Component... components) {
        for (var component : components) {
            removeComponent(component);
        }
    }

    @Override
    public void remove(Collection<Component> components) {
        for (var component : components) {
            removeComponent(component);
        }
    }

    @Override
    public void removeAll() {
        var rows = new ArrayList<>(this.rows);
        this.rows.clear();

        for (var row : rows) {
            removeRow(row);
        }
    }

    @Override
    public void addComponentAtIndex(int index, Component component) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addComponentAsFirst(Component component) {
        throw new UnsupportedOperationException();
    }

    private void addComponent(Component component) {
        Row row;
        if (rows.isEmpty() || (row = rows.getLast()).size() >= 2) {
            row = addNewRow();
        }
        row.add(component);
    }

    private void removeComponent(Component component) {
        for (var row : rows) {
            if (row.remove(component)) {
                if (row.isEmpty()) {
                    removeRow(row);
                }
                return;
            }
        }
    }

    private Row addNewRow() {
        var ret = new Row();
        rows.add(ret);
        getElement().appendChild(ret.getElement());
        return ret;
    }

    private void removeRow(Row row) {
        row.removeAll();
        getElement().removeChild(row.getElement());
        rows.remove(row);
    }
}
