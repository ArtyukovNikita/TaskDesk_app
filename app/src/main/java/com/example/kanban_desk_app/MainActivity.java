package com.example.kanban_desk_app;

import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.LinearLayout;
import android.app.DatePickerDialog;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private DataBaseHelper dbHelper;
    private RecyclerView boardsRecyclerView;
    private BoardsAdapter boardsAdapter;
    private TasksAdapter tasksAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DataBaseHelper(this);

        boardsRecyclerView = findViewById(R.id.recyclerView_boards);
        boardsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadBoards();

        FloatingActionButton fab = findViewById(R.id.fab_add_board); // Исправлено на FloatingActionButton
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddBoardDialog();
            }
        });
    }

    private void showAddBoardDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Добавить доску");

        // Поле ввода для названия доски
        final EditText inputName = new EditText(this);
        inputName.setHint("Название доски");

        // Создание компоновки для диалога
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(inputName);
        builder.setView(layout);

        builder.setPositiveButton("Добавить", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = inputName.getText().toString().trim();
                // Проверка на пустое название
                if (name.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Название доски не может быть пустым", Toast.LENGTH_SHORT).show();
                    return;
                }
                addBoard(name); // Добавляем доску в базу данных
            }
        });

        builder.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel(); // Закрыть диалог
            }
        });

        builder.show(); // Показать диалог
    }


    private void addBoard(String name) {
        // Вставка в базу данных
        dbHelper.addBoard(name); // ваш метод для добавления в базу
        loadBoards(); // Обновить отображение досок
    }

    private void loadBoards() {
        Cursor cursor = dbHelper.getAllBoards();
        if (cursor != null && cursor.getCount() > 0) {
            boardsAdapter = new BoardsAdapter(this, cursor);
            boardsRecyclerView.setAdapter(boardsAdapter);
        } else {
            // Обработка случая, когда нет доступных досок
            Toast.makeText(this, "Нет доступных досок", Toast.LENGTH_SHORT).show();
        }
    }
    public void deleteBoard(int boardId) {
        dbHelper.deleteBoard(boardId); // Ваш метод для удаления доски
        loadBoards(); // Обновление списка досок
    }

    public void showAddTaskDialog(int boardId) {
        Log.d("MainActivity", "boardId в showAddTaskDialog: " + boardId);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Добавить задание");

        // Поля ввода
        final EditText inputName = new EditText(this);
        inputName.setHint("Название задания");

        final EditText inputDescription = new EditText(this);
        inputDescription.setHint("Описание задания");

        final EditText inputDate = new EditText(this);
        inputDate.setHint("Дата задания (дд.мм.гггг)");

        // Установка клика для выбора даты
        inputDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog(inputDate);
            }
        });

        // Создание компоновки для диалога
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(inputName);
        layout.addView(inputDescription);
        layout.addView(inputDate);
        builder.setView(layout);

        // Кнопка "Добавить"
        builder.setPositiveButton("Добавить", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = inputName.getText().toString().trim();
                String description = inputDescription.getText().toString().trim();
                String date = inputDate.getText().toString().trim();

                // Проверка на пустые значения
                if (name.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Название задания не может быть пустым", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Добавление задания в базу данных
                dbHelper.addTask(boardId, name, description, date); // Метод добавления задания

                // Обновление задач для текущей доски
                updateTasksForBoard(boardId);
            }
        });

        // Кнопка "Отмена"
        builder.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel(); // Закрыть диалог
            }
        });

        builder.show(); // Показать диалог
    }

    private void updateTasksForBoard(int boardId) {
        // Получение курсора задач для текущей доски
        Cursor newTasksCursor = dbHelper.getTasksByBoardId(boardId);

        // Получаем адаптер для досок
        BoardsAdapter boardsAdapter = (BoardsAdapter) boardsRecyclerView.getAdapter();
        if (boardsAdapter != null) {
            for (int i = 0; i < boardsAdapter.getItemCount(); i++) {
                if (boardsAdapter.isActiveBoardAtPosition(i, boardId)) {
                    BoardsAdapter.ViewHolder holder = (BoardsAdapter.ViewHolder) boardsRecyclerView.findViewHolderForAdapterPosition(i);
                    if (holder != null) {
                        // Закрываем старый курсор, если он существует
                        if (holder.tasksRecyclerView.getAdapter() != null) {
                            TasksAdapter currentAdapter = (TasksAdapter) holder.tasksRecyclerView.getAdapter();
                            if (currentAdapter != null) {
                                Cursor oldCursor = currentAdapter.getCursor(); // Получаем текущий курсор
                                if (oldCursor != null && !oldCursor.isClosed()) {
                                    oldCursor.close(); // Закрываем старый курсор, если он не закрыт
                                }
                            }
                        }

                        // Устанавливаем новый адаптер задач
                        TasksAdapter tasksAdapter = new TasksAdapter(this, newTasksCursor, dbHelper, boardId); //надеюсь будет работать эта
                        //RecyclerView recyclerView = findViewById(R.id.recycler_view);                //(this, newTasksCursor, dbHelper) конструкция
                        //recyclerView.setLayoutManager(new LinearLayoutManager(this));
                        holder.tasksRecyclerView.setAdapter(tasksAdapter);

                    }
                }
            }
        }
    }





    // Метод для отображения выбора даты
    private void showDatePickerDialog(final EditText inputDate) {
        // Получаем текущую дату
        final Calendar c = Calendar.getInstance();
        int day = c.get(Calendar.DAY_OF_MONTH);
        int month = c.get(Calendar.MONTH);
        int year = c.get(Calendar.YEAR);

        // Создаем DatePickerDialog
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int selectedYear, int selectedMonth, int selectedDay) {
                        // Устанавливаем выбранную дату в поле ввода
                        inputDate.setText(String.format("%02d.%02d.%04d", selectedDay, selectedMonth + 1, selectedYear));
                    }
                }, year, month, day);

        datePickerDialog.show(); // Показываем DatePickerDialog
    }

    public void showEditBoardDialog(int boardId, String currentName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Редактировать доску");

        // Поле для редактирования имени доски
        final EditText inputName = new EditText(this);
        inputName.setHint("Название доски");
        inputName.setText(currentName); // Устанавливаем текущее имя доски

        builder.setView(inputName);

        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String newName = inputName.getText().toString().trim();
            if (!newName.isEmpty()) {
                updateBoardName(boardId, newName);
            } else {
                Toast.makeText(MainActivity.this, "Название доски не может быть пустым", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void updateBoardName(int boardId, String newName) {
        dbHelper.updateBoardName(boardId, newName);
        loadBoards(); // Обновить отображение досок после изменения
    }
}
