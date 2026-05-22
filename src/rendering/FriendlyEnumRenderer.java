package rendering;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import java.awt.Component;
import java.util.Locale;

public class FriendlyEnumRenderer<T extends Enum<T>> extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(
            JList<?> list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus
    ) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        if (value instanceof Enum<?> enumValue) {
            String text = enumValue.name().toLowerCase(Locale.ROOT).replace('_', ' ');
            setText(Character.toUpperCase(text.charAt(0)) + text.substring(1));
        }

        return this;
    }
}
