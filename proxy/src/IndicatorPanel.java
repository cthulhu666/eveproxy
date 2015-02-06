import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by mgruszecki on 11.01.15.
 */
public class IndicatorPanel extends JPanel {
    private final AtomicBoolean state;
    private final Color activeColor;

    public IndicatorPanel(AtomicBoolean state, Color activeColor, String label) {
        this.state = state;
        this.activeColor = activeColor;
        JLabel jLabel = new JLabel();
        jLabel.setText(label);
        this.add(jLabel);
    }

    public void reset() {
        state.set(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        setBackground(state.get() ? activeColor : Color.BLACK);
        super.paintComponent(g);
    }

}
