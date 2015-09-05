package tomthomas.nearby;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import javax.net.ssl.HttpsURLConnection;


/**
 * A placeholder fragment containing a simple view.
 */
public class TaskFragment extends Fragment implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener,
        AdapterView.OnItemSelectedListener, GoogleMap.OnMarkerClickListener {

    private GoogleApiClient mGoogleApiClient = null;
    private Location mLastLocation = null;
    private GoogleMap map = null;
    private LocationRequest mLocationRequest = null;
    private Marker[] previousMarkers = null;
    private ArrayList<Marker> previousMarkers2 = null;
    private MarkerOptions[] previousPlaces = null;
    private ArrayList<MarkerOptions> previousPlaces2 = null;
    private float zoomLevel = 13;  //up to 21
    private double myLatitude, myLongitude;
    private boolean moreResults = false;
    private boolean needsUpdate = false;
    boolean activityNeedsUpdate = false;
    boolean safeToUpdate = true;
    int spinnerInitCount = 4;

    private final int MAX_PLACES = 60;
    private final float smallestDisplacement = 200;
    private final String placesAPIKey = "&key=AIzaSyDE1q5ngIV8Nx7bN4US_W-o5Moe4sAzLVw";
    private final String baseURL = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?";
    private final String placeBaseURL = "https://maps.googleapis.com/maps/api/place/details/json?placeid=";
    private String placesOrder = "&rankby=prominence";  //if rankby == distance, must not use radius parameter
    private String placesRadius = "&radius=3000"; //in meters
    private String placesTypes = "&types=";
    private String pagetoken = "";

    private AsyncTask<String, Void, String> mPlacesTask = null;
    private HashMap<String, Integer> mIcons = null;

    String[] filterTypes = {"food", "cafe", "bar", "movie_theater", "night_club", "bowling_alley", "amusement_park", "museum", "casino", "zoo",
            "airport", "aquarium", "art_gallery", "atm", "bakery", "bank", "beauty_salon", "book_store", "campground", "church", "clothing_store",
            "convenience_store", "department_store", "doctor", "electronics_store", "florist", "gas_station", "grocery_or_supermarket",
            "gym", "health", "hindu_temple", "hospital", "library", "lodging", "meal_delivery", "meal_takeaway", "mosque",
            "movie_rental", "park", "pharmacy", "police", "school", "shopping_mall", "spa", "stadium", "synagogue", "university"};

    String[] defaultSelection = {"food", "cafe", "bar", "movie_theater", "night_club", "bowling_alley", "amusement_park", "museum", "casino", "zoo"};

    ArrayList<String> selectedFilters;

    public TaskFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setRetainInstance(true);

        previousMarkers2 = new ArrayList<>();
        previousPlaces2 = new ArrayList<>();

        selectedFilters = new ArrayList<>();
        for(String s: defaultSelection)
        {
            selectedFilters.add(s);
        }

        buildGoogleApiClient();
        mGoogleApiClient.connect();
        buildIconsMap();
        map = ((SupportMapFragment)getActivity().getSupportFragmentManager().findFragmentById(R.id.ourMap)).getMap();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_task, container, false);
    }

    private void buildIconsMap()
    {
        mIcons = new HashMap<String, Integer>();

        mIcons.put("https://maps.gstatic.com/mapfiles/place_api/icons/airport-71.png", R.drawable.airport_71);
        mIcons.put("https://maps.gstatic.com/mapfiles/place_api/icons/aquarium-71.png", R.drawable.aquarium_71);
        mIcons.put("https://maps.gstatic.com/mapfiles/place_api/icons/art_gallery-71.png", R.drawable.art_gallery_71);
        mIcons.put("https://maps.gstatic.com/mapfiles/place_api/icons/bar-71.png", R.drawable.bar_71);
        mIcons.put("https://maps.gstatic.com/mapfiles/place_api/icons/bowling-71.png", R.drawable.bowling_71);
        mIcons.put("https://maps.gstatic.com/mapfiles/place_api/icons/cafe-71.png", R.drawable.cafe_71);
        mIcons.put("https://maps.gstatic.com/mapfiles/place_api/icons/camping-71.png", R.drawable.camping_71);
        mIcons.put("https://maps.gstatic.com/mapfiles/place_api/icons/casino-71.png", R.drawable.casino_71);
        mIcons.put("https://maps.gstatic.com/mapfiles/place_api/icons/dentist-71.png", R.drawable.dentist_71);
        mIcons.put("https://maps.gstatic.com/mapfiles/place_api/icons/doctor-71.png", R.drawable.doctor_71);
        mIcons.put("https://maps.gstatic.com/mapfiles/place_api/icons/fitness-71.png", R.drawable.fitness_71);
        mIcons.put("https://maps.gstatic.com/mapfiles/place_api/icons/gas_station-71.png", R.drawable.gas_station_71);
        mIcons.put("https://maps.gstatic.com/mapfiles/place_api/icons/generic_business-71.png", R.drawable.generic_business_71);
        mIcons.put("https://maps.gstatic.com/mapfiles/place_api/icons/generic_recreational-71.png", R.drawable.generic_recreational_71);
        mIcons.put("https://maps.gstatic.com/mapfiles/place_api/icons/library-71.png", R.drawable.library_71);
        mIcons.put("https://maps.gstatic.com/mapfiles/place_api/icons/lodging-71.png", R.drawable.lodging_71);
        mIcons.put("https://maps.gstatic.com/mapfiles/place_api/icons/movies-71.png", R.drawable.movies_71);
        mIcons.put("https://maps.gstatic.com/mapfiles/place_api/icons/museum-71.png", R.drawable.museum_71);
        mIcons.put("https://maps.gstatic.com/mapfiles/place_api/icons/police-71.png", R.drawable.police_71);
        mIcons.put("https://maps.gstatic.com/mapfiles/place_api/icons/restaurant-71.png", R.drawable.restaurant_71);
        mIcons.put("https://maps.gstatic.com/mapfiles/place_api/icons/shopping-71.png", R.drawable.shopping_71);
        mIcons.put("https://maps.gstatic.com/mapfiles/place_api/icons/stadium-71.png", R.drawable.stadium_71);
        mIcons.put("https://maps.gstatic.com/mapfiles/place_api/icons/wine-71.png", R.drawable.wine_71);
        mIcons.put("https://maps.gstatic.com/mapfiles/place_api/icons/worship_general-71.png", R.drawable.worship_general_71);
        mIcons.put("https://maps.gstatic.com/mapfiles/place_api/icons/worship_hindu-71.png", R.drawable.worship_hindu_71);
        mIcons.put("https://maps.gstatic.com/mapfiles/place_api/icons/worship_islam-71.png", R.drawable.worship_islam_71);
        mIcons.put("https://maps.gstatic.com/mapfiles/place_api/icons/worship_jewish-71.png", R.drawable.worship_jewish_71);
        mIcons.put("https://maps.gstatic.com/mapfiles/place_api/icons/zoo-71.png", R.drawable.zoo_71);
    }

    private void updateLocation(Marker[] placeMarkers, MarkerOptions[] places) {
        if (previousMarkers != null) {
            for (int i = 0; i < previousMarkers.length; i++) {
                if (previousMarkers[i] != null)
                    previousMarkers[i].remove();
            }
        }

        LatLng myLatLng = new LatLng(myLatitude, myLongitude);
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, zoomLevel), 2000, null);

        if (moreResults) {
            if (places != null && placeMarkers != null) {
                for (int place = 0; place < places.length && place < placeMarkers.length; place++) {
                    if (places[place] != null) {
                        placeMarkers[place] = map.addMarker(places[place]);
                        previousMarkers2.add(placeMarkers[place]);
                        previousPlaces2.add(places[place]);
                    }
                }
            }

            previousMarkers = new Marker[previousMarkers2.size()];
            for (int i = 0; i < previousMarkers.length; i++) {
                previousMarkers[i] = previousMarkers2.get(i);
            }

            previousPlaces = new MarkerOptions[previousPlaces2.size()];
            for (int i = 0; i < previousPlaces.length; i++) {
                previousPlaces[i] = previousPlaces2.get(i);
            }

            moreResults = !moreResults;
            previousMarkers2.clear();
            previousPlaces2.clear();
        } else {
            if (places != null && placeMarkers != null) {
                for (int place = 0; place < places.length && place < placeMarkers.length; place++) {
                    if (places[place] != null) {
                        placeMarkers[place] = map.addMarker(places[place]);
                    }
                }
            }

            previousMarkers = placeMarkers;
            previousPlaces = places;
        }

        safeToUpdate = true;

        if(needsUpdate)
        {
            needsUpdate = false;
            String placesUrl = buildQueryUrl();
            startPlacesTask(placesUrl);
        }

        if (activityNeedsUpdate) {
            activityNeedsUpdate = false;
            activityChanged();
        }
    }

    private void getMoreResults(Marker[] placeMarkers, MarkerOptions[] places)
    {
        //safeToUpdate = false;
        if(previousMarkers != null){
            for(int i = 0; i < previousMarkers.length; i++){
                if(previousMarkers[i] != null)
                    previousMarkers[i].remove();
            }
        }

        LatLng myLatLng = new LatLng(myLatitude, myLongitude);
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, zoomLevel), 2000, null);

        if(places != null && placeMarkers != null){
            for(int place = 0; place < places.length && place < placeMarkers.length; place++){
                if(places[place] != null) {
                    placeMarkers[place] = map.addMarker(places[place]);
                    previousMarkers2.add(placeMarkers[place]);
                    previousPlaces2.add(places[place]);
                }
            }
        }

        String placesUrl = buildQueryUrl();
        startPlacesTask(placesUrl);
    }

    public void activityChanged()
    {
        map = ((SupportMapFragment)getActivity().getSupportFragmentManager().findFragmentById(R.id.ourMap)).getMap();
        updateLocation(previousMarkers, previousPlaces);
    }

    public void filtersUpdated(ArrayList<String> selection)
    {
        selectedFilters = selection;

        if(!safeToUpdate) {
            needsUpdate = true;
            return;
        }

        else
        {
            String placesUrl = buildQueryUrl();
            startPlacesTask(placesUrl);
        }
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(30000);
        mLocationRequest.setFastestInterval(30000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(smallestDisplacement);
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);

        if(mLastLocation == null)
        {
            Toast t = Toast.makeText(getActivity(), "Can't access location!", Toast.LENGTH_SHORT);
            t.show();
        }

        else
        {
            myLatitude = mLastLocation.getLatitude();
            myLongitude = mLastLocation.getLongitude();
        }

        createLocationRequest();
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        myLatitude = mLastLocation.getLatitude();
        myLongitude = mLastLocation.getLongitude();

        String placesUrl = buildQueryUrl();
        startPlacesTask(placesUrl);
    }

    public boolean onMarkerClick(Marker m)
    {
        String placeId = m.getTitle();
        new PlacesDetailTask().execute(placeBaseURL + placeId + placesAPIKey);
        return true;
    }

    private synchronized String buildQueryUrl()
    {
        String location = "&location=" + String.valueOf(myLatitude) + "," + String.valueOf(myLongitude);
        String types = "";
        for(String s: selectedFilters)
        {
            types += "|" + s;
        }
        if(!types.equals("")) {
            types = types.substring(1);
        }
        types = placesTypes + types;

        if(!pagetoken.contains("pagetoken="))
            pagetoken = "pagetoken=" + pagetoken;

        String placesUrl;

        if(placesOrder.equals("&rankby=distance")) {
            placesUrl = baseURL
                    + pagetoken
                    + location
                    + placesOrder
                    + types
                    + placesAPIKey;
        }

        else {
            placesUrl = baseURL
                    + pagetoken
                    + location
                    + placesOrder
                    + placesRadius
                    + types
                    + placesAPIKey;
        }

        return placesUrl;
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection has been interrupted.
        // Disable any UI components that depend on Google APIs
        // until onConnected() is called.
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // This callback is important for handling errors that
        // may occur while attempting to connect with Google.
        //
        // More about this in the 'Handle Connection Failures' section.
    }

    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        if(spinnerInitCount != 0) {
            spinnerInitCount--;
            return;
        }

        if(parent.getItemAtPosition(pos).equals("Distance")) {
            placesOrder = "&rankby=distance";
            zoomLevel = 14;
            ((MainActivity)getActivity()).disableRadiusSpinner();
        }
        else if (parent.getItemAtPosition(pos).equals("Ranking")) {
            placesOrder = "&rankby=prominence";
            if(placesRadius.equals("&radius=10000"))
                zoomLevel = 11;
            else if(placesRadius.equals("&radius=5000"))
                zoomLevel = 12;
            else if(placesRadius.equals("&radius=3000"))
                zoomLevel = 13;
            else
                zoomLevel = 14;

            ((MainActivity)getActivity()).enableRadiusSpinner();
        }
        else if (parent.getItemAtPosition(pos).equals("10000m")) {
            placesRadius = "&radius=10000";
            zoomLevel = 11;
        }
        else if (parent.getItemAtPosition(pos).equals("5000m")) {
            placesRadius = "&radius=5000";
            zoomLevel = 12;
        }
        else if (parent.getItemAtPosition(pos).equals("3000m")) {
            placesRadius = "&radius=3000";
            zoomLevel = 13;
        }
        else if (parent.getItemAtPosition(pos).equals("1000m")) {
            placesRadius = "&radius=1000";
            zoomLevel = 14;
        }

        if(!safeToUpdate) {
            needsUpdate = true;
            return;
        }

        else
        {
            String placesUrl = buildQueryUrl();
            startPlacesTask(placesUrl);
        }
    }

    public void onItemReselected(AdapterView<?> parent, View view, int pos, long id)
    {

    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }

    private synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(getActivity(), this, this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .build();
    }

    private synchronized void startPlacesTask(String placesUrl) {
        if(mPlacesTask != null) {
            mPlacesTask.cancel(true);
        }

        mPlacesTask = new PlacesTask().execute(placesUrl);
    }

    public class PlacesTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... placesURL) {

            if(moreResults)
                SystemClock.sleep(2000);

            StringBuilder placesBuilder = new StringBuilder();

            for (String placeUrl : placesURL) {
                Log.v("TaskFragment", placeUrl);
                try
                {
                    URL url = new URL(placeUrl);
                    HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.connect();

                    if(urlConnection.getResponseCode() == 200)
                    {
                        InputStream placesContent = urlConnection.getInputStream();
                        InputStreamReader placesInput = new InputStreamReader(placesContent);
                        BufferedReader placesReader = new BufferedReader(placesInput);
                        String line;

                        while ((line = placesReader.readLine()) != null) {
                            if(isCancelled())
                            {
                                return null;
                            }
                            placesBuilder.append(line);
                        }
                    }

                    //further implement error handling
                    else
                    {
                        Log.v("TaskFragment", "could not connect");
                    }
                }

                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }

            return placesBuilder.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            safeToUpdate = false;
            MarkerOptions[] places = null;
            Marker[] placeMarkers = new Marker[MAX_PLACES];
            try {
                //parse JSON

                JSONObject resultObject = new JSONObject(result);

                try {
                    pagetoken = resultObject.getString("next_page_token");
                    moreResults = true;
                }
                catch (Exception e) {
                    pagetoken = "";
                }

                JSONArray placesArray = resultObject.getJSONArray("results");
                places = new MarkerOptions[placesArray.length()];

                for (int place = 0; place < placesArray.length(); place++) {
                    boolean incomplete; //is place info incomplete?
                    LatLng placeCoordinates = null;
                    String placeId = "";
                    int icon = R.drawable.generic_business_71;

                    try{
                        incomplete = false;
                        JSONObject placeObject = placesArray.getJSONObject(place);
                        JSONObject loc = placeObject.getJSONObject("geometry").getJSONObject("location");

                        placeCoordinates = new LatLng(Double.valueOf(loc.getString("lat")), Double.valueOf(loc.getString("lng")));
                        placeId = placeObject.getString("place_id");

                        if(mIcons.get(placeObject.getString("icon")) != null) {
                            icon = mIcons.get(placeObject.getString("icon"));
                        }
                    }

                    catch(JSONException jse){
                        Log.v("PLACES", "missing value");
                        incomplete = true;
                        jse.printStackTrace();
                    }

                    if(incomplete)
                        places[place] = null;

                    else {
                        places[place] = new MarkerOptions()
                                .position(placeCoordinates)
                                .title(placeId)
                                .icon(BitmapDescriptorFactory.fromResource(icon));
                    }
                }

                if(moreResults && !pagetoken.equals(""))
                    getMoreResults(placeMarkers, places);
                else
                    updateLocation(placeMarkers, places);
            }

            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public class PlacesDetailTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... placesURL) {
            StringBuilder placesBuilder = new StringBuilder();

            for (String placeUrl : placesURL) {
                Log.v("PlacesDetailTask", placeUrl);
                try
                {
                    URL url = new URL(placeUrl);
                    HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.connect();

                    if(urlConnection.getResponseCode() == 200)
                    {
                        InputStream placesContent = urlConnection.getInputStream();
                        InputStreamReader placesInput = new InputStreamReader(placesContent);
                        BufferedReader placesReader = new BufferedReader(placesInput);
                        String line;

                        while ((line = placesReader.readLine()) != null) {
                            if(isCancelled())
                            {
                                return null;
                            }
                            placesBuilder.append(line);
                        }
                    }

                    //further implement error handling
                    else
                    {
                        Log.v("TaskFragment", "could not connect");
                    }
                }

                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }

            return placesBuilder.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                //parse JSON

                JSONObject resultObject = new JSONObject(result);

                JSONObject place = resultObject.getJSONObject("result");

                String placeId = "";
                final StringBuffer placeName = new StringBuffer();
                final StringBuffer vicinity =  new StringBuffer();

                try {
                    placeName.append(place.getString("name"));
                    vicinity.append(place.getString("formatted_address"));
                    placeId = place.getString("place_id");
                } catch (JSONException jse) {
                    Log.v("PlacesDetailTask", "missing value");
                    jse.printStackTrace();
                }

                final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                final String pId = (placeId + " " + preferences.getString("Favorites", ""));

                LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View markerPopupRoot = inflater.inflate(R.layout.marker_popup, null);

                final String address = placeName.toString() + "," + vicinity.toString();

                Button directionsButton = (Button) markerPopupRoot.findViewById(R.id.markerDirections);
                directionsButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Uri endpoint = Uri.parse("geo:0,0?q=" + Uri.encode(address));
                        Intent mapIntent = new Intent(Intent.ACTION_VIEW, endpoint);
                        mapIntent.setPackage("com.google.android.apps.maps");
                        startActivity(mapIntent);
                    }});

                Button favButton = (Button) markerPopupRoot.findViewById(R.id.addToFav);
                favButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        preferences.edit().putString("Favorites", pId).commit();
                        Log.v("bad", pId);
                    }});

                Button calendarButton = (Button) markerPopupRoot.findViewById(R.id.markerAddCalendar);
                calendarButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent calendarIntent = new Intent(Intent.ACTION_INSERT)
                                .setData(CalendarContract.Events.CONTENT_URI)
                                .putExtra(CalendarContract.Events.TITLE, placeName.toString())
                                .putExtra(CalendarContract.Events.EVENT_LOCATION, vicinity.toString());
                        startActivity(calendarIntent);
                    }});

                TextView markerName = (TextView)markerPopupRoot.findViewById(R.id.marker_name);
                TextView markerAddr = (TextView)markerPopupRoot.findViewById(R.id.marker_address);

                markerName.setText(placeName);
                markerAddr.setText(vicinity);
                PopupWindow markerDetails = new PopupWindow(getActivity());
                markerDetails.setContentView(markerPopupRoot);
                markerDetails.setHeight(LinearLayout.LayoutParams.WRAP_CONTENT);
                markerDetails.setWidth(LinearLayout.LayoutParams.WRAP_CONTENT);
                markerDetails.setFocusable(true);
                markerDetails.setTouchable(true);
                markerDetails.showAtLocation(markerPopupRoot, Gravity.CENTER, 0, 0);

            } catch (Exception e) {
                Log.v("wooo", "wooooooooooooooo");
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String defFilters = "";
        for(String s: selectedFilters) {
            defFilters = defFilters + s + "," ;
        }
        preferences.edit().putString("defFilters", defFilters).commit();
        Log.v("selected Filters", defFilters);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        if(getActivity().isFinishing())
        {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
            mPlacesTask.cancel(true);


        }
    }
}