package app.geniuslab.beer.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import app.geniuslab.beer.R;
import app.geniuslab.beer.connection.MyConnection;
import app.geniuslab.beer.connection.RestApi;
import app.geniuslab.beer.dialog.LoadingDialog;
import app.geniuslab.beer.model.Beer;
import app.geniuslab.beer.recycler.AdapterRecycler;
import app.geniuslab.beer.session.Preference;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    ImageView profileImage;
    TextView nameText;

    private RestApi restApi = RestApi.RETROFIT.create(RestApi.class);
    private Preference preference;

    RecyclerView recyclerView;
    AdapterRecycler adapter;

    Context context=this;
    List<Beer> beers = new ArrayList<>();
    LoadingDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        ButterKnife.bind(this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        loadingDialog = new LoadingDialog(HomeActivity.this, getString(R.string.app_name),getString(R.string.process_message));

        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Beer!");
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        View hView =  navigationView.getHeaderView(0);

        profileImage = hView.findViewById(R.id.profile_picture);
        nameText = hView.findViewById(R.id.name_text);
        preference = Preference.getIntance(this);
        nameText.setText(preference.getName());

        RequestOptions requestOptions = new RequestOptions();
        int size10 = (int)  getResources().getDimension(R.dimen.size80);
        requestOptions = requestOptions.transforms(new CenterCrop(), new RoundedCorners(size10));
        Glide.with(this).asBitmap().load(preference.getProfilePicture()).apply(requestOptions).into(profileImage);
        recyclerView = findViewById(R.id.recyclerview_home);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdapterRecycler(this,null);
        adapter.setOnClickBeer(new AdapterRecycler.OnClickBeer() {
            @Override
            public void onDelete(Beer beer) {
                deleteBeer(beer);
            }
        });
        recyclerView.setAdapter(adapter);

        loadData();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                insertData();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();


    }

    private void deleteBeer(final Beer beer){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Desea eliminar la bebida?");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MyConnection cn=new MyConnection(context,null,null,2);
                SQLiteDatabase db=cn.getWritableDatabase();

                db.delete("beer",
                        "id=?",
                        new String[]{String.valueOf(beer.getId())});
                updateData();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();

    }

    @OnClick(R.id.export_btn)
    public void actionExport(){
        loadingDialog.show();
        MyConnection sqlite = new MyConnection(context,null,null,2);
        SQLiteDatabase db = sqlite.getWritableDatabase();
        restApi.saveBeers("drinks",sqlite.getList(db)).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                loadingDialog.showMessage(getString(R.string.export_message));
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("onFailure",t.toString());
            }
        });
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {
            startActivity(new Intent(HomeActivity.this, RegisterBeerActivity.class));
        } else if (id == R.id.nav_slideshow) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        401);
            }
            else {
                startActivity(new Intent(HomeActivity.this, DecoderActivity.class));
            }
        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

     void loadData(){
       restApi.getdrink().enqueue(new Callback<JsonArray>() {
           @Override
           public void onResponse(Call<JsonArray> call, Response<JsonArray> response) {
              beers = new ArrayList<>();
               for (JsonElement beer:response.body()){
                   beers.add(new Beer(beer.getAsJsonObject().get("id").getAsInt(),
                           beer.getAsJsonObject().get("name").getAsString(),
                           beer.getAsJsonObject().get("image").getAsString(),
                           beer.getAsJsonObject().get("price").getAsString()));
               }
               Toast.makeText(context,"El servicio esta iniciado correctamente "  + beers.size(), Toast.LENGTH_LONG).show();
           }

           @Override
           public void onFailure(Call<JsonArray> call, Throwable t) {

           }
       });
    }
    void insertData(){
        Set set = new HashSet(Arrays.asList(beers));
        Set set2 = new HashSet(Arrays.asList(listado()));

        if(set.containsAll(set2)){

        }

        MyConnection sqlite = new MyConnection(context,null,null,2);
        SQLiteDatabase db = sqlite.getWritableDatabase();
        for(Beer beer : beers){
            if (!validation(db,beer.getId())){
                sqlite.insertBeer(beer.getName(),beer.getPrice(),beer.getImage(),db);
            }
        }
        Toast.makeText(context,"Se inserto correctamente "  + beers.size(), Toast.LENGTH_LONG).show();
        updateData();


    }


    public boolean validation(SQLiteDatabase db, int id){
            String Query = "Select * from " + "beer" + " where " + "id" + " = " + id;
            Cursor cursor = db.rawQuery(Query, null);
            if(cursor.getCount() <= 0){
                cursor.close();
                return false;
            }
            cursor.close();
            return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateData();
    }
    private void updateData(){
        MyConnection sqlite = new MyConnection(context,null,null,2);
        SQLiteDatabase db = sqlite.getWritableDatabase();
        adapter.setBeers(sqlite.getList(db));
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 401: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startActivity(new Intent(HomeActivity.this, DecoderActivity.class));

                } else {

                }
                return;
            }

        }
    }

    public ArrayList<Beer> listado() {
        ArrayList<Beer> products = new ArrayList<Beer>();

        try {
            MyConnection cn = new MyConnection(context, null, null, 2);
            SQLiteDatabase db = cn.getReadableDatabase();

            // Cursor es como un ResultSet
            Cursor cur = db.rawQuery("select * from product", null);
            Beer bean;
            while (cur.moveToNext()) {
                bean = new Beer();
                bean.setId(cur.getInt(0));
                bean.setName(cur.getString(1));
                bean.setImage(cur.getString(2));
                bean.setPrice(cur.getString(3));

                products.add(bean);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return products;
    }
}
