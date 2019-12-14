package com.example.grapeproject.Panel;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.grapeproject.Adapter.AdapterValues;
import com.example.grapeproject.Help.Base64;
import com.example.grapeproject.Help.SettingsDB;
import com.example.grapeproject.MainActivity;
import com.example.grapeproject.Models.Users;
import com.example.grapeproject.Models.Values;
import com.example.grapeproject.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnMonthChangedListener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class StatusActivity extends AppCompatActivity {

    private MaterialCalendarView calendario;
    private Double totalProfit = 0.0, average = 0.0;
    private int totalQuantity;
    private Values value;
    private TextView txtUser, txtProfit, txtQuantity, txtAverage;
    private RecyclerView recyclerView;
    private List<Values> values = new ArrayList<>(  );
    private AdapterValues adapterValues;
    private DatabaseReference reference = SettingsDB.getDataBaseReference();
    private FirebaseAuth auth =  SettingsDB.getFireBaseAuth();
    private DatabaseReference usuarioref, movimentacaoref = SettingsDB.getDataBaseReference();
    private ValueEventListener valEventListUser, valEventListValue;
    private String mesAnoSelecionado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);

        getSupportActionBar().setElevation(0);
        recuperarResumo();

        calendario = findViewById(R.id.calendarView);
        calendario.state().edit()
                .setMaximumDate(CalendarDay.from(2019,10,1))
                .setMaximumDate( CalendarDay.from(2020,2,1))
                .commit();

        CharSequence meses[] = {"Janeiro","Fevereiro","Março","Abril","Maio","Junho","Julho","Agosto","Setembro","Outubro","Novembro","Dezembro"};
        calendario.setTitleMonths(meses);

        CalendarDay calendarDay = calendario.getCurrentDate();

        String mesSelecionado = String.format( "%02d", (calendarDay.getMonth()+1) );
        mesAnoSelecionado = String.valueOf(mesSelecionado + "" +calendarDay.getYear());

        calendario.setOnMonthChangedListener( new OnMonthChangedListener() {
            @Override
            public void onMonthChanged(MaterialCalendarView widget, CalendarDay date) {
                String mesSelecionado = String.format( "%02d", (date.getMonth()+1) );
                mesAnoSelecionado = String.valueOf( (mesSelecionado + "" +date.getYear() ));
                System.out.println( mesAnoSelecionado );

                movimentacaoref.removeEventListener(valEventListValue);
                recuperarMovimentacao();
            }
        });

        txtUser = findViewById( R.id.txtUser );
        txtProfit = findViewById( R.id.txtValue );
        txtAverage = findViewById( R.id.txtAVG );
        txtQuantity = findViewById( R.id.txtQuantity );
        recyclerView = findViewById( R.id.recHistory );

        adapterValues = new AdapterValues(values, this );
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager( this );
        recyclerView.setLayoutManager( layoutManager );
        recyclerView.setHasFixedSize( true );
        recyclerView.setAdapter(adapterValues);

        swipe();

    }

    public void recuperarMovimentacao(){

        String email = auth.getCurrentUser().getEmail();
        String id = Base64.codeBase( email );
        movimentacaoref = reference.child( "value" ).child( id ).child( mesAnoSelecionado );

        System.out.println( "Movimentacao = " + mesAnoSelecionado );

        valEventListValue = movimentacaoref.addValueEventListener( new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                values.clear();
                for(DataSnapshot dados: dataSnapshot.getChildren()){
                    Values value = dados.getValue( Values.class);
                    //System.out.println( movimentacao.getCategoria());
                    value.setKey( dados.getKey() );
                    values.add(value);
                }
                adapterValues.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        } );
    }


    public void recuperarResumo(){

        String email = auth.getCurrentUser().getEmail();
        String id = Base64.codeBase( email );
        usuarioref = reference.child( "users" ).child( id );

        valEventListUser = usuarioref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                Users users = dataSnapshot.getValue( Users.class );


                totalProfit = users.getTotalProfit();
                totalQuantity = users.getTotalBoxes();
                average = totalProfit / totalQuantity;

                DecimalFormat decimalFormat = new DecimalFormat( "0.##" );
                String valueProfit = decimalFormat.format( totalProfit );
                String valueAvg = decimalFormat.format( average );

                txtUser.setText( "Olá, " + users.getName()+".");
                txtProfit.setText("Total: R$ "+ valueProfit);
                txtAverage.setText("Media: R$ "+ valueAvg);
                txtQuantity.setText("Caixas: "+ totalQuantity);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void swipe(){

        ItemTouchHelper.Callback item = new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                int dragFlags = ItemTouchHelper.ACTION_STATE_IDLE;
                int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
                return makeMovementFlags( dragFlags, swipeFlags );
            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                excluirMovimento( viewHolder );

            }
        };

        new ItemTouchHelper( item ).attachToRecyclerView( recyclerView );

    }

    public void excluirMovimento(final RecyclerView.ViewHolder viewHolder){

        AlertDialog.Builder alert = new AlertDialog.Builder( this );
        alert.setTitle( "Exlcuir movimentação" );
        alert.setMessage( "Tem certeza que deseja exlcuir a movimentação?" );
        alert.setCancelable( false );

        alert.setPositiveButton( "Confirmar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                int pos = viewHolder.getAdapterPosition();
                value = values.get( pos );


                String email = auth.getCurrentUser().getEmail();
                String id = Base64.codeBase( email );
                movimentacaoref = reference.child( "value" ).child( id ).child( mesAnoSelecionado );

                movimentacaoref.child( value.getKey() ).removeValue();
                adapterValues.notifyItemRemoved(pos);
                atualizarSaldo();
            }
        } );

        alert.setNegativeButton( "Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText( getApplicationContext(), "Exclusão cancelada", Toast.LENGTH_SHORT ).show();
                adapterValues.notifyDataSetChanged();
            }
        } );

        AlertDialog alertDialog = alert.create();
        alertDialog.show();

    }

    public void atualizarSaldo(){

        String email = auth.getCurrentUser().getEmail();
        String id = Base64.codeBase( email );
        usuarioref = reference.child( "users" ).child( id );

        if(!value.getType().equals( "" )){
            Double profit = totalProfit - value.getValue();
            int quantity =  totalQuantity - value.getQuantity();
            usuarioref.child( "totalProfit" ).setValue( profit );
            usuarioref.child( "totalBoxes" ).setValue( quantity );
        }

    }

    public void criarLucro(View view){
        startActivity( new Intent( getApplicationContext(), ProfitActivity.class));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate( R.menu.menu_status, menu );
        return super.onCreateOptionsMenu( menu );
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.itemColheita:
                Toast.makeText(this, "Colheita encerrada!", Toast.LENGTH_SHORT).show();
                break;
            case R.id.itemSair:
                auth.signOut();
                startActivity( new Intent( this, MainActivity.class ) );
                finish();
                break;
        }
        return super.onOptionsItemSelected( item );
    }

    @Override
    protected void onStart() {
        super.onStart();
        recuperarResumo();
        recuperarMovimentacao();
        System.out.println( "Evento iniciado" );
    }

    @Override
    protected void onStop() {
        super.onStop();
        System.out.println( "Evento removido" );
        usuarioref.removeEventListener( valEventListUser );
        movimentacaoref.removeEventListener( valEventListValue );
    }
}