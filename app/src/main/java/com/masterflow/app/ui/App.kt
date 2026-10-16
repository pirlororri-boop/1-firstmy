package com.masterflow.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.masterflow.app.MainViewModel
import com.masterflow.app.data.Appointment
import com.masterflow.app.data.MoneyTransaction
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MasterFlowApp(vm: MainViewModel = viewModel()) {
    var tab by remember { mutableIntStateOf(0) }
    val appointments by vm.appointments.collectAsState()
    val transactions by vm.transactions.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar {
                listOf("Главная", "Календарь", "Деньги").forEachIndexed { i, title ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Text(if (i == 0) "⌂" else if (i == 1) "◫" else "₴") },
                        label = { Text(title) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                0 -> Dashboard(transactions, appointments)
                1 -> CalendarScreen(appointments, vm::addAppointment, vm::completeAppointment)
                else -> MoneyScreen(transactions, vm::addTransaction)
            }
        }
    }
}

@Composable
private fun Dashboard(transactions: List<MoneyTransaction>, appointments: List<Appointment>) {
    val cal = Calendar.getInstance()
    val month = cal.get(Calendar.MONTH)
    val year = cal.get(Calendar.YEAR)
    val monthTx = transactions.filter {
        val c = Calendar.getInstance().apply { timeInMillis = it.occurredAt }
        c.get(Calendar.MONTH) == month && c.get(Calendar.YEAR) == year
    }
    val income = monthTx.filter { it.type == "INCOME" }.sumOf { it.amountKopecks }
    val expense = monthTx.filter { it.type == "EXPENSE" }.sumOf { it.amountKopecks }
    val today = appointments.filter { sameDay(it.startAt, System.currentTimeMillis()) && !it.completed }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("MasterFlow", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item { SummaryCard("Выручка за месяц", income) }
        item { SummaryCard("Расходы за месяц", expense) }
        item { SummaryCard("Чистыми", income - expense) }
        item { Text("Сегодня", style = MaterialTheme.typography.titleLarge) }
        if (today.isEmpty()) item { Text("Записей на сегодня пока нет") }
        items(today) { AppointmentRow(it) }
    }
}

@Composable
private fun SummaryCard(title: String, kopecks: Long) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(money(kopecks), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CalendarScreen(
    appointments: List<Appointment>,
    add: (String, String, String, Long, Int, Double) -> Unit,
    complete: (Appointment) -> Unit
) {
    var show by remember { mutableStateOf(false) }
    val upcoming = appointments.filter { it.startAt >= startOfToday() }.sortedBy { it.startAt }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Календарь", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Button(onClick = { show = true }) { Text("+ Запись") }
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(upcoming) { item ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        AppointmentRow(item)
                        if (!item.completed) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = { complete(item) }) { Text("Клиент пришёл — записать доход") }
                        }
                    }
                }
            }
        }
    }
    if (show) AppointmentDialog(onDismiss = { show = false }) { n,p,s,time,d,price ->
        add(n,p,s,time,d,price); show = false
    }
}

@Composable
private fun AppointmentDialog(onDismiss: () -> Unit, onSave: (String,String,String,Long,Int,Double)->Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var service by remember { mutableStateOf("Окрашивание") }
    var date by remember { mutableStateOf(SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())) }
    var timeText by remember { mutableStateOf("12:00") }
    var duration by remember { mutableStateOf("120") }
    var price by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая запись") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Field("Имя клиента", name) { name = it }
                Field("Телефон", phone) { phone = it }
                Field("Услуга", service) { service = it }
                Field("Дата, ДД.ММ.ГГГГ", date) { date = it }
                Field("Время, ЧЧ:ММ", timeText) { timeText = it }
                Field("Длительность, минут", duration) { duration = it }
                Field("Цена, грн", price) { price = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                parseDateTime(date, timeText)?.let { onSave(name, phone, service, it, duration.toIntOrNull() ?: 60, price.replace(',', '.').toDoubleOrNull() ?: 0.0) }
            }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
private fun MoneyScreen(transactions: List<MoneyTransaction>, add: (String,String,Double,String,String)->Unit) {
    var show by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Деньги", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Button(onClick = { show = true }) { Text("+ Операция") }
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(transactions) { tx ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(tx.category, fontWeight = FontWeight.SemiBold)
                            if (tx.clientName.isNotBlank()) Text(tx.clientName, style = MaterialTheme.typography.bodySmall)
                            if (tx.note.isNotBlank()) Text(tx.note, style = MaterialTheme.typography.bodySmall)
                        }
                        Text((if (tx.type == "INCOME") "+ " else "− ") + money(tx.amountKopecks), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
    if (show) TransactionDialog(onDismiss = { show = false }) { t,c,a,cl,n -> add(t,c,a,cl,n); show = false }
}

@Composable
private fun TransactionDialog(onDismiss: () -> Unit, onSave: (String,String,Double,String,String)->Unit) {
    var type by remember { mutableStateOf("EXPENSE") }
    var category by remember { mutableStateOf("Расходники") }
    var amount by remember { mutableStateOf("") }
    var client by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая операция") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = type == "INCOME", onClick = { type = "INCOME" }, label = { Text("Доход") })
                    FilterChip(selected = type == "EXPENSE", onClick = { type = "EXPENSE" }, label = { Text("Расход") })
                }
                Field("Категория: аренда, краска, услуга…", category) { category = it }
                Field("Сумма, грн", amount) { amount = it }
                Field("Клиент (необязательно)", client) { client = it }
                Field("Комментарий", note) { note = it }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(type, category, amount.replace(',', '.').toDoubleOrNull() ?: 0.0, client, note) }) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
private fun Field(label: String, value: String, onChange: (String)->Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun AppointmentRow(item: Appointment) {
    val f = SimpleDateFormat("dd.MM  HH:mm", Locale.getDefault())
    Column {
        Text("${f.format(Date(item.startAt))} · ${item.clientName}", fontWeight = FontWeight.Bold)
        Text("${item.service} · ${item.durationMinutes} мин · ${money(item.priceKopecks)}")
        Text(item.phone, style = MaterialTheme.typography.bodySmall)
        if (item.completed) Text("Выполнено ✓", style = MaterialTheme.typography.bodySmall)
    }
}

private fun money(k: Long) = String.format(Locale.getDefault(), "%.2f грн", k / 100.0)
private fun sameDay(a: Long, b: Long): Boolean {
    val x = Calendar.getInstance().apply { timeInMillis = a }
    val y = Calendar.getInstance().apply { timeInMillis = b }
    return x.get(Calendar.YEAR) == y.get(Calendar.YEAR) && x.get(Calendar.DAY_OF_YEAR) == y.get(Calendar.DAY_OF_YEAR)
}
private fun startOfToday(): Long = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY,0); set(Calendar.MINUTE,0); set(Calendar.SECOND,0); set(Calendar.MILLISECOND,0) }.timeInMillis
private fun parseDateTime(date: String, time: String): Long? = runCatching {
    SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).apply { isLenient = false }.parse("$date $time")!!.time
}.getOrNull()
