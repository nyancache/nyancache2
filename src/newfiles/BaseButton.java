package newfiles;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.ImageObserver;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicButtonUI;

public class BaseButton extends JButton{
	
	private static final long serialVersionUID = 1L;

	BaseButton(String s){
		super(s);
		this.setUI(BaseButtonUI.createUI(this));
		
	}

	public static class BaseButtonUI extends BasicButtonUI implements java.io.Serializable,
    MouseListener, KeyListener {

		private static final long serialVersionUID = 1L;

private final static BaseButtonUI m_buttonUI = new BaseButtonUI();

  protected Border m_borderRaised = UIManager.getBorder("Button.border");

  protected Border m_borderLowered = UIManager
      .getBorder("Button.borderPressed");

  protected Color m_backgroundNormal = UIManager
      .getColor("Button.background");

  protected Color m_backgroundPressed = UIManager
      .getColor("Button.pressedBackground");

  protected Color m_foregroundNormal = UIManager
      .getColor("Button.foreground");

  protected Color m_foregroundActive = UIManager
      .getColor("Button.activeForeground");

  protected Color m_focusBorder = UIManager.getColor("Button.focusBorder");

  public static ComponentUI createUI(JComponent c) {
    return m_buttonUI;
  }

  public void installUI(JComponent c) {
    super.installUI(c);

    c.addMouseListener(this);
    c.addKeyListener(this);
  }

  public void uninstallUI(JComponent c) {
    super.uninstallUI(c);
    c.removeMouseListener(this);
    c.removeKeyListener(this);
  }

  public void paint(Graphics g, JComponent c) {
    AbstractButton b = (AbstractButton) c;
    Dimension d = b.getSize();
    Icon i = b.getIcon();

    g.setFont(c.getFont());
    FontMetrics fm = g.getFontMetrics();

    g.setColor(b.getForeground());
    String caption = b.getText();
    int x = (d.width - fm.stringWidth(caption)) / 2;
    int y = (d.height + fm.getAscent()) / 2;
    g.drawString(caption, x, y);
    
    if(i != null){
    	g.drawImage(((ImageIcon) i).getImage(), 5, 4, i.getIconWidth(), i.getIconHeight(), null);
    }
    
  }

  public Dimension getPreferredSize(JComponent c) {
    Dimension d = super.getPreferredSize(c);
    if (m_borderRaised != null) {
      Insets ins = m_borderRaised.getBorderInsets(c);
      d.setSize(d.width + ins.left + ins.right, d.height + ins.top
          + ins.bottom);
    }
    return d;
  }

  public void mouseClicked(MouseEvent e) {
  }

  public void mousePressed(MouseEvent e) {
    JComponent c = (JComponent) e.getComponent();
    c.setBorder(m_borderLowered);
    c.setBackground(m_backgroundPressed);
  }

  public void mouseReleased(MouseEvent e) {
    JComponent c = (JComponent) e.getComponent();
    c.setBorder(m_borderRaised);
    c.setBackground(m_backgroundNormal);
  }

  public void mouseEntered(MouseEvent e) {
    JComponent c = (JComponent) e.getComponent();
    c.setForeground(m_foregroundActive);
    c.repaint();
  }

  public void mouseExited(MouseEvent e) {
    JComponent c = (JComponent) e.getComponent();
    c.setForeground(m_foregroundNormal);
    c.repaint();
  }

  public void keyTyped(KeyEvent e) {
  }

  public void keyPressed(KeyEvent e) {
    int code = e.getKeyCode();
    if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_SPACE) {
      JComponent c = (JComponent) e.getComponent();
      c.setBorder(m_borderLowered);
      c.setBackground(m_backgroundPressed);
    }
  }

  public void keyReleased(KeyEvent e) {
    int code = e.getKeyCode();
    if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_SPACE) {
      JComponent c = (JComponent) e.getComponent();
      c.setBorder(m_borderRaised);
      c.setBackground(m_backgroundNormal);
    }
  }
  
}
	public void makeAlert(){
		ImageIcon icon = new ImageIcon("newfiles/error.png");
		System.out.println(icon.getIconHeight() + "" + icon.getIconWidth());
		System.out.println(icon.getImageLoadStatus()==java.awt.MediaTracker.COMPLETE);
		this.setIcon(icon);
	}
	
	public void removeAlert(){
		this.setIcon(null);
	}
}
