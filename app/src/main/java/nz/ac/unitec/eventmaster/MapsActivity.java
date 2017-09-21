package nz.ac.unitec.eventmaster;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;



public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private class StableArrayAdapter extends ArrayAdapter<String> {

        HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();

        public StableArrayAdapter(Context context, int textViewResourceId,
                                  List<String> objects) {
            super(context, textViewResourceId, objects);
            for (int i = 0; i < objects.size(); ++i) {
                mIdMap.put(objects.get(i), i);
            }
        }

        @Override
        public long getItemId(int position) {
            String item = getItem(position);
            return mIdMap.get(item);
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

    }

    private GoogleMap mMap;
    private double lat;
    private double lng;
    private String city;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


        final ListView listview = findViewById(R.id.list_view);
        final ArrayList<String> list = new ArrayList<String>();

        ((SlidingUpPanelLayout)findViewById(R.id.slMap)).setScrollableView(listview);

        final FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(getApplicationContext());
        final Geocoder gcd = new Geocoder(getApplicationContext(), Locale.getDefault());
        final RequestQueue queue = Volley.newRequestQueue(getApplicationContext());

        LocationRequest req = new LocationRequest();
        req.setInterval(10000); // 10 seconds
        req.setFastestInterval(5000); // 5 seconds
        req.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        client.requestLocationUpdates(req,new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Log.e("location:",locationResult.getLastLocation().toString());
            }
        },null);

        try {
            Task<Location> location = client.getLastLocation();

            location.addOnCompleteListener(new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    try {
                        lat = task.getResult().getLatitude();
                        lng = task.getResult().getLongitude();
                        LatLng marker = new LatLng(lat, lng);
                        mMap.addMarker(new MarkerOptions().position(marker).title("I am here!").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_map_pin_dark_red)));
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(marker, 10));
                    } catch (NullPointerException e) {
                        System.out.println("task = null");
                    }
                    System.out.println("lat = " + lat + ", lng = " + lng);

                    List<Address> addresses = null;
                    try {
                        addresses = gcd.getFromLocation(lat, lng, 1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    assert addresses != null;
                    if (addresses.size() > 0) {
                        city = addresses.get(0).getLocality();
                        System.out.println(city);
                        String url = "https://app.ticketmaster.com/discovery/v2/events.json?city=" + city + "&apikey=EGOkWNJ4TAr11J6KzrEETI4CzyprJVc3";
                        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                                    @Override
                                    public void onResponse(JSONObject response) {
                                        try {
                                            JSONObject embedded = response.getJSONObject("_embedded");
                                            JSONArray events = embedded.getJSONArray("events");
                                            int length = events.length();
                                            for (int index = 0; index < length; index++) {
                                                JSONObject event = events.getJSONObject(index);
                                                String eventName = event.getString("name");
                                                JSONObject dates = event.getJSONObject("dates");
                                                JSONObject start = dates.getJSONObject("start");
                                                String startDateTime = start.getString("dateTime");

                                                String currency = "";
                                                String minPrice = "";
                                                String maxPrice = "";
                                                try {
                                                    JSONArray priceRanges = event.getJSONArray("priceRanges");
                                                    JSONObject priceRange = priceRanges.getJSONObject(0);
                                                    currency = priceRange.getString("currency");
                                                    minPrice = priceRange.getString("min");
                                                    maxPrice = priceRange.getString("max");
                                                } catch (JSONException e) {

                                                }

                                                JSONObject embeddedEvent = event.getJSONObject("_embedded");
                                                JSONArray venues = embeddedEvent.getJSONArray("venues");
                                                int venuesLength = venues.length();
                                                for(int venuesIndex = 0; venuesIndex < venuesLength; venuesIndex++) {

                                                    JSONObject venue = venues.getJSONObject(venuesIndex);
                                                    JSONObject venueLocation = venue.getJSONObject("location");

                                                    double venueLng = Double.parseDouble(venueLocation.get("longitude").toString());
                                                    double venueLat = Double.parseDouble(venueLocation.get("latitude").toString());

                                                    if((venueLng != 0d) && (venueLat != 0d)) {
                                                        LatLng venueMarker = new LatLng(venueLat, venueLng);
                                                        mMap.addMarker(new MarkerOptions().position(venueMarker).title(eventName).snippet(startDateTime + " " + minPrice + currency + " - " + maxPrice + currency).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_map_pin_azure)));
                                                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(venueMarker, 10));
                                                        list.add(eventName + " (price from " + minPrice + currency + " to " + maxPrice + currency + ", start at " + startDateTime + ")");
                                                    }
                                                }
                                            }

                                            final StableArrayAdapter adapter = new StableArrayAdapter(getApplicationContext(), android.R.layout.simple_list_item_1, list);
                                            listview.setAdapter(adapter);

                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        } finally {

                                        }
                                    }
                                }, new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {

                                    }
                                });

                        queue.add(jsObjRequest);
                    }
                }
            });
        } catch(SecurityException ex) {
            ex.printStackTrace();
        }
    }
}
