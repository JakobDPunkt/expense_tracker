package com.example.expense_tracker.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.example.expense_tracker.data.ExpenseDatabase
import com.example.expense_tracker.data.ExpenseItem
import com.example.expense_tracker.ui.components.CategoryDropdown
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
class ExpenseViewModel(private val db: ExpenseDatabase) : ViewModel() {
    private val dao = db.expenseDao()
    val expenses: Flow<List<ExpenseItem>> = dao.getAll()

    fun addExpense(desc: String, amt: Double, cat: String, date: String) =
        viewModelScope.launch {
            dao.insert(ExpenseItem(name = desc, price = amt, category = cat, date = date))
        }

    fun updateExpense(item: ExpenseItem) = viewModelScope.launch { dao.update(item) }
    fun deleteExpense(item: ExpenseItem) = viewModelScope.launch { dao.delete(item) }
}

class ExpenseViewModelFactory(private val db: ExpenseDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ExpenseViewModel(db) as T
}

@Composable
fun rememberDatabase(): ExpenseDatabase {
    val ctx = LocalContext.current
    return remember {
        Room.databaseBuilder(ctx, ExpenseDatabase::class.java, "expense-db")
            .fallbackToDestructiveMigration()
            .build()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen() {
    val db = rememberDatabase()
    val vm: ExpenseViewModel = viewModel(factory = ExpenseViewModelFactory(db))
    val expenses by vm.expenses.collectAsState(initial = emptyList())

    // Form state
    var desc by remember { mutableStateOf("") }
    var amt  by remember { mutableStateOf("") }
    var cat  by remember { mutableStateOf("") }

    // Category options
    val categories = listOf("Food", "Transport", "Entertainment", "Utilities", "Other")

    // Date picker state
    val todayMillis = remember {
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = todayMillis)
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }
    val dateString = datePickerState.selectedDateMillis?.let { millis ->
        Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(dateFormatter)
    } ?: ""

    val ctx = LocalContext.current

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Add Expense", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        TextField(
            value = desc,
            onValueChange = { desc = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))

        TextField(
            value = amt,
            onValueChange = { amt = it },
            label = { Text("Amount") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))

        CategoryDropdown(
            options = categories,
            selectedOption = cat,
            onOptionSelected = { cat = it },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))

        OutlinedButton(
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (dateString.isNotBlank()) dateString else "Select date")
        }
        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                val amount = amt.toDoubleOrNull()
                if (desc.isNotBlank() && amount != null && cat.isNotBlank() && dateString.isNotBlank()) {
                    vm.addExpense(desc, amount, cat, dateString)
                    desc = ""; amt = ""; cat = ""
                    datePickerState.selectedDateMillis = todayMillis
                } else {
                    Toast.makeText(ctx, "Please fill all fields correctly", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Expense")
        }

        Spacer(Modifier.height(16.dp))
        Text("Expenses", style = MaterialTheme.typography.titleMedium)

        Row(
            Modifier
                .fillMaxWidth()
                .background(Color.Gray)
                .padding(8.dp)
        ) {
            Text("Description", Modifier.weight(2f), color = Color.White)
            Text("Amount",      Modifier.weight(1f), color = Color.White)
            Text("Category",    Modifier.weight(1f), color = Color.White)
            Text("Date",        Modifier.weight(1f), color = Color.White)
            Spacer(Modifier.weight(1f))
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(expenses) { exp ->
                ExpenseRow(
                    exp,
                    onUpdate = vm::updateExpense,
                    onDelete = { vm.deleteExpense(exp) }
                )
            }
        }
    }
}

@Composable
fun ExpenseRow(
    exp: ExpenseItem,
    onUpdate: (ExpenseItem) -> Unit,
    onDelete: () -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    var desc by remember { mutableStateOf(exp.name) }
    var amt  by remember { mutableStateOf(exp.price.toString()) }
    var cat  by remember { mutableStateOf(exp.category) }
    var date by remember { mutableStateOf(exp.date) }

    if (editing) {
        Row(Modifier.fillMaxWidth().padding(4.dp)) {
            TextField(desc, { desc = it }, Modifier.weight(2f))
            TextField(amt,  { amt  = it }, Modifier.weight(1f))
            TextField(cat,  { cat  = it }, Modifier.weight(1f))
            TextField(date, { date = it }, Modifier.weight(1f))
            Button(onClick = {
                onUpdate(
                    exp.copy(
                        name = desc,
                        price      = amt.toDoubleOrNull() ?: 0.0,
                        category    = cat,
                        date        = date
                    )
                )
                editing = false
            }) { Text("Save") }
        }
    } else {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .background(Color.LightGray)
        ) {
            Text(desc, Modifier.weight(2f).padding(4.dp))
            Text(exp.price.toString(), Modifier.weight(1f).padding(4.dp))
            Text(cat, Modifier.weight(1f).padding(4.dp))
            Text(date, Modifier.weight(1f).padding(4.dp))
            Row(Modifier.weight(1f)) {
                TextButton({ editing = true }) { Text("Edit") }
                TextButton(onDelete)          { Text("Delete") }
            }
        }
    }
}
