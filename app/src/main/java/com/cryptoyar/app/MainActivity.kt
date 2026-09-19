package com.cryptoyar.app

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.util.Locale

data class Site(val name:String,val currency:String,val balance:Double,val today:Double,val url:String)
data class Task(val site:String,val title:String,val reward:Double,val minutes:Int) {
    val perMinute get() = if(minutes>0) reward/minutes else 0.0
}

class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { CryptoYarApp() } }
    }
}

@Composable
fun CryptoYarApp() {
    var tab by remember { mutableIntStateOf(0) }
    var selectedUrl by remember { mutableStateOf<String?>(null) }
    val sites = remember { mutableStateListOf(
        Site("Cointiply","USD",0.0,0.0,"https://cointiply.com/"),
        Site("AdBTC","BTC",0.0,0.0,"https://adbtc.top/")
    )}
    val tasks = remember { mutableStateListOf(
        Task("Cointiply","Offer کوتاه",0.30,5),
        Task("Cointiply","Survey",0.80,15),
        Task("AdBTC","PTC",0.05,3)
    )}
    var todayIncome by remember { mutableDoubleStateOf(0.0) }
    val goal = 3.0

    if(selectedUrl != null) {
        SiteWebView(selectedUrl!!) { selectedUrl = null }
        return
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("کریپتو یار") }, actions = { Text("نسخه 0.1", modifier=Modifier.padding(end=16.dp)) }) },
        bottomBar = {
            NavigationBar {
                listOf("خانه","سایت‌ها","کارها","کیف پول").forEachIndexed { i, label ->
                    NavigationBarItem(selected=tab==i,onClick={tab=i},icon={IconLabel(label)},label={Text(label)})
                }
            }
        }
    ) { p ->
        Box(Modifier.padding(p).fillMaxSize()) {
            when(tab) {
                0 -> HomeScreen(todayIncome,goal,sites)
                1 -> SitesScreen(sites){selectedUrl=it}
                2 -> TasksScreen(tasks){ earned -> todayIncome += earned }
                3 -> WalletScreen()
            }
        }
    }
}

@Composable fun IconLabel(label:String) { Text(label.take(1)) }

@Composable
fun HomeScreen(income:Double,goal:Double,sites:List<Site>) {
    val progress=(income/goal).coerceIn(0.0,1.0).toFloat()
    LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    Text("هدف امروز",style=MaterialTheme.typography.titleLarge)
                    Text("$"+String.format(Locale.US,"%.2f",income)+" از $"+String.format(Locale.US,"%.2f",goal))
                    LinearProgressIndicator(progress={progress},Modifier.fillMaxWidth())
                    Text("درآمد فقط بر اساس فعالیت دستی شما ثبت می‌شود.")
                }
            }
        }
        item { Text("سایت‌های درآمدی",style=MaterialTheme.typography.titleMedium) }
        items(sites) { site ->
            ListItem(headlineContent={Text(site.name)}, supportingContent={Text(site.currency+" | امروز: "+String.format(Locale.US,"%.2f",site.today))})
        }
    }
}

@Composable
fun SitesScreen(sites:List<Site>,open:(String)->Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        items(sites) { site ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp).fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)){ Text(site.name,style=MaterialTheme.typography.titleMedium); Text(site.url,style=MaterialTheme.typography.bodySmall) }
                    Button(onClick={open(site.url)}){Text("باز کردن")}
                }
            }
        }
    }
}

@Composable
fun TasksScreen(tasks:List<Task>,earned:(Double)->Unit) {
    var running by remember { mutableStateOf<Task?>(null) }
    var elapsed by remember { mutableLongStateOf(0L) }
    var startedAt by remember { mutableLongStateOf(0L) }
    LaunchedEffect(running) {
        while(running!=null) {
            kotlinx.coroutines.delay(1000)
            elapsed=(System.currentTimeMillis()-startedAt)/1000
        }
    }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        item { Text("کارها بر اساس درآمد هر دقیقه",style=MaterialTheme.typography.titleLarge) }
        items(tasks.sortedByDescending{it.perMinute}) { task ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text(task.title,style=MaterialTheme.typography.titleMedium)
                    Text(task.site+" • $"+String.format(Locale.US,"%.2f",task.reward)+" • "+task.minutes+" دقیقه")
                    Text("حدود $"+String.format(Locale.US,"%.3f",task.perMinute)+" در دقیقه")
                    Button(onClick={
                        if(running==task) { earned(task.reward); running=null; elapsed=0 }
                        else { running=task; startedAt=System.currentTimeMillis(); elapsed=0 }
                    }) { Text(if(running==task) "توقف و ثبت درآمد" else "شروع زمان‌سنج") }
                    if(running==task) Text("زمان: "+(elapsed/60)+":"+String.format(Locale.US,"%02d",elapsed%60))
                }
            }
        }
    }
}

@Composable
fun WalletScreen() {
    var btc by remember { mutableStateOf("") }
    var usdt by remember { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item { Text("کیف پول برداشت",style=MaterialTheme.typography.titleLarge) }
        item { OutlinedTextField(btc,{btc=it},label={Text("آدرس BTC")},modifier=Modifier.fillMaxWidth()) }
        item { OutlinedTextField(usdt,{usdt=it},label={Text("آدرس USDT")},modifier=Modifier.fillMaxWidth()) }
        item { Text("فقط آدرس عمومی برداشت ذخیره می‌شود؛ کلید خصوصی وارد نکنید.",style=MaterialTheme.typography.bodySmall) }
        item { Button(onClick={},modifier=Modifier.fillMaxWidth()){Text("ذخیره")} }
    }
}

@Composable
fun SiteWebView(url:String,onClose:()->Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            TextButton(onClick=onClose){Text("بازگشت")}
            Text(url,Modifier.weight(1f),maxLines=1)
        }
        AndroidView(factory={ context ->
            WebView(context).apply {
                webViewClient=WebViewClient()
                settings.javaScriptEnabled=true
                settings.domStorageEnabled=true
                loadUrl(url)
            }
        },modifier=Modifier.fillMaxSize())
    }
}
