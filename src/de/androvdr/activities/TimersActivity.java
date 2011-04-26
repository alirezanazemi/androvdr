/*
 * Copyright (c) 2010-2011 by androvdr <androvdr@googlemail.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * For more information on the GPL, please go to:
 * http://www.gnu.org/copyleft/gpl.html
 */

package de.androvdr.activities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import de.androvdr.EpgSearch;
import de.androvdr.Preferences;
import de.androvdr.R;
import de.androvdr.controllers.TimerController;

public class TimersActivity extends AbstractListActivity {
	private static transient Logger logger = LoggerFactory.getLogger(TimersActivity.class);
	private static final int HEADER_TEXT_SIZE = 15;
	
	private TimerController mController;
	private boolean mIsSearch = false;
	private ListView mListView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.timers);

		TextView tv = (TextView) findViewById(R.id.timers_header);
		tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP,	HEADER_TEXT_SIZE + Preferences.textSizeOffset);
		
		logger.trace("onCreate");
	    mListView = (ListView) findViewById(android.R.id.list);

		/*
		 * setTheme doesn't change background color :(
		 */
		if (Preferences.blackOnWhite)
			mListView.setBackgroundColor(Color.WHITE);
		
		/*
		 * perform epgsearch ?
		 */
		EpgSearch epgSearch = null;
	    Intent intent = getIntent();
	    if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
	      String query = intent.getStringExtra(SearchManager.QUERY);
	      epgSearch = new EpgSearch();
	      epgSearch.search = query.trim();
	      epgSearch.inTitle = Preferences.epgsearch_title;
	      epgSearch.inSubtitle = Preferences.epgsearch_subtitle;
	      epgSearch.inDescription = Preferences.epgsearch_description;
	      tv.setText(epgSearch.search);
	      mIsSearch = true;
	    }

		mController = new TimerController(this, handler, mListView, epgSearch);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
		case R.id.timer_overview:
			mController.action(TimerController.TIMER_ACTION_PROGRAMINFOS, info.position);
			return true;
		case R.id.timer_overviewfull:
			mController.action(TimerController.TIMER_ACTION_PROGRAMINFOS_ALL, info.position);
			return true;
		case R.id.timer_delete:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.timer_delete_timer)
			       .setCancelable(false)
			       .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			        	   dialog.dismiss();
			        	   mController.action(TimerController.TIMER_ACTION_DELETE, info.position);
			           }
			       })
			       .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			                dialog.cancel();
			           }
			       });
			AlertDialog alert = builder.create();
			alert.show();
			return true;
		case R.id.timer_toggle:
			mController.action(TimerController.TIMER_ACTION_TOGGLE, info.position);
			return true;
		case R.id.timer_record:
			mController.action(TimerController.TIMER_ACTION_RECORD, info.position);
			return true;
		case R.id.timer_switch:
			mController.action(TimerController.TIMER_ACTION_SWITCH_CAHNNEL, info.position);
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		AdapterContextMenuInfo mi = (AdapterContextMenuInfo) menuInfo;
		MenuInflater inflater = getMenuInflater();
		if (mIsSearch)
			inflater.inflate(R.menu.timers_menu_search, menu);
		else
			inflater.inflate(R.menu.timers_menu, menu);
		menu.setHeaderTitle(mController.getTitle(mi.position));
	}
}
