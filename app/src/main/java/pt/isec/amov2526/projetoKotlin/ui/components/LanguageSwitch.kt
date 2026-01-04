package pt.isec.amov2526.projetoKotlin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.isec.amov2526.projetoKotlin.utils.LocaleHelper

import pt.isec.amov2526.projetoKotlin.ui.theme.*

@Composable
fun LanguageSwitch(
    modifier: Modifier = Modifier,
    onLanguageChange: () -> Unit = {}
) {
    val context = LocalContext.current
    var currentLang by remember { mutableStateOf(LocaleHelper.getSelectedLanguage()) }

    Row(
        modifier = modifier
            .height(36.dp)
            .width(100.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Slate200)
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(if (currentLang == "en") Color.White else Color.Transparent)
                .clickable {
                    if (currentLang != "en") {
                        LocaleHelper.saveAndSetLocale(context, "en")
                        currentLang = "en"
                        onLanguageChange()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "ENG",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (currentLang == "en") Slate900 else Color.Gray
            )
        }


        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(if (currentLang == "pt") Color.White else Color.Transparent)
                .clickable {
                    if (currentLang != "pt") {
                        LocaleHelper.saveAndSetLocale(context, "pt")
                        currentLang = "pt"
                        onLanguageChange()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "PT",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (currentLang == "pt") Slate900 else Color.Gray
            )
        }
    }
}
