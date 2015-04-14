package newfiles;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class KillButton extends BaseButton{
	private static final long serialVersionUID = 1L;
	
	KillButton(Frame win, InfoStream is){
		this("",win, is);
	}
	
	KillButton(String s,Frame win, InfoStream is){
		super(s);
		this.addActionListener(new ActionListener(){	    
			 @Override
				public void actionPerformed(ActionEvent e){
					win.dispose();
					try{
						is.close();
					}catch(Exception ioe){
						
					}
					System.exit(0);
				}
			});
	}
	
}