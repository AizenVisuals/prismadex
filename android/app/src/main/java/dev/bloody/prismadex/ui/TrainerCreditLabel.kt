package dev.bloody.prismadex.ui
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.sp
import dev.bloody.prismadex.data.*

@Composable internal fun TrainerCreditLabel(key:String){
 if(key.isBlank())return
 val context=LocalContext.current;val uri=LocalUriHandler.current
 val repository=remember{TrainerCreditsRepository(context)}
 val credit by produceState<TrainerCredit?>(null,key){value=repository.credit(key)}
 credit?.let{row->
  row.artist?.let{LText("Arte de treinador: $it",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
  TextButton(onClick={uri.openUri(row.sourceIndexUrl)}){LText("Fonte e créditos do personagem",fontSize=12.sp)}
 }
}
