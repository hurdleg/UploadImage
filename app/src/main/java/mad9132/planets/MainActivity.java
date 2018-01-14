package mad9132.planets;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import mad9132.planets.model.PlanetPOJO;
import mad9132.planets.services.MyService;
import mad9132.planets.services.UploadImageFileService;
import mad9132.planets.utils.HttpMethod;
import mad9132.planets.utils.NetworkHelper;
import mad9132.planets.utils.RequestPackage;

/**
 * CRUD app for the Planets:
 *   Create a new planet (Pluto) on the web service
 *   Read/Retrieve - get collection (array) of planets from the web service
 *   Update planet Pluto on the web service
 *   Delete planet Pluto on the web service
 *   Upload an image of Pluto
 *
 * @author Gerald.Hurdle@AlgonquinCollege.com
 *
 * Reference: Chapter 5. Sending Data to Web Services
 *            "Android App Development: RESTful Web Services" with David Gassner
 */
public class MainActivity extends Activity {
    private static final Boolean IS_LOCALHOST;
    private static final String  JSON_URL;
    private static final String  TAG = "CRUD";

    private PlanetAdapter mPlanetAdapter;
    private RecyclerView  mRecyclerView;

    static {
        IS_LOCALHOST = false;
        JSON_URL = IS_LOCALHOST ? "http://10.0.2.2:3000/planets" : "https://planets.mybluemix.net/planets";
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra(MyService.MY_SERVICE_PAYLOAD)) {
                PlanetPOJO[] planetsArray = (PlanetPOJO[]) intent
                        .getParcelableArrayExtra(MyService.MY_SERVICE_PAYLOAD);
                Toast.makeText(MainActivity.this,
                        "Received " + planetsArray.length + " planets from service",
                        Toast.LENGTH_SHORT).show();

                mPlanetAdapter.setPlanets(planetsArray);
            } else if (intent.hasExtra(MyService.MY_SERVICE_RESPONSE)) {
                String response = intent.getStringExtra(MyService.MY_SERVICE_RESPONSE);
                Toast.makeText(context, response, Toast.LENGTH_SHORT).show();
                // the planet data has changed on the web service
                // fetch fetch all the planet data, and re-fresh the list
                getPlanets();
            } else if (intent.hasExtra(MyService.MY_SERVICE_EXCEPTION)) {
                String message = intent.getStringExtra(MyService.MY_SERVICE_EXCEPTION);
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }

            if (intent.hasExtra(UploadImageFileService.UPLOAD_IMAGE_FILE_SERVICE_RESPONSE)) {
                String response = intent.getStringExtra(UploadImageFileService.UPLOAD_IMAGE_FILE_SERVICE_RESPONSE);
                Toast.makeText(context, response, Toast.LENGTH_SHORT).show();
                // the planet data has changed on the web service
                // fetch fetch all the planet data, and re-fresh the list
                getPlanets();
            } else if (intent.hasExtra(UploadImageFileService.UPLOAD_IMAGE_FILE_SERVICE_EXCEPTION)) {
                String message = intent.getStringExtra(UploadImageFileService.UPLOAD_IMAGE_FILE_SERVICE_EXCEPTION);
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize RecyclerView + Adapter
        mRecyclerView = (RecyclerView) findViewById(R.id.rvPlanets);
        mRecyclerView.setHasFixedSize( true );
        mPlanetAdapter = new PlanetAdapter( this );
        mRecyclerView.setAdapter( mPlanetAdapter );

        LocalBroadcastManager.getInstance(getApplicationContext())
                .registerReceiver(mBroadcastReceiver,
                        new IntentFilter(MyService.MY_SERVICE_MESSAGE));

        LocalBroadcastManager.getInstance(getApplicationContext())
                .registerReceiver(mBroadcastReceiver,
                        new IntentFilter(UploadImageFileService.UPLOAD_IMAGE_FILE_SEVICE_MESSAGE));

        if (NetworkHelper.hasNetworkAccess(this)) {
            getPlanets();
        } else {
            Toast.makeText(this, "Network not available", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(getApplicationContext())
                .unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_get_data:
                getPlanets();
                return true;

            case R.id.action_post_data:
                createPlanetPluto();
                return true;

            case R.id.action_put_data:
                updatePlanetPluto();
                return true;

            case R.id.action_delete_data:
                deletePlanetPluto();
                return true;

            case R.id.action_delete_bogus:
                deletePlanetBogus();
                return true;

            // TODO #8 - handle menu item Upload Image of Pluto
            case R.id.action_post_data_binary:
                uploadImageFileOfPluto();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Create a new planet on the web service named: Pluto
     *
     * Operation: HTTP POST /planets
     *
     * The body of the POST request contains the new planet.
     */
    private void createPlanetPluto() {
        // Create a new Planet --- Pluto
        PlanetPOJO pluto = new PlanetPOJO();

        // Set the planet's Id to 0
        // Problem: Mars has a planet Id of 0
        // Solution: the web service will change the Id from 0 to the next Id to uniquely identify
        //           this planet (pluto)
        //           The next available Id is: 8
        pluto.setPlanetId( 0 );

        // The web service requires values for *each* property (i.e. instance variable).
        pluto.setName( "Pluto" );
        pluto.setOverview( "I miss Pluto!" );
        // web service hosts images/noimagefound.jpg
        // The URL is http://planets.mybluemix.net/images/noimagefound.jpg
        // set the image to relative path on web service
        pluto.setImage( "images/noimagefound.jpg" );
        pluto.setDescription( "Pluto was stripped of planet status :(" );
        pluto.setDistanceFromSun( 39.5d );
        pluto.setNumberOfMoons( 5 );

        RequestPackage requestPackage = new RequestPackage();
        requestPackage.setMethod( HttpMethod.POST );
        requestPackage.setEndPoint( JSON_URL );
        requestPackage.setParam("planetId", pluto.getPlanetId() + "");
        requestPackage.setParam("name", pluto.getName() );
        requestPackage.setParam("overview", pluto.getOverview() );
        requestPackage.setParam("image", pluto.getImage() );
        requestPackage.setParam("description", pluto.getDescription() );
        requestPackage.setParam("distanceFromSun", pluto.getDistanceFromSun() + "");
        requestPackage.setParam("numberOfMoons", pluto.getNumberOfMoons() + "" );

        Intent intent = new Intent(this, MyService.class);
        intent.putExtra(MyService.REQUEST_PACKAGE, requestPackage);
        startService(intent);

        Log.i(TAG, "createPlanetPluto(): " + pluto.getName());
    }

    /**
     * Delete a planet that does not exist.
     *
     * Operation: HTTP DELETE /planets/99
     */
    private void deletePlanetBogus() {
        RequestPackage requestPackage = new RequestPackage();
        requestPackage.setMethod( HttpMethod.DELETE );
        // DELETE the planet with Id 99; this planet does not exist
        requestPackage.setEndPoint( JSON_URL + "/99" );

        Intent intent = new Intent(this, MyService.class);
        intent.putExtra(MyService.REQUEST_PACKAGE, requestPackage);
        startService(intent);

        Log.i(TAG, "deletePlanetBogus() Id: 99");
    }

    /**
     * Delete Pluto from the web service.
     *
     * Operation: HTTP DELETE /planets/8
     */
    private void deletePlanetPluto() {
        RequestPackage requestPackage = new RequestPackage();
        requestPackage.setMethod( HttpMethod.DELETE );
        // DELETE the planet with Id 8
        requestPackage.setEndPoint( JSON_URL + "/8" );

        Intent intent = new Intent(this, MyService.class);
        intent.putExtra(MyService.REQUEST_PACKAGE, requestPackage);
        startService(intent);

        Log.i(TAG, "deletePlanetPluto() Id: 8");
    }

    /**
     * Fetch the planet data from the web service.
     *
     * Operation: HTTP GET /planets
     */
    private void getPlanets() {
        RequestPackage getPackage = new RequestPackage();
        getPackage.setMethod( HttpMethod.GET );
        getPackage.setEndPoint( JSON_URL );

        Intent intent = new Intent(this, MyService.class);
        intent.putExtra(MyService.REQUEST_PACKAGE, getPackage);
        startService(intent);

        Log.i(TAG, "getPlanets()");
    }

    /**
     * Update (rename) planet Pluto to hurdleg
     *
     * Operation: HTTP UPDATE /planets/8
     *
     * The body of the POST request contains the updated planet.
     */
    private void updatePlanetPluto() {
        PlanetPOJO pluto = new PlanetPOJO();
        // The web service requires values for *each* property (i.e. instance variable).
        // Update existing planet Id 8 (Pluto)
        // Change the name, overview and description to: hurdleg
        // Change the image to: images/pluto.jpeg (image is stored on web service)
        pluto.setPlanetId( 8 );
        pluto.setName( "hurdleg" );
        pluto.setOverview( "hurdleg" );
        pluto.setImage( "images/pluto.jpeg" );
        pluto.setDescription( "hurdleg" );
        pluto.setDistanceFromSun( 39.5d );
        pluto.setNumberOfMoons( 5 );

        RequestPackage requestPackage = new RequestPackage();
        requestPackage.setMethod( HttpMethod.PUT );
        requestPackage.setEndPoint( JSON_URL + "/8" );
        requestPackage.setParam("planetId", pluto.getPlanetId() + "");
        requestPackage.setParam("name", pluto.getName() );
        requestPackage.setParam("overview", pluto.getOverview() );
        requestPackage.setParam("image", pluto.getImage() );
        requestPackage.setParam("description", pluto.getDescription() );
        requestPackage.setParam("distanceFromSun", pluto.getDistanceFromSun() + "");
        requestPackage.setParam("numberOfMoons", pluto.getNumberOfMoons() + "" );

        Intent intent = new Intent(this, MyService.class);
        intent.putExtra(MyService.REQUEST_PACKAGE, requestPackage);
        startService(intent);

        Log.i(TAG, "updatePlanetPluto(): " + pluto.getName());
    }

    // TODO #9 - event handler for menu item Upload Image of Pluto
    /**
     * Update binary image file of Pluto
     *
     * Operation: HTTP POST /planets/8/image
     */
    private void uploadImageFileOfPluto() {

        RequestPackage requestPackage = new RequestPackage();
        requestPackage.setMethod( HttpMethod.POST );
        requestPackage.setEndPoint( JSON_URL + "/8" + "/image" );

        Intent intent = new Intent(this, UploadImageFileService.class);
        intent.putExtra(UploadImageFileService.REQUEST_PACKAGE, requestPackage);
        startService(intent);

        Log.i(TAG, "uploadImageFileOfPluto()");
    }
}
