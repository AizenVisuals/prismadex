package dev.bloody.prismadex.meta

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.YearMonth

internal data class MetaEntry(val rank:Int,val name:String,val usage:Double)
internal data class MetaSnapshot(val month:String,val formatCode:String,val sourceUrl:String,val battles:Long,val fetchedAt:Long,val entries:List<MetaEntry>,val cached:Boolean=false,val notice:String?=null)

/** Public, versioned simulator reports only. Never labels OU as ranked Champions Singles. */
internal class MetaRepository(context:Context){
    private val directory=File(context.applicationContext.filesDir,"meta-cache")
    private fun download(url:String):String{
        val connection=URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout=8_000;connection.readTimeout=12_000
        connection.setRequestProperty("User-Agent","PrismaDex/0.1 (Champions companion)")
        return try{check(connection.responseCode==200){"Fonte indisponível (${connection.responseCode})."};connection.inputStream.bufferedReader().use{r->val result=StringBuilder();val buffer=CharArray(8192);while(true){val count=r.read(buffer);if(count<0)break;result.append(buffer,0,count);check(result.length<1_000_000){"Relatório maior que o esperado."}};result.toString()}}finally{connection.disconnect()}
    }
    private fun decode(j:JSONObject)=MetaSnapshot(j.getString("month"),j.getString("formatCode"),j.getString("sourceUrl"),j.getLong("battles"),j.getLong("fetchedAt"),j.getJSONArray("entries").let{rows->(0 until rows.length()).map{i->rows.getJSONObject(i).let{MetaEntry(it.getInt("rank"),it.getString("name"),it.getDouble("usage"))}}})
    private fun encode(s:MetaSnapshot)=JSONObject().put("month",s.month).put("formatCode",s.formatCode).put("sourceUrl",s.sourceUrl).put("battles",s.battles).put("fetchedAt",s.fetchedAt).put("entries",JSONArray().apply{s.entries.forEach{put(JSONObject().put("rank",it.rank).put("name",it.name).put("usage",it.usage))}})
    /** Emit saved reports before any network work, including expired reports. */
    fun observe(format:String,force:Boolean=false)=flow {
        val saved=readCached(format)
        saved?.let { emit(it) }
        if(force || saved==null || System.currentTimeMillis()-saved.fetchedAt>=6*60*60*1000L){
            emit(load(format,true))
        }
    }
    suspend fun readCached(format:String):MetaSnapshot?=withContext(Dispatchers.IO){
        require(format in setOf("singles","doubles"))
        runCatching{decode(JSONObject(File(directory,"$format.json").readText())).copy(cached=true)}.getOrNull()
    }
    suspend fun load(format:String,force:Boolean=false):MetaSnapshot=withContext(Dispatchers.IO){
        require(format in setOf("singles","doubles"))
        directory.mkdirs()
        val cache=File(directory,"$format.json")
        val old=runCatching{decode(JSONObject(cache.readText()))}.getOrNull()
        if(!force&&old!=null&&System.currentTimeMillis()-old.fetchedAt<6*60*60*1000)return@withContext old.copy(cached=true)
        try{
            val latestMonth=YearMonth.now().minusMonths(1)
            val pattern=if(format=="singles")Regex("gen9championsbssreg[a-z]+-1630\\.txt") else Regex("gen9championsvgc[0-9]+reg[a-z]+-1630\\.txt")
            var found:MetaSnapshot?=null
            for(offset in 0L..2L){
                val month=latestMonth.minusMonths(offset).toString()
                currentCoroutineContext().ensureActive()
                val index=runCatching{download("https://www.smogon.com/stats/$month/")}.getOrNull()?:continue
                currentCoroutineContext().ensureActive()
                val file=pattern.findAll(index).map{it.value}.distinct().maxByOrNull{it.substringAfter("reg")}?:continue
                val url="https://www.smogon.com/stats/$month/$file"
                val report=download(url)
                val entries=parseReport(report)
                check(entries.isNotEmpty()){"O formato do relatório mudou. Consulte a fonte."}
                val battles=Regex("Total battles:\\s*([0-9]+)").find(report)?.groupValues?.get(1)?.toLongOrNull()?:0
                found=MetaSnapshot(month,file.removeSuffix("-1630.txt"),url,battles,System.currentTimeMillis(),entries);break
            }
            val result=found?:error("Não foi encontrado relatório Champions deste formato nos últimos três meses.")
            currentCoroutineContext().ensureActive()
            val temporary=File(directory,"$format.pending");temporary.writeText(encode(result).toString());check(temporary.renameTo(cache)){"Falha ao atualizar o cache do Meta."};result
        }catch(e:CancellationException){throw e}catch(e:Exception){old?.copy(cached=true,notice="Não foi possível atualizar. Exibindo o último relatório salvo.")?:throw e}
    }
    companion object{
        internal fun parseReport(report:String):List<MetaEntry>{
            val row=Regex("^\\|\\s*([0-9]+)\\s*\\|\\s*([^|]+?)\\s*\\|\\s*([0-9.]+)%",RegexOption.MULTILINE)
            return row.findAll(report).map{m->MetaEntry(m.groupValues[1].toInt(),m.groupValues[2].trim(),m.groupValues[3].toDouble())}.toList()
        }
    }
}
