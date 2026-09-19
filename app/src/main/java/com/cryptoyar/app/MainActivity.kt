package com.cryptoyar.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.LayoutDirection
import java.util.Locale

data class Site(val name:String,val currency:String,val balance:Double,val today:Double,val url:String)
data class Task(val site:String,val title:String,val reward:Double,val minutes:Int) {
    val perMinute get() = if(minutes>0) reward/minutes else 0.0
}

class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                CryptoYarApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CryptoYarApp() {
    var tab by remember { mutableIntStateOf(0) }
    var selectedUrl by remember { mutableStateOf<String?>(null) }
    var todayIncome by remember { mutableDoubleStateOf(0.0) }
    var totalMinutes by remember { mutableLongStateOf(0L) }

    val sites = remember {
        listOf(
            Site("Cointiply","USD",0.0,0.0,"https://cointiply.com/"),
            Site("AdBTC","BTC",0.0,0.0,"https://adbtc.top/")
        )
    }
    val tasks = remember {
        listOf(
            Task("Cointiply","Offer کوتاه",0.30,5),
            Task("Cointiply","Survey",0.80,15),
            Task("AdBTC","PTC",0.05,3)
        )
    }

    if(selectedUrl != null) {
        SiteWebView(selectedUrl!!) { selectedUrl = null }
        return
    }

    val labels=listOf("خانه","سایت‌ها","کارها","کیف پول")
    val icons=listOf(Icons.Default.Home,Icons.Default.Language,Icons.Default.Timer,Icons.Default.AccountBalanceWallet)

    Scaffold(
        topBar={ TopAppBar(title={Text("کریپتو یار")},actions={Text("v0.4",Modifier.padding(end=16.dp))}) },
        bottomBar={ NavigationBar {
            labels.forEachIndexed { i,label ->
                NavigationBarItem(tab==i,{tab=i},{Icon(icons[i],label)},label={Text(label)})
            }
        }}
    ){ p ->
        Box(Modifier.padding(p).fillMaxSize()) {
            when(tab){
                0->HomeScreen(todayIncome,totalMinutes,3.0,sites)
                1->SitesScreen(sites){selectedUrl=it}
                2->TasksScreen(tasks){ earned,minutes->todayIncome+=earned;totalMinutes+=minutes }
                3->WalletScreen()
            }
        }
    }
}

@Composable
fun HomeScreen(income:Double,minutes:Long,goal:Double,sites:List<Site>) {
    val progress=(income/goal).coerceIn(0.0,1.0).toFloat()
    val hourly=if(minutes>0) income/(minutes/60.0) else 0.0
    LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{
            Card(Modifier.fillMaxWidth()){
                Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                    Text("داشبورد امروز",style=MaterialTheme.typography.headlineSmall)
                    Text("$"+String.format(Locale.US,"%.2f",income)+" از هدف $"+String.format(Locale.US,"%.2f",goal))
                    LinearProgressIndicator(progress={progress},Modifier.fillMaxWidth())
                    Text("زمان فعالیت: "+(minutes/60)+" ساعت و "+(minutes%60)+" دقیقه")
                    Text("میانگین ثبت‌شده: $"+String.format(Locale.US,"%.2f",hourly)+" در ساعت")
                }
            }
        }
        item{Text("سایت‌ها",style=MaterialTheme.typography.titleLarge)}
        items(sites){site->
            ListItem(
                headlineContent={Text(site.name)},
                supportingContent={Text(site.currency+" • امروز: $"+String.format(Locale.US,"%.2f",site.today))}
            )
        }
        item{Text("درآمدها باید دستی و مطابق قوانین هر سایت ثبت شوند.",style=MaterialTheme.typography.bodySmall)}
    }
}

@Composable
fun SitesScreen(sites:List<Site>,open:(String)->Unit){
    LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        item{
            Card(Modifier.fillMaxWidth()){
                Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
                    Text("باز کردن سریع و سبک",style=MaterialTheme.typography.titleLarge)
                    Text("اگر سایت داخل برنامه درست نمایش داده نشد، گزینه مرورگر سیستم را بزن.")
                }
            }
        }
        items(sites){site->
            Card(Modifier.fillMaxWidth()){
                Row(Modifier.padding(14.dp).fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                    Column(Modifier.weight(1f)){
                        Text(site.name,style=MaterialTheme.typography.titleMedium)
                        Text(site.url,style=MaterialTheme.typography.bodySmall)
                    }
                    Button({open(site.url)}){Text("باز کردن")}
                }
            }
        }
    }
}

@Composable
fun TasksScreen(tasks:List<Task>,earned:(Double,Int)->Unit){
    var running by remember{mutableStateOf<Task?>(null)}
    var elapsed by remember{mutableLongStateOf(0L)}
    var startedAt by remember{mutableLongStateOf(0L)}
    LaunchedEffect(running){
        while(running!=null){
            kotlinx.coroutines.delay(1000)
            elapsed=(System.currentTimeMillis()-startedAt)/1000
        }
    }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        item{Text("کارها — بیشترین درآمد در دقیقه",style=MaterialTheme.typography.headlineSmall)}
        items(tasks.sortedByDescending{it.perMinute}){task->
            Card(Modifier.fillMaxWidth()){
                Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                    Text(task.title,style=MaterialTheme.typography.titleMedium)
                    Text(task.site+" • $"+String.format(Locale.US,"%.2f",task.reward)+" • "+task.minutes+" دقیقه")
                    Text("$"+String.format(Locale.US,"%.3f",task.perMinute)+" در دقیقه")
                    Button({
                        if(running==task){
                            earned(task.reward,(elapsed/60).toInt().coerceAtLeast(1))
                            running=null
                            elapsed=0
                        } else {
                            running=task
                            startedAt=System.currentTimeMillis()
                            elapsed=0
                        }
                    }){Text(if(running==task)"توقف و ثبت" else "شروع زمان‌سنج")}
                    if(running==task) Text("زمان: "+(elapsed/60)+":"+String.format(Locale.US,"%02d",elapsed%60))
                }
            }
        }
    }
}

@Composable
fun WalletScreen(){
    var btc by remember{mutableStateOf("")}
    var usdt by remember{mutableStateOf("")}
    var saved by remember{mutableStateOf(false)}
    LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{Text("کیف پول برداشت",style=MaterialTheme.typography.headlineSmall)}
        item{OutlinedTextField(btc,{btc=it},label={Text("آدرس عمومی BTC")},modifier=Modifier.fillMaxWidth())}
        item{OutlinedTextField(usdt,{usdt=it},label={Text("آدرس عمومی USDT")},modifier=Modifier.fillMaxWidth())}
        item{Text("کلید خصوصی یا عبارت بازیابی را وارد نکنید.",style=MaterialTheme.typography.bodySmall)}
        item{Button({saved=true},Modifier.fillMaxWidth()){Text("ذخیره آدرس‌ها")}}
        if(saved)item{Text("آدرس‌ها برای این نشست ثبت شدند.")}
    }
}

@Composable
fun SiteWebView(url:String,onClose:()->Unit){
    val context = androidx.compose.ui.platform.LocalContext.current
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()){
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
            TextButton(onClose){Text("بازگشت")}
            Text("سایت",Modifier.weight(1f))
            TextButton({
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (_: Exception) {}
            }){Text("مرورگر")}
        }

        if(loading) LinearProgressIndicator(Modifier.fillMaxWidth())

        if(failed){
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment=Alignment.CenterHorizontally,
                verticalArrangement=Arrangement.Center
            ){
                Text("این سایت در WebView باز نشد.")
                Spacer(Modifier.height(12.dp))
                Text("برای سازگاری بیشتر، با مرورگر گوشی بازش کن.")
                Spacer(Modifier.height(12.dp))
                Button({
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }){Text("باز کردن با مرورگر")}
            }
        } else {
            AndroidView(
                factory={context->
                    WebView(context).apply {
                        setBackgroundColor(android.graphics.Color.WHITE)
                        webViewClient=object: WebViewClient(){
                            override fun onPageStarted(view:WebView?, url:String?, favicon:android.graphics.Bitmap?){
                                loading=true
                                failed=false
                            }
                            override fun onPageFinished(view:WebView?, url:String?){
                                loading=false
                            }
                            override fun onReceivedError(
                                view:WebView?,
                                request:WebResourceRequest?,
                                error:android.webkit.WebResourceError?
                            ){
                                if(request?.isForMainFrame != false) failed=true
                                loading=false
                            }
                        }
                        webChromeClient=WebChromeClient()
                        settings.javaScriptEnabled=true
                        settings.domStorageEnabled=true
                        settings.databaseEnabled=true
                        settings.loadsImagesAutomatically=true
                        settings.javaScriptCanOpenWindowsAutomatically=true
                        settings.setSupportMultipleWindows(false)
                        settings.cacheMode=android.webkit.WebSettings.LOAD_DEFAULT
                        settings.userAgentString=settings.userAgentString+" Chrome/128.0 Mobile"
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this,true)
                        loadUrl(url)
                    }
                },
                modifier=Modifier.fillMaxSize()
            )
        }
    }
}
