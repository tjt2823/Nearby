package tomthomas.nearby;

import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private GoogleMap map = null;
    private FragmentManager fragMan;
    private TaskFragment taskFragment;
    private Spinner mapTypes;
    private ArrayAdapter<String> mapSpinnerAdapter;
    private Spinner placesRank;
    private ArrayAdapter<String> rankSpinnerAdapter;
    private Spinner placesRadius;
    private ArrayAdapter<String> radiusSpinnerAdapter;
    private MultiSelectionSpinner placesFilter;
    private String[] mNavigationItems = {"Nearby Places", "Favorite Places", "Upcoming Events", "Settings"};
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    private CharSequence mDrawerTitle;
    private CharSequence mTitle;



    int spinnerInitCount = 1;

    private final String TASK_FRAGMENT_TAG = "task";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerList.startLayoutAnimation();

        // Set the adapter for the list view
        mDrawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, mNavigationItems));
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener(this));

        mTitle = mDrawerTitle = getTitle();
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getSupportActionBar().setTitle(mTitle);
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getSupportActionBar().setTitle(mDrawerTitle);
            }
        };

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mDrawerToggle.syncState();

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        if(map == null)
        {
            map = ((SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.ourMap)).getMap();

            if(map != null)
            {
                map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                map.setMyLocationEnabled(true);

                UiSettings settings;
                settings = map.getUiSettings();
                settings.setMapToolbarEnabled(true);
                settings.setCompassEnabled(true);
                settings.setZoomControlsEnabled(true);
            }
        }

        fragMan = getSupportFragmentManager();
        taskFragment = (TaskFragment) fragMan.findFragmentByTag(TASK_FRAGMENT_TAG);

        if(taskFragment == null)
        {
            taskFragment = new TaskFragment();
            fragMan.beginTransaction().add(taskFragment, TASK_FRAGMENT_TAG).commit();
            spinnerInitCount = 1;
            taskFragment.spinnerInitCount = 4;
            map.setOnMarkerClickListener(taskFragment);

            String previousFilters = PreferenceManager.getDefaultSharedPreferences(this).getString("defFilters", "");
            if(!previousFilters.equals(""))
            {
                taskFragment.defaultSelection = previousFilters.split(",");
                Log.v("defSelection", previousFilters);
            }

            placesFilter = (MultiSelectionSpinner) findViewById(R.id.places_filter);
            placesFilter.setFragment(taskFragment);
            placesFilter.setItems(taskFragment.filterTypes);
            placesFilter.setSelection(taskFragment.defaultSelection);
            setUpSpinners();
        }

        else
        {
            spinnerInitCount = 1;
            taskFragment.spinnerInitCount = 4;
            map.setOnMarkerClickListener(taskFragment);

            placesFilter = (MultiSelectionSpinner) findViewById(R.id.places_filter);
            placesFilter.setFragment(taskFragment);
            placesFilter.setItems(taskFragment.filterTypes);
            placesFilter.setSelection(taskFragment.selectedFilters);
            setUpSpinners();

            if(!taskFragment.safeToUpdate) {
                taskFragment.activityNeedsUpdate = true;
                return;
            }
            taskFragment.activityChanged();
        }
    }

    private void setUpSpinners()
    {
        mapTypes = (Spinner) findViewById(R.id.map_spinner);
        mapSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        mapSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mapSpinnerAdapter.add("Normal");
        mapSpinnerAdapter.add("Satellite");
        mapTypes.setAdapter(mapSpinnerAdapter);
        mapTypes.setOnItemSelectedListener(this);

        placesRank = (Spinner) findViewById(R.id.places_rank);
        rankSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        rankSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        rankSpinnerAdapter.add("Distance");
        rankSpinnerAdapter.add("Ranking");
        placesRank.setAdapter(rankSpinnerAdapter);
        placesRank.setSelection(rankSpinnerAdapter.getPosition("Ranking"));
        placesRank.setOnItemSelectedListener(taskFragment);

        placesRadius = (Spinner) findViewById(R.id.places_radius);
        radiusSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        radiusSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        radiusSpinnerAdapter.add("10000m");
        radiusSpinnerAdapter.add("5000m");
        radiusSpinnerAdapter.add("3000m");
        radiusSpinnerAdapter.add("1000m");
        placesRadius.setAdapter(radiusSpinnerAdapter);
        placesRadius.setSelection(radiusSpinnerAdapter.getPosition("3000m"));
        placesRadius.setOnItemSelectedListener(taskFragment);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle your other action bar items...

        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_main, menu);
        return false;
    }

    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        if(spinnerInitCount != 0) {
            spinnerInitCount--;
            return;
        }

        if(parent.getItemAtPosition(pos).equals("Satellite"))
            map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        else
            map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }

    public void disableRadiusSpinner()
    {
        placesRadius.setEnabled(false);
        placesRadius.getSelectedView().setEnabled(false);
    }

    public void enableRadiusSpinner()
    {
        placesRadius.setEnabled(true);
        placesRadius.getSelectedView().setEnabled(true);
    }

}
