package newfiles;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CloseButton extends BaseButton{
	private static final long serialVersionUID = 1L;
	
	CloseButton(Frame win){
		this("",win);
	}
	
	CloseButton(String s,Frame win){
		super(s);
		this.addActionListener(new ActionListener(){	    
			 @Override
				public void actionPerformed(ActionEvent e){
					win.dispose();
				}
			});
	}
	
}
