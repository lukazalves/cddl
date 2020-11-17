package br.ufma.lsdi.accesscontrol;

import android.hardware.Sensor;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import br.ufma.lsdi.cddl.message.Message;

public class MainActivity extends AppCompatActivity {

    private MainController controller;
    private Button startButton;
    private Button stopButton;
    List<String> listViewMessages;
    ListAdapter listAdapter;
    ListViewAdapter listViewAdapter;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        controller = new MainController();

        if(savedInstanceState == null){
            controller.config(this);
            controller.configSubscriber();
            controller.setListener(message -> messageHandler(message));
        }

        configSpinner();
        configStartButton();
        configStopButton();
        configListView();


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    private void messageHandler(Message message) {
    }

    private void configListView() {
        ListView listView = findViewById(R.id.listview);
        listViewMessages = new ArrayList<>();

    }

    private void configStopButton() {
        stopButton = findViewById(R.id.start_button);
        stopButton.setOnClickListener(e -> {
            Spinner spinner = findViewById(R.id.spinner);
            String selectedSensor = spinner.getSelectedItem().toString();
            controller.startSensor(selectedSensor);
            stopButton.setEnabled(true);
            startButton.setEnabled(false);
        });
    }

    private void configStartButton() {
        startButton = findViewById(R.id.start_button);
        startButton.setOnClickListener(e -> {
            Spinner spinner = findViewById(R.id.spinner);
            String selectedSensor = spinner.getSelectedItem().toString();
            controller.startSensor(selectedSensor);
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void configSpinner(){

        List<Sensor> sensors = controller.getInternalListSensor();
        List<String> sensorNames = sensors.stream().map(sensor -> sensor.getName()).collect(Collectors.toList());

        ArrayAdapter<String> adpter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, sensorNames);
        Spinner spinner = findViewById(R.id.spinner);
        spinner.setAdapter(adpter);

    }

}