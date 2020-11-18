/* package br.ufma.lsdi.accesscontrol;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
} */

package br.ufma.lsdi.accesscontrol;


import android.hardware.Sensor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.widget.ArrayAdapter;
import android.widget.Button;
//import android.widget.ListView;
import android.widget.Spinner;

//import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import br.ufma.lsdi.cddl.message.Message;

public class MainActivity extends AppCompatActivity {

    private MainController controller;
    private Button startButton;
    private Button stopButton;
    private List<String> listViewMessages;
    private ListViewAdapter listViewAdapter;
    private Handler handler;
    private Spinner spinner;
    private String selectedSensor;


    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        controller = new MainController();
       // handler = new Handler();

        if(savedInstanceState == null){
            controller.config(this);
            controller.configSubscriber();
            //controller.setListener(message -> messageHandler(message));
        }

        //sconfigSpinner();
        //configStartButton();
        //configStopButton();
        //configClearButton();
        //configListView();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
/*
    private void messageHandler(Message message) {
        handler.post(() -> {
            //Object[] serviceValue = message.getServiceValue().toString();
            //String values = String.join(", ", serviceValue);
            //String values = StringUtils.join(serviceValue,", ");
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
            //System.out.println(message);
            //listViewMessages.add(0,values);
            //listViewAdapter.notifyDataSetChanged();
        });
    }

    private void configListView() {
        ListView listView = findViewById(R.id.listView);
        listViewMessages = new ArrayList<>();
        listViewAdapter = new ListViewAdapter(this,listViewMessages);
        listView.setAdapter(listViewAdapter);

    }

    private void configStopButton() {
        stopButton = findViewById(R.id.start_button);
        stopButton.setOnClickListener(e -> {
            spinner = findViewById(R.id.spinner);
            selectedSensor = spinner.getSelectedItem().toString();
            controller.startSensor(selectedSensor);
            stopButton.setEnabled(true);
            startButton.setEnabled(false);
        });
    }/
    private void configStartButton() {
        startButton = findViewById(R.id.start_button);
        startButton.setOnClickListener(e -> {
            spinner = findViewById(R.id.spinner);
            selectedSensor = spinner.getSelectedItem().toString();
            controller.startSensor(selectedSensor);
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
        });
    }

    private void configClearButton(){
        final Button clearButton = findViewById(R.id.clear_button);
        clearButton.setOnClickListener(
                e -> {
                    listViewMessages.clear();
                    listViewAdapter.notifyDataSetChanged();
                }
        );
    }


    private void configSpinner(){
        List<Sensor> sensors = controller.getInternalSensorList();

        List<String> sensorNames = sensors.stream().map(sensor -> sensor.getName()).collect(Collectors.toList());

        /*
        List<String> sensorNames = new ArrayList<>();
        for (Sensor sensor : sensors) {
            String name = sensor.getName();
            sensorNames.add(name);
        }


        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, sensorNames);
        Spinner spinner = findViewById(R.id.spinner);
        spinner.setAdapter(adapter);

    }
*/
}