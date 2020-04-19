package lite.telestorage.kt


import android.util.Log
import lite.telestorage.kt.database.FileHelper
import org.drinkless.td.libcore.telegram.TdApi
import org.drinkless.td.libcore.telegram.TdApi.GetChatHistory
import java.util.*


object Messages {

  var hasFullMessageMap = false
  var lastMessage: TdApi.Message? = null
  val messageMap: MutableMap<Long, TdApi.Message> = mutableMapOf()
  var limit = 50

  fun getMessages(start: Boolean = true) {
    if(start){
      hasFullMessageMap = false
      lastMessage = null
      messageMap.clear()
    }
    val chatId: Long = Settings.chatId
    if(chatId != 0L && !hasFullMessageMap) {
      Data.dataTransferInProgress = Date().time
      val messageId: Long = lastMessage?.id ?: 0
      Tg.client?.send(
        GetChatHistory(chatId, messageId, 0, limit, false),
        Tg.updateHandler
      )
    }
  }


  fun messageUpdate(receivedMessages: TdApi.Messages?) {
    val messages = receivedMessages?.messages
    if(!messages.isNullOrEmpty()) {
      for((i, message) in messages.withIndex()) {
        if(message is TdApi.Message){
          if(!messageMap.containsKey(message.id)) {
            Data.addFromMsg(Message(message).fileData)
            messageMap[message.id] = message
          }
          if(i == messages.size - 1) {
            lastMessage = message
          }
        }
      }
      getMessages(false)
    } else {
      hasFullMessageMap = true
      Log.d("hasFullMessageMap", messageMap.toString())

      Data.dataTransferInProgress = 0
      FileUpdates.syncQueue()
      if(Data.toDelete.isNotEmpty()){
        for(file in Data.toDelete){
          FileHelper.delete(file)
        }
      }
      FileUpdates.nextDataTransfer()

      Data.debugInfo()

//      FileUpdates.nextDataTransfer()

//      Updates.actions.add { Log.d("Updates.actions.add", "started function after updates updated") }
//      Updates.actions.add(FileUpdates::syncQueue)
//      Updates.actions.add(this::syncMessagesWithDb) //TODO
//      Updates.updateMessagePath()
//      syncMessagesWithDb()
    }
  }

  fun deleteMsgByIds(idSet: MutableSet<Long>){
    val ids = idSet.toLongArray()
    if(Settings.chatId != 0L && ids.isNotEmpty()){
      Tg.client?.send(TdApi.DeleteMessages(Settings.chatId, ids, true), Tg.updateHandler)
    }
  }

  fun syncMessagesWithDb(){
    if(hasFullMessageMap) {
      val fileUpdate = FileUpdate(messageMap)
      fileUpdate.commitUpdates()
      Sync.afterMessageSynced()
//      messageMap.clear()
//      hasFullMessageMap = false
//      lastMessage = null
      Data.inProgress = 0
    }
  }

}
