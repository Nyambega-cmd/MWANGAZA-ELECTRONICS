package com.mwangaza.electronics

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject

data class Item(val name: String, val price: Double)

fun load(ctx: Context): List<Item> {
    val s = ctx.getSharedPreferences("mwangaza", 0).getString("items", null)
    if (s == null) return listOf(
        Item("Electric Kettle", 1800.0), Item("Microwave", 8500.0),
        Item("Flat Iron", 1200.0), Item("Blender", 3500.0), Item("Radio", 1500.0)
    )
    val a = JSONArray(s)
    return List(a.length()) { Item(a.getJSONObject(it).getString("n"), a.getJSONObject(it).getDouble("p")) }
}

fun save(ctx: Context, list: List<Item>) {
    val a = JSONArray()
    list.forEach { a.put(JSONObject().put("n", it.name).put("p", it.price)) }
    ctx.getSharedPreferences("mwangaza", 0).edit().putString("items", a.toString()).apply()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { App() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var items by remember { mutableStateOf(load(ctx).sortedBy { it.name.lowercase() }) }
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Item?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    fun update(list: List<Item>) { items = list.sortedBy { it.name.lowercase() }; save(ctx, items) }
    val shown = items.filter { it.name.contains(query.trim(), ignoreCase = true) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Mwangaza Electronics") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; showDialog = true }) {
                Icon(Icons.Default.Add, "Add item")
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).padding(12.dp)) {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                placeholder = { Text("Search item...") },
                leadingIcon = { Icon(Icons.Default.Search, null) }
            )
            Spacer(Modifier.height(8.dp))
            if (shown.isEmpty()) Text("No items found. Tap + to add.")
            LazyColumn {
                items(shown) { item ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        .clickable { editing = item; showDialog = true }) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(item.name, style = MaterialTheme.typography.titleMedium)
                                Text("KSh " + "%,.0f".format(item.price),
                                    style = MaterialTheme.typography.bodyLarge)
                            }
                            IconButton(onClick = { update(items - item) }) {
                                Icon(Icons.Default.Delete, "Delete")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        val old = editing
        var name by remember { mutableStateOf(old?.name ?: "") }
        var price by remember { mutableStateOf(old?.price?.let { "%.0f".format(it) } ?: "") }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (old == null) "Add item" else "Edit item / price") },
            text = {
                Column {
                    OutlinedTextField(name, { name = it }, label = { Text("Item name") }, singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(price, { price = it }, label = { Text("Price (KSh)") },
                        singleLine = true, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val p = price.toDoubleOrNull()
                    if (name.isNotBlank() && p != null) {
                        val base = if (old != null) items - old else items
                        update(base + Item(name.trim(), p))
                        showDialog = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } }
        )
    }
}
