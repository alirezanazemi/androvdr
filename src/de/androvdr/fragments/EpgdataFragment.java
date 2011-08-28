package de.androvdr.fragments;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.LinearLayout;
import de.androvdr.Preferences;
import de.androvdr.R;
import de.androvdr.activities.AbstractFragmentActivity;
import de.androvdr.controllers.EpgdataController;

public class EpgdataFragment extends AbstractFragment {
	private AbstractFragmentActivity mActivity;
	private EpgdataController mController;
	
	public static EpgdataFragment newInstance(int channelNumber) {
		EpgdataFragment f = new EpgdataFragment();
		Bundle bundle = new Bundle();
		bundle.putInt("channelnumber", channelNumber);
		f.setArguments(bundle);
		return f;
	}
	
	public int getChannelNumber() {
		return getArguments().getInt("channelnumber");
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mActivity = (AbstractFragmentActivity) activity;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		LinearLayout view = (LinearLayout) mActivity.findViewById(R.id.pgi);
		
		if (view == null)
			return;
		
		/*
		 * setTheme doesn't change background color :(
		 */
		if (! ((AbstractFragmentActivity) getActivity()).isDualPane())
			if (Preferences.blackOnWhite && view.getBackground() == null)
				view.setBackgroundColor(Color.WHITE);
		
		mController = new EpgdataController(mActivity, mActivity.getHandler(), 
				view, getChannelNumber());
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = mActivity.getMenuInflater();
		inflater.inflate(R.menu.epg_menu, menu);
		menu.setHeaderTitle(mController.getTitle());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.epgdata_fragment, container, false);
		registerForContextMenu(root);
		
		if (((AbstractFragmentActivity) getActivity()).isDualPane()) {
			View header = root.findViewById(R.id.pgi_header);
			if (header != null)
				header.setVisibility(View.GONE);
		}

		return root;
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.epg_record:
			mController.action(EpgdataController.EPGDATA_ACTION_RECORD);
			return true;
		}
		return false;
	}
}