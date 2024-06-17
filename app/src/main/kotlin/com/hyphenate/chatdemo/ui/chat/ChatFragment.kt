package com.hyphenate.chatdemo.ui.chat

import android.os.Bundle
import android.text.TextUtils
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.hyphenate.chatdemo.R
import com.hyphenate.chatdemo.callkit.CallKitManager
import com.hyphenate.chatdemo.common.DemoConstant
import com.hyphenate.chatdemo.common.MenuFilterHelper
import com.hyphenate.chatdemo.common.PresenceCache
import com.hyphenate.chatdemo.interfaces.IPresenceResultView
import com.hyphenate.chatdemo.utils.EasePresenceUtil
import com.hyphenate.chatdemo.viewmodel.PresenceViewModel
import com.hyphenate.chatdemo.interfaces.IPresenceRequest
import com.hyphenate.easeui.EaseIM
import com.hyphenate.easeui.common.ChatMessage
import com.hyphenate.easeui.common.ChatPresence
import com.hyphenate.easeui.common.bus.EaseFlowBus
import com.hyphenate.easeui.feature.chat.EaseChatFragment
import com.hyphenate.easeui.feature.chat.enums.EaseChatType
import com.hyphenate.easeui.feature.chat.widgets.EaseChatLayout
import com.hyphenate.easeui.menu.chat.EaseChatMenuHelper
import com.hyphenate.easeui.model.EaseEvent

class ChatFragment: EaseChatFragment() , IPresenceResultView {
    private var presenceViewModel: IPresenceRequest? = null
    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)
        binding?.titleBar?.inflateMenu(R.menu.demo_chat_menu)
        updatePresence()
    }

    override fun initEventBus() {
        super.initEventBus()
        EaseFlowBus.with<EaseEvent>(EaseEvent.EVENT.UPDATE + EaseEvent.TYPE.CONTACT + DemoConstant.EVENT_UPDATE_USER_SUFFIX).register(this) {
            if (it.isContactChange && it.message.isNullOrEmpty().not()) {
                val userId = it.message
                if (chatType == EaseChatType.SINGLE_CHAT && userId == conversationId) {
                    setDefaultHeader(true)
                }
                binding?.layoutChat?.chatMessageListLayout?.refreshMessages()
            }
        }
        EaseFlowBus.with<EaseEvent>(EaseEvent.EVENT.UPDATE.name).register(this) {
            if (it.isPresenceChange && it.message.equals(conversationId) ) {
                updatePresence()
            }
        }
    }

    override fun initViewModel() {
        super.initViewModel()
        presenceViewModel = ViewModelProvider(this)[PresenceViewModel::class.java]
        presenceViewModel?.attachView(this)
    }

    override fun initData() {
        super.initData()
        conversationId?.let {
            if (it != EaseIM.getCurrentUser()?.id){
                presenceViewModel?.fetchChatPresence(mutableListOf(it))
                presenceViewModel?.subscribePresences(mutableListOf(it))
            }
        }
    }

    override fun setMenuItemClick(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.chat_menu_video_call -> {
                showVideoCall()
                return true
            }
        }
        return super.setMenuItemClick(item)
    }

    private fun showVideoCall() {
        if (chatType == EaseChatType.SINGLE_CHAT) {
            CallKitManager.showSelectDialog(mContext, conversationId)
        } else {
            CallKitManager.startConferenceCall(mContext, conversationId)
        }
    }

    override fun onPreMenu(helper: EaseChatMenuHelper?, message: ChatMessage?) {
        super.onPreMenu(helper, message)
        MenuFilterHelper.filterMenu(helper, message)
    }

    private fun updatePresence(){
        if (chatType == EaseChatType.SINGLE_CHAT){
            conversationId?.let {
                val presence = PresenceCache.getUserPresence(it)
                val logoStatus = EasePresenceUtil.getPresenceIcon(mContext,presence)
                val subtitle = EasePresenceUtil.getPresenceString(mContext,presence)
                binding?.run{
                    titleBar.setLogoStatusMargin(end = -1, bottom = -1)
                    titleBar.setLogoStatus(logoStatus)
                    titleBar.setSubtitle(subtitle)
                    titleBar.getStatusView().visibility = View.VISIBLE
                    titleBar.setLogoStatusSize(resources.getDimensionPixelSize(com.hyphenate.easeui.R.dimen.ease_title_bar_status_icon_size))
                }
            }
        }
    }

    override fun onPeerTyping(action: String?) {
        if (TextUtils.equals(action, EaseChatLayout.ACTION_TYPING_BEGIN)) {
            binding?.titleBar?.setSubtitle(getString(com.hyphenate.easeui.R.string.alert_during_typing))
            binding?.titleBar?.visibility = View.VISIBLE
        } else if (TextUtils.equals(action, EaseChatLayout.ACTION_TYPING_END)) {
            updatePresence()
        }
    }


    override fun onDestroy() {
        conversationId?.let {
            if (it != EaseIM.getCurrentUser()?.id){
                presenceViewModel?.unsubscribePresences(mutableListOf(it))
            }
        }
        super.onDestroy()
    }

    override fun fetchChatPresenceSuccess(presence: MutableList<ChatPresence>) {
        updatePresence()
    }
}