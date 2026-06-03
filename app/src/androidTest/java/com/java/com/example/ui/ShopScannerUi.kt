package com.example.ui

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.MainViewModel
import com.example.data.Invoice
import com.example.data.InvoiceItem
import com.example.data.BarcodeProduct
import com.example.ui.theme.*
import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopBillingApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // State to toggle the beautiful built-in Barcode Scanner Simulator
    var showBarcodeScannerSimulator by remember { mutableStateOf(false) }

    // State to toggle the beautiful APK download guide dialog
    var showApkDownloadHelp by remember { mutableStateOf(false) }

    // Standard Speech To Text Intent Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenList = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val text = spokenList?.firstOrNull()
            if (text != null) {
                viewModel.processVoiceTranscript(text)
            }
        }
    }

    // Interactive printer selection dialog state
    var showPrinterDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = "Shop",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "حساب الحانوت الذكي",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 22.sp,
                            modifier = Modifier.testTag("app_title")
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            showApkDownloadHelp = true
                        },
                        modifier = Modifier.testTag("apk_download_help_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "تحميل التطبيق APK",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = {
                            viewModel.refreshPairedDevices()
                            showPrinterDialog = true
                        },
                        modifier = Modifier.testTag("printer_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "الطابعة",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // --- Custom Navigation Tabs ---
            TabRow(
                selectedTabIndex = viewModel.currentTabSelected,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[viewModel.currentTabSelected]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                Tab(
                    selected = viewModel.currentTabSelected == 0,
                    onClick = { viewModel.currentTabSelected = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("الفاتورة الحالية", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    },
                    modifier = Modifier.testTag("tab_current")
                )
                Tab(
                    selected = viewModel.currentTabSelected == 1,
                    onClick = { viewModel.currentTabSelected = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Dashboard, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("التقارير والمبيعات", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    },
                    modifier = Modifier.testTag("tab_history")
                )
                Tab(
                    selected = viewModel.currentTabSelected == 2,
                    onClick = { viewModel.currentTabSelected = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("سجل الشراء", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    },
                    modifier = Modifier.testTag("tab_purchases")
                )
            }

            AnimatedContent(
                targetState = viewModel.currentTabSelected,
                transitionSpec = {
                    slideInHorizontally { width -> if (targetState > initialState) width else -width } togetherWith
                            slideOutHorizontally { width -> if (targetState > initialState) -width else width }
                },
                label = "TabTransition"
            ) { tabIndex ->
                when (tabIndex) {
                    0 -> CurrentInvoiceScreen(
                        viewModel = viewModel,
                        onTriggerSpeech = {
                            try {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-MA") // Moroccan Darija support!
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar-MA")
                                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "ar-MA")
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "قول السلعة والثمن (مثال: حليب جوج ب 3 دراهم)")
                                }
                                speechLauncher.launch(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "الصوت غير مدعوم على هذا الهاتف.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onTriggerScan = {
                            showBarcodeScannerSimulator = true
                        }
                    )
                    1 -> SalesDashboardAndHistoryScreen(viewModel)
                    2 -> PurchasesScreen(viewModel)
                }
            }
        }
    }

    // --- NEW BARCODE MAPPING DIALOG ---
    if (viewModel.scannedBarcodeForCreation != null) {
        Dialog(
            onDismissRequest = { viewModel.scannedBarcodeForCreation = null },
            properties = DialogProperties(usePlatformDefaultWidth = true)
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp))
                    .shadow(12.dp, RoundedCornerShape(24.dp))
                    .testTag("new_barcode_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "كود جديد",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(54.dp)
                    )
                    
                    Text(
                        text = "كود بار جديد مكتشف! 🏷️",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = "الكود: ${viewModel.scannedBarcodeForCreation}\nدخل سمية وتمن السلعة باش تحفظها وتولي تطلع أوتوماتيك من بعد!",
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )

                    OutlinedTextField(
                        value = viewModel.promptBarcodeName,
                        onValueChange = { viewModel.promptBarcodeName = it },
                        label = { Text("إسم السلعة (مثلا: كوكا قنينة)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("barcode_name_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = viewModel.promptBarcodePrice,
                        onValueChange = { viewModel.promptBarcodePrice = it },
                        label = { Text("الثمن (بالدرهم DH)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("barcode_price_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.scannedBarcodeForCreation = null },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("إلغاء", fontWeight = FontWeight.Bold)
                        }
                        
                        Button(
                            onClick = { viewModel.saveNewBarcodeProduct() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("حفظ السلعة", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // --- BLUETOOTH PRINTER DRAWER DIALOG ---
    if (showPrinterDialog) {
        Dialog(
            onDismissRequest = { showPrinterDialog = false }
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "إعدادات طابعة البلوتوث 🖨️",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "قم بربط طابعة الفواتير (Thermal Printer) من إعدادات الهاتف أولاً، ثم اخترها من القائمة أسفله:",
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Divider()

                    if (viewModel.pairedPrinters.isEmpty()) {
                        Text(
                            text = "لم يتم العثور على طابعات مقترنة.\nتأكد من تشغيل البلوتوث وتوصيل طابعة.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .heightIn(max = 200.dp)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(viewModel.pairedPrinters) { device ->
                                val isSelected = device.address == viewModel.selectedPrinterAddress
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            else Color.Transparent
                                        )
                                        .clickable { viewModel.selectedPrinterAddress = device.address }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Bluetooth,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(device.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(device.address, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.refreshPairedDevices() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تحديث")
                        }

                        Button(
                            onClick = { showPrinterDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("حفظ واغلاق")
                        }
                    }
                }
            }
        }
    }

    // --- VISUAL BARCODE SCANNER SIMULATOR DIALOG (سكاني) ---
    if (showBarcodeScannerSimulator) {
        Dialog(
            onDismissRequest = { showBarcodeScannerSimulator = false }
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MoroccanDarkBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .border(2.dp, MoroccanGoldDark, RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "محاكاة ماسح الباركود 🏷️",
                            color = MoroccanGoldDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        IconButton(onClick = { showBarcodeScannerSimulator = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    // Simulated Camera Frame with Laser beam
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0F1113))
                            .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Four Corner brackets
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Top left corner outline
                            Box(modifier = Modifier.align(Alignment.TopStart).padding(16.dp).size(20.dp).border(width = 3.dp, color = MoroccanGoldDark, shape = RoundedCornerShape(topStart = 4.dp)))
                            // Top right
                            Box(modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).size(20.dp).border(width = 3.dp, color = MoroccanGoldDark, shape = RoundedCornerShape(topEnd = 4.dp)))
                            // Bottom left
                            Box(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp).size(20.dp).border(width = 3.dp, color = MoroccanGoldDark, shape = RoundedCornerShape(bottomStart = 4.dp)))
                            // Bottom right
                            Box(modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).size(20.dp).border(width = 3.dp, color = MoroccanGoldDark, shape = RoundedCornerShape(bottomEnd = 4.dp)))
                        }

                        // Barcode Vector Graphic
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(80.dp)
                        )

                        // Glowing Laser scan line
                        val infiniteTransition = rememberInfiniteTransition(label = "Laser")
                        val laserOffset by infiniteTransition.animateFloat(
                            initialValue = -60f,
                            targetValue = 60f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "LaserMotion"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .offset(y = laserOffset.dp)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(Color.Transparent, MoroccanTealDark, Color.Transparent)
                                    )
                                )
                                .shadow(8.dp, spotColor = MoroccanTealDark)
                        )
                    }

                    Text(
                        text = "اختر كود بار دغيا باش تسكاني السلعة ولا دخل كود جديد لتجربته:",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )

                    // Quick list of mock products to scan
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Demo Product 1: Registered
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .clickable {
                                    // Let's seed products or scan them
                                    // Since we scan item, we call onBarcodeScanned
                                    // We will treat these codes as registered mock codes
                                    // In MainViewModel they will prompt for mapping if not found
                                    // Let's pass a specific code
                                    viewModel.onBarcodeScanned("6111248000123")
                                    showBarcodeScannerSimulator = false
                                }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = MoroccanGoldDark, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("كود بار: 6111248000123", color = Color.White, fontSize = 13.sp)
                            }
                            Text("اضغط للمحاكاة", color = MoroccanTealDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Demo Product 2: Unregistered (this triggers the registering prompt!)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .clickable {
                                    val randomNewCode = (10000000..99999999).random().toString()
                                    viewModel.onBarcodeScanned(randomNewCode)
                                    showBarcodeScannerSimulator = false
                                }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = MoroccanGoldDark, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("كود جديد عشوائي 🆕", color = Color.White, fontSize = 13.sp)
                            }
                            Text("تسجيل سلعة جديدة", color = MoroccanGoldDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Direct input option
                    var customCodeEntered by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = customCodeEntered,
                        onValueChange = { customCodeEntered = it },
                        label = { Text("أدخل رمز باركود يدوي", color = Color.White.copy(alpha = 0.6f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MoroccanGoldDark,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (customCodeEntered.trim().isNotEmpty()) {
                                viewModel.onBarcodeScanned(customCodeEntered.trim())
                                showBarcodeScannerSimulator = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MoroccanGoldDark),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("مسح الكود المكتوب", fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        }
    }

    // --- VISUAL PRINTER SIMULATOR ANIMATION OVERLAY ---
    if (viewModel.isPrintingSimulated) {
        // Automatically hide simulation after 4 seconds
        LaunchedEffect(Unit) {
            delay(4000)
            viewModel.isPrintingSimulated = false
        }

        Dialog(
            onDismissRequest = { viewModel.isPrintingSimulated = false }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .background(Color.Transparent)
                        .fillMaxWidth()
                ) {
                    // Modern slot device box
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C3E50)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        color = MoroccanWarmGold,
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("جاري الطباعة بالبلوتوث...", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (viewModel.selectedPrinterAddress.isNotEmpty()) "طابعة نشطة" else "محاكاة طبع التجربة",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp
                                )
                            }
                            
                            // Mechanical Printing Slot Indicator
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                                    .background(Color(0xFF1A252F))
                            )
                        }
                    }

                    // Ticket Animation flowing out from slot
                    var ticketFinished by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        delay(200)
                        ticketFinished = true
                    }

                    val ticketHeight by animateDpAsState(
                        targetValue = if (ticketFinished) 260.dp else 0.dp,
                        animationSpec = tween(durationMillis = 2500, easing = LinearEasing),
                        label = "TicketFlow"
                    )

                    Card(
                        shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(4.dp),
                        modifier = Modifier
                            .width(220.dp)
                            .height(ticketHeight)
                            .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("🧾 فاتورة إلكترونية", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.Black)
                            Text("حساب الحانوت الذكي", fontSize = 10.sp, color = Color.Gray)
                            Text("التاريخ: ${viewModel.inputInvoiceDateTime}", fontSize = 8.sp, color = Color.DarkGray)
                            if (viewModel.inputCustomerName.isNotBlank()) {
                                Text("الزبون: ${viewModel.inputCustomerName}", fontSize = 8.sp, color = Color.DarkGray, fontWeight = FontWeight.Bold)
                            }
                            if (viewModel.inputCustomerPhone.isNotBlank()) {
                                Text("الهاتف: ${viewModel.inputCustomerPhone}", fontSize = 8.sp, color = Color.DarkGray)
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp), color = Color.LightGray)
                            
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                items(viewModel.currentInvoiceItems) { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(item.name, fontSize = 10.sp, maxLines = 1, color = Color.Black)
                                        Text("${item.quantity}x${item.price}", fontSize = 9.sp, color = Color.DarkGray)
                                    }
                                }
                            }
                            
                            Divider(color = Color.LightGray)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("المجموع الإجمالي", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black)
                                Text("${String.format("%.2f", viewModel.currentInvoiceTotal)} DH", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MoroccanTealMint)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("شكرا على زيارتكم!", fontSize = 9.sp, color = Color.Gray, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }

    // --- APK DOWNLOAD GUIDE DIALOG (طريقة تحميل التطبيق) ---
    if (showApkDownloadHelp) {
        Dialog(
            onDismissRequest = { showApkDownloadHelp = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(16.dp)
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(28.dp))
                    .shadow(16.dp, RoundedCornerShape(28.dp))
                    .testTag("apk_download_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تحميل وتثبيت التطبيق 📱💾",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { showApkDownloadHelp = false }) {
                            Icon(Icons.Default.Close, contentDescription = "الغاء")
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                    // Infographic Image we generated earlier
                    Image(
                        painter = painterResource(id = com.example.R.drawable.img_apk_guide),
                        contentDescription = "دليل تحميل التطبيق",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )

                    // Step By Step Arabic Instructions
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "كيفاش تيليشارجي تطبيق 'حساب الحانوت' على تليفونك:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(verticalAlignment = Alignment.Top) {
                            Text("1️⃣  ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "اضغط على زر الإعدادات الموجود فوق اليمين في واجهة موقع (AI Studio).",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(verticalAlignment = Alignment.Top) {
                            Text("2️⃣  ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "اختار خيار 'Export' ثم 'Generate APK' باش يبدا بناء ملف التثبيت هاهنا على السحابة.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(verticalAlignment = Alignment.Top) {
                            Text("3️⃣  ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "من بعد ما يسالي البناء، غايبان ليك زر 'Download APK'، كليكي عليه وغايتحمل نيشان للتلفون ديالك!",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(verticalAlignment = Alignment.Top) {
                            Text("4️⃣  ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "إلى بغيتي صيفط اللينك فالايميل: دير نسخ للرابط صيفطو لبريدك tantamttaskort@gmail.com باش تثبتو دغيا بكل سهولة! 🎉",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { showApkDownloadHelp = false },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("مفهوم، شكراً !", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun CurrentInvoiceScreen(
    viewModel: MainViewModel,
    onTriggerSpeech: () -> Unit,
    onTriggerScan: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // --- Customer & Invoice Info Card (معلومات الزبون والفاتورة) ---
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "معلومات الزبون والتاريخ 👤📅",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = viewModel.inputCustomerName,
                        onValueChange = { viewModel.inputCustomerName = it },
                        label = { Text("إسم الزبون") },
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("input_customer_name"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )

                    OutlinedTextField(
                        value = viewModel.inputCustomerPhone,
                        onValueChange = { viewModel.inputCustomerPhone = it },
                        label = { Text("رقم الهاتف") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .weight(1.1f)
                            .testTag("input_customer_phone"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }

                OutlinedTextField(
                    value = viewModel.inputInvoiceDateTime,
                    onValueChange = { viewModel.inputInvoiceDateTime = it },
                    label = { Text("التاريخ والوقت") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_invoice_datetime"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )
            }
        }

        // --- 1. Quick Form Inputs ---
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Product Entry Name Input
                    OutlinedTextField(
                        value = viewModel.inputItemName,
                        onValueChange = { viewModel.inputItemName = it },
                        label = { Text("اسم السلعة") },
                        modifier = Modifier
                            .weight(1.8f)
                            .testTag("input_item_name"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        trailingIcon = {
                            if (viewModel.inputItemName.isNotEmpty()) {
                                IconButton(onClick = { viewModel.inputItemName = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        }
                    )

                    // Price Input
                    OutlinedTextField(
                        value = viewModel.inputPrice,
                        onValueChange = { viewModel.inputPrice = it },
                        label = { Text("الثمن DH") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1.1f)
                            .testTag("input_price"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    // Quantity Input
                    OutlinedTextField(
                        value = viewModel.inputQuantity,
                        onValueChange = { viewModel.inputQuantity = it },
                        label = { Text("العدد") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("input_quantity"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Manual Add Item button
                    Button(
                        onClick = {
                            val name = viewModel.inputItemName.trim()
                            val price = viewModel.inputPrice.toDoubleOrNull() ?: 0.0
                            val qty = viewModel.inputQuantity.toDoubleOrNull() ?: 1.0

                            if (name.isEmpty() || price <= 0.0) {
                                Toast.makeText(context, "دخل سمية وتمن صحيح عافاك!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            viewModel.addItem(name, price, qty)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(50.dp)
                            .testTag("add_item_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إضافة", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                    }

                    // SCAN BUTTON (سكاني)
                    Row(
                        modifier = Modifier
                            .weight(1.2f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                            )
                            .clickable { onTriggerScan() }
                            .border(1.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(12.dp)),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "سكاني 🏷️",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 14.sp
                        )
                    }

                    // MICROPHONE BUTTON (مكان المكريفون)
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(
                                brush = if (viewModel.isVoiceProcessing) {
                                    Brush.radialGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.error,
                                            MaterialTheme.colorScheme.primary
                                        )
                                    )
                                } else {
                                    Brush.radialGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.secondary,
                                            MaterialTheme.colorScheme.secondaryContainer
                                        )
                                    )
                                }
                            )
                            .clickable { onTriggerSpeech() }
                            .testTag("mic_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (viewModel.isVoiceProcessing) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "قراءة صوتية",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- 2. Live Dynamic Summary Dashboard (العداد اتمام اتوماتيك) ---
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "المجموع الإجمالي (اتوماتيك)",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${String.format("%.2f", viewModel.currentInvoiceTotal)} DH",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("total_amount_counter")
                    )
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.padding(2.dp)
                ) {
                    Text(
                        text = "عدد المنتوجات: ${viewModel.currentInvoiceItems.size}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- 3. Scrollable List of Items ---
        Text(
            text = "المنتوجات المضافة للفاتورة",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (viewModel.currentInvoiceItems.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ShoppingCart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "الفاتورة خاوية دبا!\nاضغط على الميكروفون 🎙️ ودخل السلعة بصوتك\nأو سكاني الباركود 🏷️ مباشرة.",
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        lineHeight = 20.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(viewModel.currentInvoiceItems) { item ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(1.dp, RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = "الكمية: ${if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity}",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                        Text(
                                            text = "ثمن الحبة: ${item.price} DH",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(
                                        text = "${String.format("%.2f", item.price * item.quantity)} DH",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )

                                    IconButton(
                                        onClick = { viewModel.removeItem(item) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "حذف منتج",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- 4. Core Workflow Buttons Panel (ارسال لواتساب، طباعة بلوتوث، حفظ فاتورة جديدة) ---
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // WhatsApp Button (إرسال لواتساب)
                Button(
                    onClick = { viewModel.shareInvoiceToWhatsApp() },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("whatsapp_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)) // WhatsApp iconic color!
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إرسال للواتساب", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                }

                // Bluetooth Print Button (طباعة)
                Button(
                    onClick = { viewModel.printBluetoothInvoice() },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("print_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("طباعة بالبلوتوث", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                }
            }

            // Save & Start New Button (حفظ ملي نكمل فكتورة ندير حفظ يعطيني فاتورة جديدة)
            Button(
                onClick = { viewModel.saveInvoiceAndStartNew() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_finished_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "حفظ وإعطاء فاتورة جديدة 💾✨",
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun SalesHistoryScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val savedList by viewModel.savedInvoices.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "الفواتير وتاريخ المبيعات المحفوظة 📁",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (savedList.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.FolderOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "مازال ما كاين حتى فاتورة محفوظة.\nملي تبيع شي سلعة اضغط على 'حفظ'!",
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(savedList) { invoice ->
                    val dateFormatted = remember(invoice.timestamp) {
                        try {
                            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                            sdf.format(Date(invoice.timestamp))
                        } catch (e: Exception) {
                            "تاريخ غير معروف"
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(1.dp, RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "فاتورة رقم #${invoice.id}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "التاريخ: $dateFormatted",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                if (!invoice.customerName.isNullOrBlank()) {
                                    Text(
                                        text = "الزبون: ${invoice.customerName}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                                if (!invoice.customerPhone.isNullOrBlank()) {
                                    Text(
                                        text = "الهاتف: ${invoice.customerPhone}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "${String.format("%.2f", invoice.totalAmount)} DH",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )

                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "محفوظ",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SalesDashboardAndHistoryScreen(viewModel: MainViewModel) {
    val savedList by viewModel.savedInvoices.collectAsStateWithLifecycle()
    val allSoldItems by viewModel.allInvoiceItems.collectAsStateWithLifecycle()
    
    // Calculate stats
    val totalRevenue = savedList.sumOf { it.totalAmount }
    val totalOrders = savedList.size
    
    val topSellingItems = remember(allSoldItems) {
        allSoldItems.groupBy { it.name.trim() }
            .mapValues { entry -> 
                val qty = entry.value.sumOf { it.quantity }
                val rev = entry.value.sumOf { it.price * it.quantity }
                Pair(qty, rev)
            }
            .toList()
            .sortedByDescending { it.second.first } // sort by units
            .take(5)
    }

    // List of last 7 days daily sales
    val calendar = java.util.Calendar.getInstance()
    val dayFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
    val dayKeyFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    
    val chartData = remember(savedList) {
        (0..6).map { i ->
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.DAY_OF_YEAR, -i)
            val date = cal.time
            val dateStr = dayFormat.format(date)
            val keyStr = dayKeyFormat.format(date)
            
            val dailySum = savedList.filter { 
                dayKeyFormat.format(Date(it.timestamp)) == keyStr 
            }.sumOf { it.totalAmount }
            
            Pair(dateStr, dailySum)
        }.reversed()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Stat Cards ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("إجمالي المبيعات", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("${String.format("%.2f", totalRevenue)} DH", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("عدد العمليات", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text("$totalOrders فواتير", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }
        }

        // --- Custom Area Bar Chart (Recharts Native Alternative) ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .shadow(1.dp, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "مبيعات آخر 7 أيام 📊",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val maxVal = chartData.maxOfOrNull { it.second } ?: 100.0
                    val highestRange = if (maxVal == 0.0) 100.0 else maxVal

                    // Draw Chart Canvas
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    ) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        
                        // Draw grid lines
                        val gridLines = 3
                        for (i in 0..gridLines) {
                            val y = canvasHeight * (i.toFloat() / gridLines)
                            drawLine(
                                color = Color.LightGray.copy(alpha = 0.5f),
                                start = androidx.compose.ui.geometry.Offset(0f, y),
                                end = androidx.compose.ui.geometry.Offset(canvasWidth, y),
                                strokeWidth = 1f
                            )
                        }

                        // Plot items
                        val barCount = chartData.size
                        val barSpacing = canvasWidth / barCount
                        val barWidth = barSpacing * 0.5f

                        chartData.forEachIndexed { index, data ->
                            val x = index * barSpacing + barSpacing * 0.25f
                            val valPct = (data.second / highestRange).toFloat()
                            val barHeight = canvasHeight * valPct * 0.85f // leave padding for labels
                            val y = canvasHeight - barHeight

                            // Draw rounded gradient bar
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF6200EE),
                                        Color(0xFFBB86FC)
                                    )
                                ),
                                topLeft = androidx.compose.ui.geometry.Offset(x, y),
                                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )
                        }
                    }

                    // Labels below bars
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        chartData.forEach { data ->
                            Text(
                                text = data.first,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    // Values line
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        chartData.forEach { data ->
                            Text(
                                text = "${data.second.toInt()}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // --- Top Selling Products Chart list ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .shadow(1.dp, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "السلع الأكثر مبيعا 🔥",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (topSellingItems.isEmpty()) {
                        Text(
                            "مكاين مبيعات فالفترة الحالية.",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        val maxQty = topSellingItems.first().second.first
                        topSellingItems.forEach { (name, stats) ->
                            val qty = stats.first
                            val revenue = stats.second
                            val progressPct = if (maxQty > 0.0) (qty / maxQty).toFloat() else 0f

                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("${qty.toInt()} حبة (- ${revenue} DH)", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { progressPct },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Core Sales History list ---
        item {
            Text(
                text = "سجل المبيعات والتاريخ 📁",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        if (savedList.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("مكاين حتى فاتورة محفوظة", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(savedList) { invoice ->
                val dateFormatted = remember(invoice.timestamp) {
                    try {
                        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        sdf.format(Date(invoice.timestamp))
                    } catch (e: Exception) {
                        "تاريخ غير معروف"
                    }
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(1.dp, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "فاتورة رقم #${invoice.id}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "التاريخ: $dateFormatted",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            if (!invoice.customerName.isNullOrBlank()) {
                                Text(
                                    text = "الزبون: ${invoice.customerName}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            if (!invoice.customerPhone.isNullOrBlank()) {
                                Text(
                                    text = "الهاتف: ${invoice.customerPhone}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "${String.format("%.2f", invoice.totalAmount)} DH",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )

                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "محفوظ",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PurchasesScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val savedPurchases by viewModel.savedPurchaseInvoices.collectAsStateWithLifecycle()
    val allPurchaseLines by viewModel.allPurchaseItems.collectAsStateWithLifecycle()
    
    // Launcher setups
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            viewModel.processReceiptImageForOcr(bitmap)
        }
    }
    
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val resolver = context.contentResolver
                val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    val source = android.graphics.ImageDecoder.createSource(resolver, uri)
                    android.graphics.ImageDecoder.decodeBitmap(source)
                } else {
                    android.provider.MediaStore.Images.Media.getBitmap(resolver, uri)
                }
                viewModel.processReceiptImageForOcr(bitmap)
            } catch (e: Exception) {
                Toast.makeText(context, "فشل قراءة الصورة", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- SECTION 1: Add purchase header ---
        item {
            Text(
                text = "تسجيل مشتريات جديدة بالجملة 📦",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // --- Photo & Capture Controls card ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "صور كشف السلعة وبدء تفريغ الفاتورة تلقائياً بالذكاء الاصطناعي:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { cameraLauncher.launch(null) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("التقاط صورة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        Button(
                            onClick = { galleryLauncher.launch("image/*") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("من المعرض", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    // Demo Simulation button for instant value
                    OutlinedButton(
                        onClick = { 
                            // Feed mock receipt details directly
                            val dummyBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
                            viewModel.processReceiptImageForOcr(dummyBitmap)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تجربة الفحص التلقائي (محاكاة) 🤖✨", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- Processing Indicator ---
        if (viewModel.isOcrProcessing) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Text(
                            "جاري فحص صورة الفاتورة واستخراج محتواها... 🧠⚡",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // --- Extracted Raw Text Copy Panel ("ونسخ ما بداخلها") ---
        if (viewModel.extractedOcrRawText != null) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("النص المُستخرج من الفاتورة 🧾", fontFamily = MaterialTheme.typography.titleMedium.fontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Gray)
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Extracted Receipt", viewModel.extractedOcrRawText)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "تم نسخ النص بنجاح! 📋✅", Toast.LENGTH_SHORT).show()
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("نسخ المحتوى", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Text(
                            text = viewModel.extractedOcrRawText ?: "",
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        )
                    }
                }
            }
        }

        // --- Supplier Form State Area ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .shadow(1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("معلومات الفاتورة والسلع والمشتريات:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    
                    OutlinedTextField(
                        value = viewModel.purchaseDraftSupplierName,
                        onValueChange = { viewModel.purchaseDraftSupplierName = it },
                        label = { Text("إسم المورد / الشركة") },
                        modifier = Modifier.fillMaxWidth().testTag("purchase_supplier_input"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                    
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))
                    
                    // Add item manually row
                    Text("إضافة سلعة يدوياً إلى هذه الفاتورة:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = viewModel.purchaseInputItemName,
                            onValueChange = { viewModel.purchaseInputItemName = it },
                            label = { Text("السلعة") },
                            modifier = Modifier.weight(1.2f).testTag("purchase_item_input"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = viewModel.purchaseInputPrice,
                            onValueChange = { viewModel.purchaseInputPrice = it },
                            label = { Text("ثمن الشراء") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(0.9f).testTag("purchase_price_input"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = viewModel.purchaseInputQuantity,
                            onValueChange = { viewModel.purchaseInputQuantity = it },
                            label = { Text("الكمية") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(0.8f).testTag("purchase_quantity_input"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }
                    
                    Button(
                        onClick = { 
                            val p = viewModel.purchaseInputPrice.toDoubleOrNull() ?: 0.0
                            val q = viewModel.purchaseInputQuantity.toDoubleOrNull() ?: 1.0
                            if (viewModel.purchaseInputItemName.isNotBlank() && p > 0.0) {
                                viewModel.addDraftPurchaseItem(viewModel.purchaseInputItemName, p, q)
                            } else {
                                Toast.makeText(context, "دخل المعلومات صحيحة", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("purchase_add_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إضافة السلعة للفاتورة", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- Drafting Purchase Items list ---
        if (viewModel.purchaseDraftItems.isNotEmpty()) {
            item {
                Text("السلع النشطة المضافة للفاتورة الحالية:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Gray)
            }
            
            items(viewModel.purchaseDraftItems) { draft ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(draft.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("الكمية: ${draft.quantity} × الثمن: ${draft.price} DH", fontSize = 12.sp, color = Color.DarkGray)
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "${String.format("%.2f", draft.price * draft.quantity)} DH",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = { viewModel.removeDraftPurchaseItem(draft) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "حذف", tint = Color.Red, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
            
            // Core save draft button
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("المجموع الإجمالي للشراء:", fontSize = 12.sp, color = Color.DarkGray)
                            Text("${String.format("%.2f", viewModel.purchaseDraftTotal)} DH", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.savePurchaseInvoiceAndStartNew() },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("حفظ الشراء", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            
                            OutlinedButton(
                                onClick = { viewModel.clearPurchaseDraft() },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("إلغاء", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // --- SEARCH SECTION AT THE BOTTOM ("مكان بحت عن سلعة معينة داخل سجل الشراء") ---
        item {
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.6f), modifier = Modifier.padding(vertical = 12.dp))
            Text(
                text = "البحث وسجل مشتريات الجملة 📁🔍",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Search Bar (البحث عن سلعة معينة داخل سجل الشراء)
        item {
            OutlinedTextField(
                value = viewModel.purchaseSearchQuery,
                onValueChange = { viewModel.purchaseSearchQuery = it },
                label = { Text("أكتب إسم السلعة للبحث في فواتير الشراء") },
                placeholder = { Text("مثال: حليب، كوكا، زيت...") },
                modifier = Modifier.fillMaxWidth().testTag("purchase_search_field"),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    if (viewModel.purchaseSearchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.purchaseSearchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "مسح")
                        }
                    } else {
                        Icon(Icons.Default.Search, contentDescription = "بحث")
                    }
                }
            )
        }

        // Search Result logic
        if (viewModel.purchaseSearchQuery.isNotBlank()) {
            val q = viewModel.purchaseSearchQuery.trim().lowercase()
            val filteredLines = allPurchaseLines.filter { it.name.trim().lowercase().contains(q) }
            
            if (filteredLines.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("السلعة المبحوث عنها غير متوفرة في سجل الشراء ❌", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                item {
                    Text("نتائج البحث المتطابقة في المشتريات (${filteredLines.size}):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                }
                
                items(filteredLines) { line ->
                    // Find corresponding supplier and date
                    val parentInvoice = savedPurchases.find { it.id == line.purchaseInvoiceId }
                    val supplier = parentInvoice?.supplierName ?: "مورد غير معروف"
                    val dateFormatted = remember(parentInvoice?.timestamp) {
                        if (parentInvoice?.timestamp != null) {
                            try {
                                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(parentInvoice.timestamp))
                            } catch (e: Exception) {
                                "تاريخ غير معروف"
                            }
                        } else "تاريخ غير معروف"
                    }

                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(line.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                                Text("المورد: $supplier", fontSize = 11.sp, color = Color.DarkGray, fontWeight = FontWeight.SemiBold)
                                Text("تاريخ الشراء: $dateFormatted", fontSize = 10.sp, color = Color.Gray)
                            }
                            
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${line.price} DH / للحبة", fontWeight = FontWeight.Black, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                                Text("الكمية: ${line.quantity} حبة", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }

        } else {
            // Display normal purchase record invoices history
            if (savedPurchases.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("السجل فارغ. مكاين حتى فاتورة شراء مسجلة.", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                items(savedPurchases) { purchase ->
                    val dateFormatted = remember(purchase.timestamp) {
                        try {
                            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(purchase.timestamp))
                        } catch (e: Exception) {
                            "تاريخ غير معروف"
                        }
                    }
                    
                    // State to handle expansion inline
                    var isExpanded by remember { mutableStateOf(false) }
                    
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(1.dp, RoundedCornerShape(12.dp))
                            .clickable { isExpanded = !isExpanded }
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = purchase.supplierName ?: "مورد الجملة",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "التاريخ: $dateFormatted • رقم الفاتورة #${purchase.id}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = if (isExpanded) "اضغط للطي 🔼" else "اضغط لعرض السلع 🔽",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "${String.format("%.2f", purchase.totalAmount)} DH",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    
                                    IconButton(
                                        onClick = { viewModel.deletePurchaseInvoice(purchase.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                            
                            // Expanded inside inline lines
                            if (isExpanded) {
                                val invoiceItems = allPurchaseLines.filter { it.purchaseInvoiceId == purchase.id }
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                                        .padding(12.dp)
                                ) {
                                    Text("السلع المتضمنة في الفاتورة:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(bottom = 6.dp))
                                    invoiceItems.forEach { subItem ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("• ${subItem.name}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Text("${subItem.quantity} حبة × ${subItem.price} DH = ${subItem.price * subItem.quantity} DH", fontSize = 12.sp, color = Color.DarkGray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
