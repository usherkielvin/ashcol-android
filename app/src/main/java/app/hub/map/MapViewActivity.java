package app.hub.map;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import app.hub.R;
import app.hub.util.GooglePlayServicesUtils;

public class MapViewActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private double latitude, longitude;
    private String address;
    private boolean isReadonly;
    private TextView tvAddress;
    private Button btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_view);

        if (!GooglePlayServicesUtils.ensureAvailable(this)) {
            finish();
            return;
        }

        // Get data from intent
        latitude = getIntent().getDoubleExtra("latitude", 0.0);
        longitude = getIntent().getDoubleExtra("longitude", 0.0);
        address = getIntent().getStringExtra("address");
        isReadonly = getIntent().getBooleanExtra("readonly", true);

        initViews();
        MapsInitializer.initialize(getApplicationContext());
        setupMap();
    }

    private void initViews() {
        tvAddress = findViewById(R.id.tvAddress);
        btnBack = findViewById(R.id.btnBack);

        if (address != null) {
            tvAddress.setText(address);
        }

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (latitude != 0 && longitude != 0) {
            LatLng location = new LatLng(latitude, longitude);
            
            // Add marker
            mMap.addMarker(new MarkerOptions()
                    .position(location)
                    .title("Service Location")
                    .snippet(address != null ? address : "Selected Location"));

            // Move camera to location
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f));

            // Disable interaction if readonly
            if (isReadonly) {
                mMap.getUiSettings().setAllGesturesEnabled(false);
                mMap.getUiSettings().setZoomControlsEnabled(false);
                mMap.getUiSettings().setMapToolbarEnabled(false);
            }
        }
    }
}
