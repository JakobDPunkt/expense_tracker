package com.example.expense_tracker.ui

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
import android.widget.Toast

// ---------- ViewModel ----------
class ExpenseViewModel(private val db: ExpenseDatabase) : ViewModel() {
    private val dao = db.expenseDao()
    val expenses: Flow<List<ExpenseItem>> = dao.getAll()

    fun addExpense(desc: String, amt: Double, cat: String, date: String) =
        viewModelScope.launch {
            dao.insert(ExpenseItem(description = desc, amount = amt, category = cat, date = date))
        }

    fun updateExpense(item: ExpenseItem) = viewModelScope.launch { dao.update(item) }
    fun deleteExpense(item: ExpenseItem) = viewModelScope.launch { dao.delete(item) }
}

class ExpenseViewModelFactory(private val db: ExpenseDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ExpenseViewModel(db) as T
}

// ---------- Composables ----------
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

    var desc by remember { mutableStateOf("") }
    var amt  by remember { mutableStateOf("") }
    var cat  by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    val ctx = LocalContext.current

    Column(Modifier.padding(16.dp)) {
        Text("Add Expense", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        TextField(desc, { desc = it }, label = { Text("Description") })
        Spacer(Modifier.height(4.dp))
        TextField(amt, { amt = it }, label = { Text("Amount") })
        Spacer(Modifier.height(4.dp))
        TextField(cat, { cat = it }, label = { Text("Category") })
        Spacer(Modifier.height(4.dp))
        TextField(date, { date = it }, label = { Text("Date (yyyy-MM-dd)") })
        Spacer(Modifier.height(8.dp))

        Button(onClick = {
            val amount = amt.toDoubleOrNull()
            if (desc.isNotBlank() && amount != null && cat.isNotBlank() && date.isNotBlank()) {
                vm.addExpense(desc, amount, cat, date)
                desc = ""; amt = ""; cat = ""; date = ""
            } else {
                Toast.makeText(ctx, "Please fill all fields correctly", Toast.LENGTH_SHORT).show()
            }
        }) { Text("Add Expense") }

        Spacer(Modifier.height(16.dp))
        Text("Expenses", style = MaterialTheme.typography.titleMedium)

        Row(Modifier.fillMaxWidth().background(Color.Gray).padding(8.dp)) {
            Text("Description", Modifier.weight(2f), color = Color.White)
            Text("Amount",      Modifier.weight(1f), color = Color.White)
            Text("Category",    Modifier.weight(1f), color = Color.White)
            Text("Date",        Modifier.weight(1f), color = Color.White)
            Spacer(Modifier.weight(1f))
        }

        LazyColumn {
            items(expenses) { exp ->
                ExpenseRow(exp, onUpdate = vm::updateExpense) { vm.deleteExpense(exp) }
            }
        }
    }
}

@Composable
fun ExpenseRow(exp: ExpenseItem, onUpdate: (ExpenseItem) -> Unit, onDelete: () -> Unit) {
    var editing by remember { mutableStateOf(false) }
    var desc by remember { mutableStateOf(exp.description) }
    var amt  by remember { mutableStateOf(exp.amount.toString()) }
    var cat  by remember { mutableStateOf(exp.category) }
    var date by remember { mutableStateOf(exp.date) }

    if (editing) {
        Row(Modifier.fillMaxWidth().padding(4.dp)) {
            TextField(desc, { desc = it }, Modifier.weight(2f))
            TextField(amt,  { amt  = it }, Modifier.weight(1f))
            TextField(cat,  { cat  = it }, Modifier.weight(1f))
            TextField(date, { date = it }, Modifier.weight(1f))
            Button(onClick = {
                onUpdate(exp.copy(
                    description = desc,
                    amount      = amt.toDoubleOrNull() ?: 0.0,
                    category    = cat,
                    date        = date
                ))
                editing = false
            }) { Text("Save") }
        }
    } else {
        Row(Modifier.fillMaxWidth().padding(8.dp).background(Color.LightGray)) {
            Text(desc, Modifier.weight(2f).padding(4.dp))
            Text(exp.amount.toString(), Modifier.weight(1f).padding(4.dp))
            Text(cat, Modifier.weight(1f).padding(4.dp))
            Text(date, Modifier.weight(1f).padding(4.dp))
            Row(Modifier.weight(1f)) {
                TextButton({ editing = true }) { Text("Edit") }
                TextButton(onDelete) { Text("Delete") }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewExpenseApp() { ExpenseApp() }