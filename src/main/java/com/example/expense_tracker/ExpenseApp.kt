@file:Suppress("UNCHECKED_CAST")

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.example.expense_tracker.data.ExpenseDatabase
import com.example.expense_tracker.data.ExpenseItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** ViewModel */
@OptIn(ExperimentalMaterial3Api::class)      // required for the M3 date‑picker APIs
class ExpenseViewModel(db: ExpenseDatabase) : ViewModel() {
    private val dao = db.expenseDao()
    val expenses: Flow<List<ExpenseItem>> = dao.getAll()

    fun addExpense(name: String, pri: Double, cat: String, date: String) =
        viewModelScope.launch {
            dao.insert(ExpenseItem(name = name, price = pri, category = cat, date = date))
        }

    fun updateExpense(item: ExpenseItem) = viewModelScope.launch { dao.update(item) }
    fun deleteExpense(item: ExpenseItem) = viewModelScope.launch { dao.delete(item) }
}

class ExpenseViewModelFactory(private val db: ExpenseDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ExpenseViewModel(db) as T
}

/** Remember a single Room instance */
@Composable
fun rememberDatabase(): ExpenseDatabase {
    val ctx = LocalContext.current
    return remember {
        Room.databaseBuilder(ctx, ExpenseDatabase::class.java, "expense-db")
            .fallbackToDestructiveMigration()
            .build()
    }
}

@Composable
fun ExpenseApp() {
    val db = rememberDatabase()
    val vm: ExpenseViewModel = viewModel(factory = ExpenseViewModelFactory(db))
    val expenses by vm.expenses.collectAsState(initial = emptyList())

    /* ── Form state ─────────────────────────────────── */
    var name by remember { mutableStateOf("") }
    var pri  by remember { mutableStateOf("") }
    var cat  by remember { mutableStateOf("") }

    /* ── Date‑picker state ───────────────────────────── */
    val todayMillis = remember {
        LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
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
    val categories = listOf("Food", "Apartment", "Transport", "Fees", "Health", "Social", "Shopping", "Travel", "Others")
    expanded by remember { mutableStateOf(false) }

    Column(Modifier.padding(16.dp)) {

        /* ── Add expense ─────────────────────────────── */
        Text("Add Expense", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        TextField(name, { name = it }, label = { Text("Description") })
        Spacer(Modifier.height(4.dp))
        TextField(pri, { pri = it }, label = { Text("Price") })
        Spacer(Modifier.height(4.dp))
        OutlinedButton(onClick = { expanded = true }) {
            Text(if (cat.isNotEmpty()) cat else "Select Category")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            categories.forEach { category ->
                DropdownMenuItem(text = { Text(category) }, onClick = {
                    cat = category
                    expanded = false
                })
            }
        }
        // TextField(cat, { cat = it }, label = { Text("Category") })
        Spacer(Modifier.height(4.dp))

        /* Date picker button */
        OutlinedButton(onClick = { showDatePicker = true }) {
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

        Button(onClick = {
            val price = pri.toDoubleOrNull()
            if (name.isNotBlank() && price != null && cat.isNotBlank() && dateString.isNotBlank()) {
                vm.addExpense(name, price, cat, date)
                /* Clear form */
                name = ""; pri = ""; cat = ""
                datePickerState.selectedDateMillis = todayMillis
            } else {
                Toast.makeText(ctx, "Please fill all fields correctly", Toast.LENGTH_SHORT).show()
            }
        }) { Text("Add Expense") }

        /* ── Expense list ─────────────────────────────── */
        Spacer(Modifier.height(16.dp))
        Text("Expenses", style = MaterialTheme.typography.titleMedium)

        Row(
            Modifier
                .fillMaxWidth()
                .background(Color.Gray)
                .padding(8.dp)
        ) {
            Text("Name", Modifier.weight(2f), color = Color.White)
            Text("Price",       Modifier.weight(1f), color = Color.White)
            Text("Category",    Modifier.weight(1f), color = Color.White)
            Text("Date",        Modifier.weight(1f), color = Color.White)
            Spacer(Modifier.weight(1f))
        }

        LazyColumn {
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

/** Single row in the list */
@Composable
fun ExpenseRow(
    exp: ExpenseItem,
    onUpdate: (ExpenseItem) -> Unit,
    onDelete: () -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(exp.name) }
    var amt  by remember { mutableStateOf(exp.amount.toString()) }
    var pri  by remember { mutableStateOf(exp.price) }
    var date by remember { mutableStateOf(exp.date) }

    if (editing) {
        Row(Modifier.fillMaxWidth().padding(4.dp)) {
            TextField(name, { name = it }, Modifier.weight(2f))
            TextField(amt,  { amt  = it }, Modifier.weight(1f))
            TextField(pri,  { pri  = it }, Modifier.weight(1f))
            TextField(date, { date = it }, Modifier.weight(1f))
            Button(onClick = {
                onUpdate(exp.copy(
                    name = name,
                    amount      = amt.toDoubleOrNull() ?: 0.0,
                    price       = pri,
                    date        = date
                ))
                editing = false
            }) { Text("Save") }
        }
    } else {
        Row(Modifier.fillMaxWidth().padding(8.dp).background(Color.LightGray)) {
            Text(name, Modifier.weight(2f).padding(4.dp))
            Text(exp.price.toString(), Modifier.weight(1f).padding(4.dp))
            Text(pri, Modifier.weight(1f).padding(4.dp))
            Text(date, Modifier.weight(1f).padding(4.dp))
            Row(Modifier.weight(1f)) {
                TextButton({ editing = true }) { Text("Edit") }
                TextButton(onDelete)          { Text("Delete") }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewExpenseApp() {
    ExpenseApp()
}
