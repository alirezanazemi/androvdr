/*
 * www.jcraft.com/jsch/
 * 
 * Copyright (c) 2002,2003,2004,2005,2006,2007,2008 Atsuhiko Yamanaka, 
 * JCraft,Inc. All rights reserved. Redistribution and use in source and binary 
 * forms, with or without modification, are permitted provided that the 
 * following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this 
 * list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice, 
 * this list of conditions and the following disclaimer in the documentation 
 * and/or other materials provided with the distribution.
 * 3. The names of the authors may not be used to endorse or promote products 
 * derived from this software without specific prior written permission. THIS 
 * SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES, 
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND 
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL 
 * JCRAFT,INC. OR ANY CONTRIBUTORS TO THIS SOFTWARE BE LIABLE FOR ANY DIRECT, 
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND 
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */

package de.androvdr;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Handler;
import android.os.Message;
import android.widget.EditText;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

import de.androvdr.activities.AndroVDR;
import de.androvdr.devices.VdrDevice;

public class PortForwarding implements Runnable {
	
	private static final String TAG = "PortForwarding";
	// wird fuer die Kommunikation mit der GUI gebraucht
	public static volatile boolean guiFlag = false; // wird beim GUI-Dialogende auf true gesetzt
	public static String sshPassword;
	public static String guiMessage;
	public static boolean positiveButton;  // ist nach Dialogende true,wenn der positiveButton gedrueckt wurde
	
	private static Session session = null;
	
	static final int START_PROGRESS_DIALOG = 20 , STOP_PROGRESS_DIALOG = 10 ,
	                 PROMPT_PASSWORD = 0 , PROMPT_YES_NO = 1 , PROMPT_MESSAGE = 2;
	

	// Handler zum Benachrichtigen der GUI, wird beim Instanzieren gesetzt
	private Handler handler;
	
		
	// der Context der aufrufenden Klasse
	private static Context c;
	
	
	@SuppressWarnings("static-access")
	public PortForwarding(Handler h,Context c){
		Preferences.useInternet = false;
		session = null;
		guiFlag = false;
		positiveButton = false;
		this.handler = h;
		this.c = c;
		handler.sendEmptyMessage(START_PROGRESS_DIALOG);
		Thread thread = new Thread(this);
		thread.start();
	}
	
	// dieser Tread etabliert PortForwarding
	public void run() {
	    try{
	      JSch jsch=new JSch();
	      
	      VdrDevice vdr = Preferences.getVdr();
	      session=jsch.getSession(vdr.remote_user, vdr.remote_host, vdr.remote_port);
	      // session=jsch.getSession(Preferences.remoteUser, Preferences.remoteHost, Preferences.remotePort);
	      
	      // password will be given via UserInfo interface.
	      UserInfo ui=new MyUserInfo();
	      
	      session.setUserInfo(ui);
	      
	      session.connect();
	      
	      MyLog.v(TAG,"SSH-Session verbunden");

	      int assinged_port=session.setPortForwardingL(vdr.remote_local_port, "localhost",
	    		  vdr.getPort());
	      
	      MyLog.v(TAG,"localhost:"+assinged_port+" -> "+vdr.getIP()+":"+vdr.getPort());
	      
	      handler.sendEmptyMessage(STOP_PROGRESS_DIALOG); // alles OK, beende mit dieser Nachricht die Fortschrittsanzeige
	      MyLog.v(TAG,"PortForwarding eingerichtet");
	      Preferences.useInternet = true;
	    }
	    catch(Exception e){
	      MyLog.v(TAG,e.toString());
	      guiMessage = c.getString(R.string.portforwarding_fails)+e.toString();
	      guiFlag = false;
	      positiveButton = false;
	      // rufe GUI-Dialog promptMessage() auf
	      handler.sendEmptyMessage(PROMPT_MESSAGE);
	      // warte, bis GUI fertig ist
	      while(guiFlag == false);
	    }
	}
	
	
	public void disconnect(){
		if(session != null){
			if(session.isConnected()){
				session.disconnect();
				session = null;
				MyLog.v(TAG,"Session getrennt");
			}
		}
		Preferences.useInternet = false;
		AndroVDR.portForwarding = null;
	}
	
	// wird vom Handler der aufrufenden GUI-Klasse benutzt
    public static void sshDialogHandlerMessage(Message msg) {

    	switch(msg.what){
    	case PROMPT_PASSWORD:
    		promptPassword(c);
    		break;
    	case PROMPT_YES_NO:
    		promptYesNo(c);
    		break;
    	case PROMPT_MESSAGE:// Fehler beim Aktivieren von PortForwarding (Aufruf im Portforwarding-Thread)
    		promptMessage(c);
    		Preferences.useInternet = false;
      		//break; Kein break,hier. ProgressDialog auch beenden
    	case STOP_PROGRESS_DIALOG:// ProgressDialog beenden, Portforwarding ist aktiviert !
    		if(progressDialog != null)
    			progressDialog.dismiss();
    		break;
    	case START_PROGRESS_DIALOG:
    		//progressDialog = ProgressDialog.show(c, "", c.getString(R.string.starte_portforwarding),true,false);
    		
    		progressDialog = new ProgressDialog(c);
    		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    		progressDialog.setMessage(c.getString(R.string.start_portforwarding));
    		progressDialog.setCancelable(true);
    		progressDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, "Abbrechen", progressAbbruchListener);
    		progressDialog.show();
    		
    	}
    }

    // hier wird Portforwarding waehrend der Progressanzeige durch den User abgebrochen
    static OnClickListener progressAbbruchListener = new OnClickListener(){
		@Override
		public void onClick(DialogInterface dialog, int which) {
			// TODO Auto-generated method stub
			if(session != null){
				if(session.isConnected()){
					session.disconnect();
					MyLog.v(TAG,"Session durch Benutzer abgebrochen");
				}
			}
			Preferences.useInternet = false;
			AndroVDR.portForwarding = null;
		}
    	
    };
	
	
	// installiert Callback-Funktionen zur Interaktion mit dem GUI
	private class MyUserInfo implements UserInfo, UIKeyboardInteractive{
		
		@Override
		public String getPassphrase(){ // unbenutzt
			MyLog.v("MyUserInfo","getPassphrase");
			return null; 
		}
		
		@Override
		public boolean promptPassphrase(String arg0){ // unbenutzt
			MyLog.v("MyUserInfo","promptPassphrase");
			return true;
		}
		

		@Override
		public String getPassword(){ // liefert das Passwort zurueck
			return sshPassword; 
		}

		@Override
		public boolean promptPassword(String arg0) { // Aufruf GUI zum Eingeben des Passwortes
			MyLog.v(TAG,"promptPassword");
			
			guiFlag = false;
			positiveButton = false;
			// rufe GUI-Dialog promptPassword() auf
			handler.sendEmptyMessage(PROMPT_PASSWORD);
			// warte, bis Passwort eingegeben ist
	    	while(guiFlag == false);

			return positiveButton;  //true;
		}

		@Override
		public boolean promptYesNo(String s) {  // zeigt Host-Fingerprint an
			guiMessage = s;
			guiFlag = false;
			positiveButton = false;
			// rufe GUI-Dialog promptYesNo() auf
			handler.sendEmptyMessage(PROMPT_YES_NO);
			// warte, bis Fingerprint bestaetigt ist
	    	while(guiFlag == false);
			
			MyLog.v(TAG,"promptYesNo:"+s);
			
			return positiveButton;  //true;
		}

		@Override
		public void showMessage(String s) { // unbenutzt
			guiMessage = s;
			guiFlag = false;
			positiveButton = false;
			// rufe GUI-Dialog promptMessage() auf
			handler.sendEmptyMessage(PROMPT_MESSAGE);
			// warte, bis showMessage bestaetigt ist
	    	while(guiFlag == false);
			MyLog.v(TAG,"showMessage:"+s);
		}

		@Override   // unbenutzt
		public String[] promptKeyboardInteractive(String destination,String name,
                String instruction,String[] prompt,boolean[] echo) {
			StringBuffer s = new StringBuffer();
			MyLog.v("MyUserInfo:promptKey...","Name="+name);
			s.append(name+"\n");
			MyLog.v("MyUserInfo:promptKey...","Instruction="+instruction);
			s.append(instruction+"\n");
			for(int i=0;i < prompt.length;i++){
				MyLog.v("MyUserInfo:promptKey...",prompt[i]+"-"+echo[i]);
				s.append(prompt[i]+"-"+echo[i]+"\n");
			}
			guiMessage = s.toString();
			positiveButton = false;
			guiFlag = false;
			// rufe GUI-Dialog promptYesNo() auf
			handler.sendEmptyMessage(PROMPT_YES_NO);
			// warte, bis sendEmptyMessage bestaetigt ist
	    	while(guiFlag == false);
	    	
			return null;
		}
		
	}
	
	// ****************************************************************************
	// hier die GUI-Dialoge
	// ****************************************************************************
	
	private static ProgressDialog progressDialog;
	
	static void promptPassword(Context c){
		AlertDialog.Builder alert = new AlertDialog.Builder(c);  
		alert.setTitle(c.getString(R.string.pw_titel));  
		alert.setMessage(c.getString(R.string.pw_msg));  
		final EditText input = new EditText(c);  
		alert.setView(input);  
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {  
 		   public void onClick(DialogInterface dialog, int whichButton) {  
 			   sshPassword = input.getText().toString(); 
 			   positiveButton = true; // Flag zum feststellen, welcher Button gedrueckt wurde
 			   // setze Flag auf true, damit der Portforwarding-Thread weitermachen kann
 			   guiFlag = true;
  		   }  
 	   });  
 	   alert.setNegativeButton(c.getString(R.string.break_msg), new DialogInterface.OnClickListener() {  
 		   public void onClick(DialogInterface dialog, int whichButton) {  
 			   // Canceled. 
 			   // setze Flag auf true, damit der Portforwarding-Thread weitermachen kann
 			   guiFlag = true;
 		   }  
 	   });  
 	   alert.show();  
	}
	
	static void promptYesNo(Context c){
		AlertDialog.Builder alert = new AlertDialog.Builder(c);
		alert.setTitle(c.getString(R.string.warning));  
		alert.setMessage(guiMessage);  
		alert.setPositiveButton(c.getString(R.string.yes), new DialogInterface.OnClickListener() {  
 		   public void onClick(DialogInterface dialog, int whichButton) {  
 			   positiveButton = true; // Flag zum feststellen, welcher Button gedrueckt wurde
 			   // setze Flag auf true, damit der Portforwarding-Thread weitermachen kann
 			   guiFlag = true;
  		   }  
 	   });  
 	   alert.setNegativeButton(c.getString(R.string.no), new DialogInterface.OnClickListener() {  
 		   public void onClick(DialogInterface dialog, int whichButton) {  
 			   // Canceled. 
 			   // setze Flag auf true, damit der Portforwarding-Thread weitermachen kann
 			   guiFlag = true;
 		   }  
 	   });  
 	   alert.show();  
	}
	
	static void promptMessage(Context c){
		AlertDialog.Builder alert = new AlertDialog.Builder(c);
		alert.setTitle(c.getString(R.string.message));  
		alert.setMessage(guiMessage);  
		alert.setPositiveButton(c.getString(R.string.yes), new DialogInterface.OnClickListener() {  
 		   public void onClick(DialogInterface dialog, int whichButton) {  
 			  positiveButton = true; // Flag zum feststellen, welcher Button gedrueckt wurde
 			   // setze Flag auf true, damit der Portforwarding-Thread weitermachen kann
 			   guiFlag = true;
  		   }  
 	   });  
 	   alert.show();  
	}
	
	
}